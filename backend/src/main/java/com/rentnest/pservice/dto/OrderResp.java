package com.rentnest.pservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderResp {
    private Long id;
    private String orderNo;
    private Long userId;
    private String userNickname;
    private Long productId;
    private String productName;
    private String productSpec;
    private String productUnit;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String receiverName;
    private String receiverPhone;
    private String deliveryAddress;
    private String remark;
    private String status;
    private LocalDateTime createdAt;
}
