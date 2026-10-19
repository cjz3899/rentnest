package com.rentnest.tenant.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.tenant.dto.RepairCreateReq;
import com.rentnest.tenant.dto.RepairDetailResp;
import com.rentnest.tenant.dto.RepairQuery;
import com.rentnest.tenant.dto.RepairResp;
import com.rentnest.tenant.entity.RepairTicket;
import com.rentnest.tenant.service.RepairService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RepairController {
    @Resource
    private RepairService repairService;

    @PostMapping("/repairs")
    public Result<RepairTicket> create(@Valid @RequestBody RepairCreateReq req) {
        return Result.ok(repairService.create(req, UserContextHolder.require()));
    }

    @GetMapping("/my/repairs/page")
    public Result<PageResult<RepairResp>> myRepairs(RepairQuery query) {
        query.setUserId(UserContextHolder.require().getId());
        return Result.ok(repairService.page(query));
    }

    /** owner 本人或工作人员可看（含图片） */
    @GetMapping("/repairs/{id}")
    public Result<RepairDetailResp> detail(@PathVariable Long id) {
        return Result.ok(repairService.detailWithMedia(id, UserContextHolder.require()));
    }
}
