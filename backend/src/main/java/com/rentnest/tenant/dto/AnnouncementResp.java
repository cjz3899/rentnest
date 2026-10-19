package com.rentnest.tenant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementResp {
    private Long id;
    private String title;
    private String content;
    private String type;
    private String status;
    private Boolean isPinned;
    private Long publisherId;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
