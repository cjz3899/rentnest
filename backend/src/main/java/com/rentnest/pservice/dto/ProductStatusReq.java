package com.rentnest.pservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductStatusReq {
    @NotBlank(message = "状态不能为空")
    private String status;
}
