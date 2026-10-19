package com.rentnest.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.exception.BizException;
import com.rentnest.common.page.PageResult;
import com.rentnest.file.entity.FileRecord;
import com.rentnest.file.mapper.FileRecordMapper;
import com.rentnest.rental.service.HouseService;
import com.rentnest.tenant.dto.RepairCreateReq;
import com.rentnest.tenant.dto.RepairQuery;
import com.rentnest.tenant.dto.RepairResp;
import com.rentnest.tenant.entity.RepairMedia;
import com.rentnest.tenant.entity.RepairTicket;
import com.rentnest.tenant.enums.RepairStatus;
import com.rentnest.tenant.mapper.RepairMediaMapper;
import com.rentnest.tenant.mapper.RepairTicketMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RepairService {
    @Resource
    private RepairTicketMapper repairTicketMapper;
    @Resource
    private RepairMediaMapper repairMediaMapper;
    @Resource
    private FileRecordMapper fileRecordMapper;
    @Resource
    private HouseService houseService;

    @Transactional
    public RepairTicket create(RepairCreateReq req, CurrentUser current) {
        if (req.getHouseId() != null) {
            houseService.mustGet(req.getHouseId());
        }
        validateOwnedFiles(req.getFileIds(), current);

        RepairTicket ticket = new RepairTicket();
        ticket.setUserId(current.getId());
        ticket.setHouseId(req.getHouseId());
        ticket.setLocation(req.getLocation());
        ticket.setDescription(req.getDescription());
        ticket.setContactPhone(req.getContactPhone());
        ticket.setSupplement(req.getSupplement());
        ticket.setStatus(RepairStatus.PENDING.name());
        repairTicketMapper.insert(ticket);

        saveMedia(ticket.getId(), req.getFileIds());
        return ticket;
    }

    /** 详情含图片 URL，owner 本人或工作人员可看 */
    public com.rentnest.tenant.dto.RepairDetailResp detailWithMedia(Long id, CurrentUser current) {
        RepairTicket ticket = mustGet(id);
        if (!current.isStaff() && !ticket.getUserId().equals(current.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能查看自己的报修");
        }
        com.rentnest.tenant.dto.RepairDetailResp resp =
                new com.rentnest.tenant.dto.RepairDetailResp();
        BeanUtils.copyProperties(ticket, resp);
        resp.setMediaUrls(repairMediaMapper.selectUrlsByTicketId(id));
        return resp;
    }

    public PageResult<RepairResp> page(RepairQuery query) {
        long total = repairTicketMapper.countByQuery(query);
        List<RepairResp> records = total == 0
                ? List.of() : repairTicketMapper.selectPage(query);
        return PageResult.of(records, total, query.getPage(), query.getSize());
    }

    /** 房东/管理员处理：状态流转 + 记录处理人与结果 */
    @Transactional
    public RepairTicket handle(Long id, String targetStatus, String remark, CurrentUser current) {
        RepairTicket ticket = mustGet(id);
        RepairStatus from = RepairStatus.valueOf(ticket.getStatus());
        RepairStatus to = RepairStatus.parse(targetStatus);
        if (!from.canMoveTo(to)) {
            throw new BizException(ErrorCode.CONFLICT,
                    "报修状态不能从 " + from.name() + " 变更为 " + to.name());
        }
        ticket.setStatus(to.name());
        ticket.setHandlerId(current.getId());
        ticket.setHandlerRemark(remark);
        ticket.setHandledAt(LocalDateTime.now());
        repairTicketMapper.updateById(ticket);
        return ticket;
    }

    public RepairTicket mustGet(Long id) {
        RepairTicket ticket = repairTicketMapper.selectById(id);
        if (ticket == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "报修工单不存在");
        }
        return ticket;
    }

    /** 只能引用自己上传的文件，防止把他人文件挂进自己的工单 */
    private void validateOwnedFiles(List<Long> fileIds, CurrentUser current) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        Long owned = fileRecordMapper.selectCount(new LambdaQueryWrapper<FileRecord>()
                .in(FileRecord::getId, fileIds)
                .eq(FileRecord::getOwnerId, current.getId()));
        if (owned != fileIds.size()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "存在无效或不属于自己的图片文件");
        }
    }

    private void saveMedia(Long ticketId, List<Long> fileIds) {
        if (fileIds == null) {
            return;
        }
        int order = 0;
        for (Long fileId : fileIds.stream().distinct().toList()) {
            RepairMedia media = new RepairMedia();
            media.setTicketId(ticketId);
            media.setFileId(fileId);
            media.setSortOrder(order++);
            repairMediaMapper.insert(media);
        }
    }
}
