package com.rentnest.rental.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.rental.dto.HouseDetailResp;
import com.rentnest.rental.dto.HouseListItemResp;
import com.rentnest.rental.dto.HouseQuery;
import com.rentnest.rental.service.HouseService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/houses")
public class HouseController {
    @Resource
    private HouseService houseService;

    @GetMapping("/page")
    public Result<PageResult<HouseListItemResp>> page(HouseQuery houseQuery) {
        //用户端固定不可见下架房源，且不能按房东越权查询
        houseQuery.setIncludeSuspended(false);
        houseQuery.setOwnerUserId(null);
        return Result.ok(houseService.page(houseQuery));
    }

    @GetMapping("/{id}")
    public Result<HouseDetailResp> detail(@PathVariable Long id) {
        CurrentUser current = UserContextHolder.get();
        return Result.ok(houseService.detail(id, current));
    }
}
