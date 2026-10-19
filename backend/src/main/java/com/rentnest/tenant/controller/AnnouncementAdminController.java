package com.rentnest.tenant.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.audit.AuditLog;
import com.rentnest.common.auth.RequireRole;
import com.rentnest.common.auth.Roles;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.tenant.dto.AnnouncementQuery;
import com.rentnest.tenant.dto.AnnouncementResp;
import com.rentnest.tenant.dto.AnnouncementSaveReq;
import com.rentnest.tenant.dto.AnnouncementStatusReq;
import com.rentnest.tenant.entity.Announcement;
import com.rentnest.tenant.service.AnnouncementService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公告/FAQ 管理仅归 ADMIN（需求 6.3.2 权限拆分）。
 */
@RestController
@RequestMapping("/api/admin/announcements")
@RequireRole(Roles.ADMIN)
public class AnnouncementAdminController {
    @Resource
    private AnnouncementService announcementService;

    @GetMapping("/page")
    public Result<PageResult<AnnouncementResp>> page(AnnouncementQuery query) {
        return Result.ok(announcementService.adminPage(query));
    }

    @PostMapping
    @AuditLog(action = "CREATE_ANNOUNCEMENT", targetType = "ANNOUNCEMENT", targetId = "#result.data.id")
    public Result<Announcement> create(@Valid @RequestBody AnnouncementSaveReq req) {
        return Result.ok(announcementService.create(req, UserContextHolder.require()));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "UPDATE_ANNOUNCEMENT", targetType = "ANNOUNCEMENT", targetId = "#id")
    public Result<Announcement> update(@PathVariable Long id,
                                       @Valid @RequestBody AnnouncementSaveReq req) {
        return Result.ok(announcementService.update(id, req));
    }

    @PutMapping("/{id}/status")
    @AuditLog(action = "CHANGE_ANNOUNCEMENT_STATUS", targetType = "ANNOUNCEMENT", targetId = "#id")
    public Result<Announcement> changeStatus(@PathVariable Long id,
                                             @Valid @RequestBody AnnouncementStatusReq req) {
        return Result.ok(announcementService.changeStatus(id, req.getStatus()));
    }
}
