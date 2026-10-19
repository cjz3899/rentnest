package com.rentnest.pservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.pservice.dto.OrderQuery;
import com.rentnest.pservice.dto.OrderResp;
import com.rentnest.pservice.dto.OrderStatsResp;
import com.rentnest.pservice.entity.ProductOrder;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductOrderMapper extends BaseMapper<ProductOrder> {
    List<OrderResp> selectPage(OrderQuery query);

    long countByQuery(OrderQuery query);

    /**
     * 按商品汇总订购数量与金额，供管理端统计
     */
    List<OrderStatsResp> statsByProduct(OrderQuery query);
}
