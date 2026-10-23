package com.rentnest.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("post_media")
public class PostMedia {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long postId;
    private Long fileId;
    private Integer sortOrder;
}
