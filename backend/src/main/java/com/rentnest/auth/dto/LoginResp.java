package com.rentnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResp {
    private final String token;
    private final String refreshToken;
    private final Long userId;
    private final String nickname;
    private final String role;
}
