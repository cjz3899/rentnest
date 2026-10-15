package com.rentnest.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.exception.BizException;
import com.rentnest.common.page.PageResult;
import com.rentnest.rental.dto.AppointmentCreateReq;
import com.rentnest.rental.dto.AppointmentQuery;
import com.rentnest.rental.dto.AppointmentResp;
import com.rentnest.rental.entity.House;
import com.rentnest.rental.entity.RentalAppointment;
import com.rentnest.rental.entity.RentalAppointmentLog;
import com.rentnest.rental.enums.AppointmentStatus;
import com.rentnest.rental.enums.HouseStatus;
import com.rentnest.rental.mapper.RentalAppointmentLogMapper;
import com.rentnest.rental.mapper.RentalAppointmentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {
    @Resource
    private RentalAppointmentMapper rentalAppointmentMapper;
    @Resource
    private HouseService houseService;
    @Resource
    private RentalAppointmentLogMapper rentalAppointmentLogMapper;

    @Transactional
    public RentalAppointment create(AppointmentCreateReq req, CurrentUser current) {
        House house = houseService.mustGet(req.getHouseId());
        if (!HouseStatus.RENTABLE.name().equals(house.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "该房源当前状态不可预约");
        }
        // 同一用户对同一房源只允许一个进行中的预约
        Long ongoing = rentalAppointmentMapper.selectCount(new LambdaQueryWrapper<RentalAppointment>()
                .eq(RentalAppointment::getHouseId, req.getHouseId())
                .eq(RentalAppointment::getUserId, current.getId())
                .in(RentalAppointment::getStatus, List.of(
                        AppointmentStatus.PENDING.name(),
                        AppointmentStatus.CONTACTED.name(),
                        AppointmentStatus.VIEWING.name(),
                        AppointmentStatus.CONFIRMED.name())));
        if (ongoing > 0) {
            throw new BizException(ErrorCode.CONFLICT, "你已有一个进行中的预约，请等待处理");
        }

        RentalAppointment appt = new RentalAppointment();
        appt.setHouseId(req.getHouseId());
        appt.setUserId(current.getId());
        appt.setContactName(req.getContactName());
        appt.setContactPhone(req.getContactPhone());
        appt.setExpectedMoveInDate(req.getExpectedMoveInDate());
        appt.setLeaseTerm(req.getLeaseTerm());
        appt.setRemark(req.getRemark());
        appt.setStatus(AppointmentStatus.PENDING.name());
        rentalAppointmentMapper.insert(appt);
        writeLog(appt.getId(), null, AppointmentStatus.PENDING.name(), current.getId(), "用户提交预约");
        return appt;
    }

    public RentalAppointment myDetail(Long id, CurrentUser current) {
        RentalAppointment appt = mustGet(id);
        if (!appt.getUserId().equals(current.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能查看自己的预约");
        }
        return appt;
    }

    /**
     * 管理端状态流转。业务联动：
     * 已确认 → 房源置为预约中；已完成 → 房源置为已出租；
     * 从已确认取消 → 若该房源无其他已确认预约，回到可出租。
     */
    @Transactional
    public RentalAppointment updateStatus(Long id, String targetStatus, String remark, CurrentUser current) {
        RentalAppointment appt = mustGet(id);
        requireManageable(appt, current);

        AppointmentStatus from = AppointmentStatus.valueOf(appt.getStatus());
        AppointmentStatus to = AppointmentStatus.parse(targetStatus);
        if (!from.canMoveTo(to)) {
            throw new BizException(ErrorCode.CONFLICT,
                    "预约状态不能从 " + from.name() + " 变更为 " + to.name());
        }
        // 用户提交的预约，其终态由处理方推进；用户自行取消走 cancelOwn
        appt.setStatus(to.name());
        appt.setHandlerId(current.getId());
        appt.setHandlerRemark(remark);
        appt.setHandledAt(LocalDateTime.now());
        rentalAppointmentMapper.updateById(appt);
        writeLog(id, from.name(), to.name(), current.getId(), remark);

        if (to == AppointmentStatus.CONFIRMED) {
            houseService.markStatus(appt.getHouseId(), HouseStatus.RESERVED);
        } else if (to == AppointmentStatus.COMPLETED) {
            houseService.markStatus(appt.getHouseId(), HouseStatus.RENTED);
        } else if (to == AppointmentStatus.CANCELLED && from == AppointmentStatus.CONFIRMED) {
            releaseHouseIfNoConfirmedAppointment(appt.getHouseId(), id);
        }
        return appt;
    }

    /**
     * 用户自行取消预约。从已确认阶段取消时，同样要释放房源，
     * 否则房源会永久停在 RESERVED，后续任何人都无法再预约。
     */
    @Transactional
    public RentalAppointment cancelOwn(Long id, CurrentUser current) {
        RentalAppointment appt = myDetail(id, current);
        AppointmentStatus from = AppointmentStatus.valueOf(appt.getStatus());
        if (!from.canMoveTo(AppointmentStatus.CANCELLED)) {
            throw new BizException(ErrorCode.CONFLICT, "当前状态无法取消");
        }
        appt.setStatus(AppointmentStatus.CANCELLED.name());
        rentalAppointmentMapper.updateById(appt);
        writeLog(id, from.name(), AppointmentStatus.CANCELLED.name(), current.getId(), "用户取消");
        if (from == AppointmentStatus.CONFIRMED) {
            releaseHouseIfNoConfirmedAppointment(appt.getHouseId(), id);
        }
        return appt;
    }

    public PageResult<AppointmentResp> page(AppointmentQuery query) {
        long total = rentalAppointmentMapper.countByQuery(query);
        List<AppointmentResp> records = total == 0
                ? List.of() : rentalAppointmentMapper.selectPage(query);
        return PageResult.of(records, total, query.getPage(), query.getSize());
    }

    private void releaseHouseIfNoConfirmedAppointment(Long houseId, Long excludeApptId) {
        Long remaining = rentalAppointmentMapper.selectCount(new LambdaQueryWrapper<RentalAppointment>()
                .eq(RentalAppointment::getHouseId, houseId)
                .ne(RentalAppointment::getId, excludeApptId)
                .in(RentalAppointment::getStatus, List.of(
                        AppointmentStatus.CONFIRMED.name(),
                        AppointmentStatus.COMPLETED.name())));
        if (remaining == 0) {
            houseService.markStatus(houseId, HouseStatus.RENTABLE);
        }
    }

    private void requireManageable(RentalAppointment appt, CurrentUser current) {
        if (current.isAdmin()) {
            return;
        }
        House house = houseService.mustGet(appt.getHouseId());
        if (!house.getOwnerUserId().equals(current.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能处理自己房源的预约");
        }
    }

    private RentalAppointment mustGet(Long id) {
        RentalAppointment appt = rentalAppointmentMapper.selectById(id);
        if (appt == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "预约不存在");
        }
        return appt;
    }

    private void writeLog(Long appointmentId, String from, String to, Long operatorId, String remark) {
        RentalAppointmentLog log = new RentalAppointmentLog();
        log.setAppointmentId(appointmentId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        rentalAppointmentLogMapper.insert(log);
    }
}