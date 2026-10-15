package com.rentnest.pservice.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDetailResp {
    private Long id;
    private String name;
    private String description;
    private String spec;
    private BigDecimal price;
    private String unit;
    private String deliveryNote;
    private String status;
    private String coverUrl;
}
