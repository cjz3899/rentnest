package com.rentnest.pservice.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.pservice.dto.OrderCreateReq;
import com.rentnest.pservice.dto.OrderQuery;
import com.rentnest.pservice.dto.OrderResp;
import com.rentnest.pservice.entity.ProductOrder;
import com.rentnest.pservice.service.OrderService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {
    @Resource
    private OrderService orderService;

    @PostMapping("/orders")
    public Result<ProductOrder> create(@Valid @RequestBody OrderCreateReq req) {
        return Result.ok(orderService.create(req, UserContextHolder.require()));
    }

    @GetMapping("/my/orders/page")
    public Result<PageResult<OrderResp>> myOrders(OrderQuery query) {
        query.setUserId(UserContextHolder.require().getId());
        return Result.ok(orderService.page(query));
    }

    @PutMapping("/my/orders/{id}/cancel")
    public Result<ProductOrder> cancel(@PathVariable Long id) {
        return Result.ok(orderService.cancelOwn(id, UserContextHolder.require()));
    }
}
