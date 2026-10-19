package com.rentnest.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("feedback_media")
public class FeedbackMedia {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long feedbackId;
    private Long fileId;
    private Integer sortOrder;
}
