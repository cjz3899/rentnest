package com.rentnest.pservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSaveReq {
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100)
    private String name;

    @Size(max = 1000)
    private String description;

    @Size(max = 100)
    private String spec;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    @Size(max = 20)
    private String unit;

    /**
     * 封面：file_record.id，取自文件上传接口返回的 id
     */
    private Long coverFileId;

    @Size(max = 500)
    private String deliveryNote;
}
