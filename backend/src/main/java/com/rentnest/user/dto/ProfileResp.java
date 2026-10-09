package com.rentnest.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ProfileResp {
    private final Long id;
    private final String phone;
    private final String nickname;
    private final Long avatarFileId;
    private final String role;
    private final String realName;
    private final String email;
    private final LocalDateTime createdAt;
}
