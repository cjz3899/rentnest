package com.rentnest.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileReq {
    @Size(max = 50, message = "昵称过长")
    private String nickname;

    @Size(max = 50, message = "姓名过长")
    private String realName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100)
    private String email;

    private Long avatarFileId;
}
