package com.rentnest.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackReplyReq {
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 1000)
    private String reply;
}
