package com.rentnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginReq {
    @NotBlank
    private String phone;

    @NotBlank
    private String password;
}
