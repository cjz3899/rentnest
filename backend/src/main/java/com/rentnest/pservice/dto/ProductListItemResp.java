package com.rentnest.pservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductListItemResp {
    private Long id;
    private String name;
    private String spec;
    private BigDecimal price;
    private String unit;
    private String status;
    private String coverUrl;
}
