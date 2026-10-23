package com.rentnest.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateReq {
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论不能超过 1000 字")
    private String content;

    /** 被回复的一级评论 id，为空表示发表一级评论 */
    private Long parentId;
}
