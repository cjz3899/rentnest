package com.rentnest.tenant.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.audit.AuditLog;
import com.rentnest.common.auth.RequireRole;
import com.rentnest.common.auth.Roles;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.tenant.dto.RepairDetailResp;
import com.rentnest.tenant.dto.RepairHandleReq;
import com.rentnest.tenant.dto.RepairQuery;
import com.rentnest.tenant.dto.RepairResp;
import com.rentnest.tenant.entity.RepairTicket;
import com.rentnest.tenant.service.RepairService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报修处理归房东与管理员（需求 6.3.2 权限拆分）。
 */
@RestController
@RequestMapping("/api/admin/repairs")
@RequireRole(Roles.LANDLORD)
public class RepairAdminController {
    @Resource
    private RepairService repairService;

    @GetMapping("/page")
    public Result<PageResult<RepairResp>> page(RepairQuery query) {
        return Result.ok(repairService.page(query));
    }

    @GetMapping("/{id}")
    public Result<RepairDetailResp> detail(@PathVariable Long id) {
        return Result.ok(repairService.detailWithMedia(id, UserContextHolder.require()));
    }

    @PutMapping("/{id}/status")
    @AuditLog(action = "HANDLE_REPAIR", targetType = "REPAIR", targetId = "#id")
    public Result<RepairTicket> handle(@PathVariable Long id,
                                       @Valid @RequestBody RepairHandleReq req) {
        return Result.ok(repairService.handle(
                id, req.getStatus(), req.getRemark(), UserContextHolder.require()));
    }
}
