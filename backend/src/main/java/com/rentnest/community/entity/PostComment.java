package com.rentnest.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post_comment")
public class PostComment {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long postId;
    private Long userId;
    /** 被回复的一级评论 id，NULL 表示一级评论 */
    private Long parentId;
    private String content;
    /** NORMAL / REMOVED */
    private String status;
    private LocalDateTime createdAt;
}
