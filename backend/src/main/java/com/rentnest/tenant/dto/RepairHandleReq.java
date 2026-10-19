package com.rentnest.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RepairHandleReq {
    @NotBlank(message = "状态不能为空")
    private String status;

    /** 处理备注/维修结果 */
    @Size(max = 500)
    private String remark;
}
