package com.rentnest.pservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.pservice.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

}
