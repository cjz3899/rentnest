package com.rentnest.rental.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.audit.AuditLog;
import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.auth.RequireRole;
import com.rentnest.common.auth.Roles;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.rental.dto.*;
import com.rentnest.rental.entity.House;
import com.rentnest.rental.service.HouseService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/houses")
@RequireRole(Roles.LANDLORD)
public class HouseAdminController {
    @Resource
    private HouseService houseService;

    @GetMapping("/page")
    public Result<PageResult<HouseListItemResp>> page(HouseQuery query) {
        CurrentUser current = UserContextHolder.require();
        query.setIncludeSuspended(true);
        if (!current.isAdmin()) {
            query.setOwnerUserId(current.getId());
        }
        return Result.ok(houseService.page(query));
    }

    @GetMapping("/{id}")
    public Result<HouseDetailResp> detail(@PathVariable Long id) {
        return Result.ok(houseService.detail(id, UserContextHolder.require()));
    }

    @PostMapping
    public Result<House> create(@Valid @RequestBody HouseSaveReq req) {
        return Result.ok(houseService.create(req, UserContextHolder.require()));
    }

    @PutMapping("/{id}")
    public Result<House> update(@PathVariable Long id, @Valid @RequestBody HouseSaveReq req) {
        return Result.ok(houseService.update(id, req, UserContextHolder.require()));
    }

    @PutMapping("/{id}/status")
    @AuditLog(action = "CHANGE_HOUSE_STATUS", targetType = "HOUSE", targetId = "#id")
    public Result<House> changeStatus(@PathVariable Long id, @Valid @RequestBody HouseStatusReq req) {
        return Result.ok(houseService.changeStatus(id, req.getStatus(), UserContextHolder.require()));
    }

    @PutMapping("/{id}/recommended")
    public Result<House> setRecommended(@PathVariable Long id, @RequestParam boolean recommended) {
        return Result.ok(houseService.setRecommended(id, recommended, UserContextHolder.require()));
    }

    @DeleteMapping("/{id}")
    @AuditLog(action = "DELETE_HOUSE", targetType = "HOUSE", targetId = "#id")
    public Result<Void> delete(@PathVariable Long id) {
        houseService.delete(id, UserContextHolder.require());
        return Result.ok();
    }
}