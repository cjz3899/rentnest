package com.rentnest.pservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String description;
    private String spec;
    private BigDecimal price;
    private String unit;
    private Long coverFileId;
    private String deliveryNote;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
