package com.rentnest.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.exception.BizException;
import com.rentnest.common.page.PageResult;
import com.rentnest.rental.dto.HouseDetailResp;
import com.rentnest.rental.dto.HouseListItemResp;
import com.rentnest.rental.dto.HouseQuery;
import com.rentnest.rental.dto.HouseSaveReq;
import com.rentnest.rental.entity.House;
import com.rentnest.rental.entity.HouseFacility;
import com.rentnest.rental.entity.HouseMedia;
import com.rentnest.rental.entity.RentalAppointment;
import com.rentnest.rental.enums.AppointmentStatus;
import com.rentnest.rental.enums.HouseStatus;
import com.rentnest.rental.mapper.HouseFacilityMapper;
import com.rentnest.rental.mapper.HouseMapper;
import com.rentnest.rental.mapper.HouseMediaMapper;
import com.rentnest.rental.mapper.RentalAppointmentMapper;
import com.rentnest.rental.redis.HouseListCache;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class HouseService {
    /**
     * 设施列表
     */
    private static final Set<String> VALID_FACILITIES =
            Set.of("BED", "AC", "WATER_HEATER", "WASHER", "NETWORK", "OTHER");
    @Resource
    private HouseMapper houseMapper;
    @Resource
    private HouseListCache houseListCache;
    @Resource
    private HouseMediaMapper houseMediaMapper;
    @Resource
    private HouseFacilityMapper houseFacilityMapper;
    @Resource
    private RentalAppointmentMapper rentalAppointmentMapper;

    public PageResult<HouseListItemResp> page(HouseQuery query) {
        PageResult<HouseListItemResp> cached = houseListCache.get(query);
        if (cached != null) {
            return cached;
        }
        long total = houseMapper.countByQuery(query);
        List<HouseListItemResp> records = total == 0 ? List.of() : houseMapper.selectPage(query);
        PageResult<HouseListItemResp> result = PageResult.of(records, total, query.getPage(), query.getSize());
        houseListCache.put(query, result);
        return result;
    }

    public HouseDetailResp detail(Long id, CurrentUser current) {
        House house = mustGet(id);
        if (!HouseStatus.PUBLIC_VISIBLE.contains(house.getStatus())) {
            // SUSPENDED 仅 owner / 管理员可见
            requireReadableByStaff(house, current);
        }
        HouseDetailResp resp = new HouseDetailResp();
        BeanUtils.copyProperties(house, resp);
        resp.setFacilities(houseFacilityMapper.selectList(
                        new LambdaQueryWrapper<HouseFacility>().eq(HouseFacility::getHouseId, id))
                .stream().map(HouseFacility::getFacility).toList());
        resp.setMedia(houseMediaMapper.selectByHouseId(id));
        return resp;
    }

    public House mustGet(Long id) {
        House house = houseMapper.selectById(id);
        if (house == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "房源不存在");
        }
        return house;
    }

    @Transactional
    public House create(HouseSaveReq req, CurrentUser current) {
        House house = new House();
        BeanUtils.copyProperties(req, house);
        house.setOwnerUserId(current.getId());
        house.setStatus(HouseStatus.RENTABLE.name());
        house.setIsRecommended(false);
        houseMapper.insert(house);
        saveRelations(house.getId(), req);
        houseListCache.evictAll();
        return house;
    }

    @Transactional
    public House update(Long id, HouseSaveReq req, CurrentUser current) {
        House house = mustGetOwned(id, current);
        BeanUtils.copyProperties(req, house, "id", "ownerUserId", "status", "isRecommended");
        houseMapper.updateById(house);
        saveRelations(id, req);
        houseListCache.evictAll();
        return house;
    }

    @Transactional
    public House changeStatus(Long id, String status, CurrentUser current) {
        if (!HouseStatus.isValid(status)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未知的房源状态: " + status);
        }
        House house = mustGetOwned(id, current);
        if (status.equals(house.getStatus())) {
            return house;
        }
        // 已出租的房源不能直接回到可出租，需先走预约完成/取消流程，避免业务事实被改写
        if (HouseStatus.RENTED.name().equals(house.getStatus())
                && HouseStatus.RENTABLE.name().equals(status)) {
            throw new BizException(ErrorCode.CONFLICT, "已出租房源不能直接改为可出租");
        }
        house.setStatus(status);
        houseMapper.updateById(house);
        houseListCache.evictAll();
        return house;
    }

    @Transactional
    public void delete(Long id, CurrentUser current) {
        House house = mustGetOwned(id, current);
        Long active = rentalAppointmentMapper.selectCount(new LambdaQueryWrapper<RentalAppointment>()
                .eq(RentalAppointment::getHouseId, id)
                .in(RentalAppointment::getStatus, List.of(
                        AppointmentStatus.PENDING.name(),
                        AppointmentStatus.CONTACTED.name(),
                        AppointmentStatus.VIEWING.name(),
                        AppointmentStatus.CONFIRMED.name())));
        if (active > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该房源存在进行中的预约，不能删除");
        }
        houseMediaMapper.delete(new LambdaQueryWrapper<HouseMedia>().eq(HouseMedia::getHouseId, id));
        houseFacilityMapper.delete(new LambdaQueryWrapper<HouseFacility>().eq(HouseFacility::getHouseId, id));
        houseMapper.deleteById(house.getId());
        houseListCache.evictAll();
    }

    @Transactional
    public House setRecommended(Long id, boolean recommended, CurrentUser current) {
        House house = mustGetOwned(id, current);
        house.setIsRecommended(recommended);
        houseMapper.updateById(house);
        houseListCache.evictAll();
        return house;
    }

    public House mustGetOwned(Long id, CurrentUser current) {
        House house = mustGet(id);
        if (!current.isAdmin() && !house.getOwnerUserId().equals(current.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能管理自己的房源");
        }
        return house;
    }

    private void requireReadableByStaff(House house, CurrentUser current) {
        if (current == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!current.isStaff() && !house.getOwnerUserId().equals(current.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "该房源暂不可查看");
        }
    }

    /**
     * 设施与媒体采用「全删重建」，语义简单，避免 diff 逻辑
     */
    private void saveRelations(Long houseId, HouseSaveReq req) {
        houseFacilityMapper.delete(new LambdaQueryWrapper<HouseFacility>()
                .eq(HouseFacility::getHouseId, houseId));
        if (req.getFacilities() != null) {
            for (String facility : req.getFacilities().stream().distinct().toList()) {
                if (!VALID_FACILITIES.contains(facility)) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "未知设施类型: " + facility);
                }
                HouseFacility record = new HouseFacility();
                record.setHouseId(houseId);
                record.setFacility(facility);
                houseFacilityMapper.insert(record);
            }
        }

        houseMediaMapper.delete(new LambdaQueryWrapper<HouseMedia>().eq(HouseMedia::getHouseId, houseId));
        if (req.getMedia() != null) {
            int order = 0;
            for (HouseSaveReq.MediaReq media : req.getMedia()) {
                if (!HouseMedia.TYPE_IMAGE.equals(media.getMediaType())
                        && !HouseMedia.TYPE_VIDEO.equals(media.getMediaType())) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "未知媒体类型: " + media.getMediaType());
                }
                HouseMedia record = new HouseMedia();
                record.setHouseId(houseId);
                record.setFileId(media.getFileId());
                record.setMediaType(media.getMediaType());
                record.setSortOrder(order++);
                houseMediaMapper.insert(record);
            }
        }
    }

    /**
     * 供预约模块联动调用：不做 owner 校验，直接设置状态
     */
    @Transactional
    public void markStatus(Long houseId, HouseStatus status) {
        House house = mustGet(houseId);
        if (status.name().equals(house.getStatus())) {
            return;
        }
        house.setStatus(status.name());
        houseMapper.updateById(house);
        houseListCache.evictAll();
    }
}
