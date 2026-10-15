package com.rentnest.pservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderStatsResp {
    private Long productId;
    private String productName;
    private Long orderCount;
    private Long totalQuantity;
    private BigDecimal totalAmount;
}
