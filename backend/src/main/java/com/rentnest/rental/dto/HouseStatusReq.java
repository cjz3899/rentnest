package com.rentnest.rental.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HouseStatusReq {
    /** 目标房源状态：RENTABLE/RESERVED/RENTED/SUSPENDED */
    @NotBlank(message = "状态不能为空")
    private String status;
}