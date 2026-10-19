package com.rentnest.pservice.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.page.PageResult;
import com.rentnest.pservice.dto.ProductDetailResp;
import com.rentnest.pservice.dto.ProductListItemResp;
import com.rentnest.pservice.dto.ProductQuery;
import com.rentnest.pservice.service.ProductService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class ProductController {
    @Resource
    private ProductService productService;

    @GetMapping("/page")
    public Result<PageResult<ProductListItemResp>> page(ProductQuery query) {
        // 用户端固定只看在售商品，无视入参状态
        query.setIncludeOffSale(false);
        query.setStatus(null);
        return Result.ok(productService.page(query));
    }

    @GetMapping("/{id}")
    public Result<ProductDetailResp> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id, false));
    }
}
