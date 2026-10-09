package com.rentnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenReq {
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
