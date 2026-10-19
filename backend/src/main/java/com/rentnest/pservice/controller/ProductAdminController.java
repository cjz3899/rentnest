package com.rentnest.pservice.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.audit.AuditLog;
import com.rentnest.common.auth.RequireRole;
import com.rentnest.common.auth.Roles;
import com.rentnest.common.page.PageResult;
import com.rentnest.pservice.dto.*;
import com.rentnest.pservice.entity.Product;
import com.rentnest.pservice.service.ProductService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequireRole(Roles.ADMIN)
public class ProductAdminController {
    @Resource
    private ProductService productService;

    @GetMapping("/page")
    public Result<PageResult<ProductListItemResp>> page(ProductQuery query) {
        query.setIncludeOffSale(true);
        return Result.ok(productService.page(query));
    }

    @GetMapping("/{id}")
    public Result<ProductDetailResp> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id, true));
    }

    @PostMapping
    @AuditLog(action = "CREATE_PRODUCT", targetType = "PRODUCT", targetId = "#result.data.id")
    public Result<Product> create(@Valid @RequestBody ProductSaveReq req) {
        return Result.ok(productService.create(req));
    }

    @PutMapping("/{id}")
    @AuditLog(action = "UPDATE_PRODUCT", targetType = "PRODUCT", targetId = "#id")
    public Result<Product> update(@PathVariable Long id, @Valid @RequestBody ProductSaveReq req) {
        return Result.ok(productService.update(id, req));
    }

    @PutMapping("/{id}/status")
    @AuditLog(action = "CHANGE_PRODUCT_STATUS", targetType = "PRODUCT", targetId = "#id")
    public Result<Product> changeStatus(@PathVariable Long id, @Valid @RequestBody ProductStatusReq req) {
        return Result.ok(productService.changeStatus(id, req.getStatus()));
    }
}
