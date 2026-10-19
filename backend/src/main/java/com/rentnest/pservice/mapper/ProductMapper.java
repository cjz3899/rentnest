package com.rentnest.pservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.pservice.dto.ProductDetailResp;
import com.rentnest.pservice.dto.ProductListItemResp;
import com.rentnest.pservice.dto.ProductQuery;
import com.rentnest.pservice.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    List<ProductListItemResp> selectPage(ProductQuery query);

    long countByQuery(ProductQuery query);

    /** 详情：一次查询带出封面 URL，避免再查 file_record */
    ProductDetailResp selectDetailById(@Param("id") Long id);
}
