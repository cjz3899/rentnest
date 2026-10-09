package com.rentnest.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_BANNED = "BANNED";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String phone;
    private String passwordHash;
    private String nickname;
    private Long avatarFileId;
    private String role;
    private String status;
    private String realName;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
