package com.rentnest.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContentStatusReq {
    /** NORMAL / REMOVED */
    @NotBlank(message = "状态不能为空")
    private String status;
}
