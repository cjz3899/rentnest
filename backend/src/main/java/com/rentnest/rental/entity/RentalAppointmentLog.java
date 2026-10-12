package com.rentnest.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rental_appointment_log")
public class RentalAppointmentLog {
    /** 主键ID（雪花ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 关联的预约ID */
    private Long appointmentId;
    /** 流转前状态，可为 NULL */
    private String fromStatus;
    /** 流转后状态 */
    private String toStatus;
    /** 操作人ID */
    private Long operatorId;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createdAt;
}