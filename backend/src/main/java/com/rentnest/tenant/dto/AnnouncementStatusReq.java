package com.rentnest.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementStatusReq {
    /** PUBLISHED / UNPUBLISHED */
    @NotBlank(message = "状态不能为空")
    private String status;
}
