package com.rentnest.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repair_ticket")
public class RepairTicket {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    /** 关联房源，可空（允许未绑定） */
    private Long houseId;
    private String location;
    private String description;
    private String contactPhone;
    private String supplement;
    private String status;
    private Long handlerId;
    private String handlerRemark;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
