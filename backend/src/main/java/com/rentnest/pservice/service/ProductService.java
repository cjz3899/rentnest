package com.rentnest.pservice.service;

import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.exception.BizException;
import com.rentnest.common.page.PageResult;
import com.rentnest.pservice.dto.ProductDetailResp;
import com.rentnest.pservice.dto.ProductListItemResp;
import com.rentnest.pservice.dto.ProductQuery;
import com.rentnest.pservice.dto.ProductSaveReq;
import com.rentnest.pservice.entity.Product;
import com.rentnest.pservice.enums.ProductStatus;
import com.rentnest.pservice.mapper.ProductMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    @Resource
    private ProductMapper productMapper;

    public PageResult<ProductListItemResp> page(ProductQuery query) {
        long total = productMapper.countByQuery(query);
        List<ProductListItemResp> records = total == 0
                ? List.of() : productMapper.selectPage(query);
        return PageResult.of(records, total, query.getPage(), query.getSize());
    }

    public ProductDetailResp detail(Long id, boolean staffView) {
        ProductDetailResp resp = productMapper.selectDetailById(id);
        if (resp == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        if (!staffView && !ProductStatus.ON_SALE.name().equals(resp.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品已下架");
        }
        return resp;
    }

    public Product mustGet(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        return product;
    }

    @Transactional
    public Product create(ProductSaveReq req) {
        Product product = new Product();
        BeanUtils.copyProperties(req, product);
        // 新建默认下架，管理员确认信息无误后手动上架
        product.setStatus(ProductStatus.OFF_SALE.name());
        productMapper.insert(product);
        return product;
    }

    @Transactional
    public Product update(Long id, ProductSaveReq req) {
        Product product = mustGet(id);
        BeanUtils.copyProperties(req, product, "id", "status");
        productMapper.updateById(product);
        return product;
    }

    @Transactional
    public Product changeStatus(Long id, String status) {
        if (!ProductStatus.isValid(status)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未知的商品状态: " + status);
        }
        Product product = mustGet(id);
        product.setStatus(status);
        productMapper.updateById(product);
        return product;
    }

    // 不提供物理删除：订单列表的商品名靠 JOIN product 取得，
    // 删除商品会让历史订单查询丢行。下架即是对外不可见。
}
