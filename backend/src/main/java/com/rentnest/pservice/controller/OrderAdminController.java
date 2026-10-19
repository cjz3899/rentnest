package com.rentnest.pservice.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.audit.AuditLog;
import com.rentnest.common.auth.RequireRole;
import com.rentnest.common.auth.Roles;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.pservice.dto.OrderQuery;
import com.rentnest.pservice.dto.OrderResp;
import com.rentnest.pservice.dto.OrderStatsResp;
import com.rentnest.pservice.dto.OrderStatusReq;
import com.rentnest.pservice.entity.ProductOrder;
import com.rentnest.pservice.service.OrderService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequireRole(Roles.ADMIN)
public class OrderAdminController {
    @Resource
    private OrderService orderService;

    @GetMapping("/page")
    public Result<PageResult<OrderResp>> page(OrderQuery query) {
        return Result.ok(orderService.page(query));
    }

    /** 按商品汇总订购数量与金额 */
    @GetMapping("/stats")
    public Result<List<OrderStatsResp>> stats(OrderQuery query) {
        return Result.ok(orderService.stats(query));
    }

    @PutMapping("/{id}/status")
    @AuditLog(action = "UPDATE_ORDER_STATUS", targetType = "ORDER", targetId = "#id")
    public Result<ProductOrder> updateStatus(@PathVariable Long id,
                                             @Valid @RequestBody OrderStatusReq req) {
        return Result.ok(orderService.updateStatus(
                id, req.getStatus(), req.getRemark(), UserContextHolder.require()));
    }
}
