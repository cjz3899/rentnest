package com.rentnest.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("repair_media")
public class RepairMedia {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ticketId;
    private Long fileId;
    private Integer sortOrder;
}
