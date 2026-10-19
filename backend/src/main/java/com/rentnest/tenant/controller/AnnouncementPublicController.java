package com.rentnest.tenant.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.page.PageResult;
import com.rentnest.tenant.dto.AnnouncementQuery;
import com.rentnest.tenant.dto.AnnouncementResp;
import com.rentnest.tenant.service.AnnouncementService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开公告/FAQ 接口：游客可访问（WebConfig 已放行 /api/announcements/public/**）。
 */
@RestController
@RequestMapping("/api/announcements/public")
public class AnnouncementPublicController {
    @Resource
    private AnnouncementService announcementService;

    /** type 可选：NOTICE 公告 / FAQ 常见问题 */
    @GetMapping("/page")
    public Result<PageResult<AnnouncementResp>> page(AnnouncementQuery query) {
        return Result.ok(announcementService.publicPage(query));
    }

    @GetMapping("/{id}")
    public Result<AnnouncementResp> detail(@PathVariable Long id) {
        return Result.ok(announcementService.publicDetail(id));
    }
}
