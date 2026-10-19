package com.rentnest.tenant.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FeedbackResp {
    private Long id;
    private Long userId;
    private String userNickname;
    private String content;
    private String problemDesc;
    private String status;
    private String reply;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
    /** 图片附件 URL 列表 */
    private List<String> mediaUrls;
}
