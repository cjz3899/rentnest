package com.rentnest.community.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResp {
    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatarUrl;
    private String title;
    /** 列表查询为截断摘要，详情为全文 */
    private String content;
    private String status;
    private Integer likeCount;
    private Integer commentCount;
    /** 当前访问者是否已点赞 */
    private Boolean liked;
    private List<String> mediaUrls = List.of();
    private LocalDateTime createdAt;
}
