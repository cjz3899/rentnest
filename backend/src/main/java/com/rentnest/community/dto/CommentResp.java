package com.rentnest.community.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentResp {
    private Long id;
    private Long postId;
    private Long userId;
    private String userNickname;
    private String userAvatarUrl;
    private Long parentId;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    /** 一级评论下的回复，按时间正序 */
    private List<CommentResp> replies = List.of();
    /** 管理端列表展示所属帖子，用户端为空 */
    private String postTitle;
}
