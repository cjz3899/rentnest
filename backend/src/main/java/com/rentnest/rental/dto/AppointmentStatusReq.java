package com.rentnest.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppointmentStatusReq {
    /** 目标状态：PENDING/CONTACTED/VIEWING/CONFIRMED/COMPLETED/CANCELLED */
    @NotBlank(message = "状态不能为空")
    private String status;

    /** 处理备注 */
    @Size(max = 500)
    private String remark;
}