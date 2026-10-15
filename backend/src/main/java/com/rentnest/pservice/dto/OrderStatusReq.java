package com.rentnest.pservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderStatusReq {
    @NotBlank(message = "状态不能为空")
    private String status;

    @Size(max = 500)
    private String remark;
}
