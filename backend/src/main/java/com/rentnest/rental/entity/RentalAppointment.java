package com.rentnest.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("rental_appointment")
public class RentalAppointment {
    /** 主键ID（雪花ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 房源ID */
    private Long houseId;
    /** 提交预约的用户ID */
    private Long userId;
    /** 联系人姓名 */
    private String contactName;
    /** 联系方式 */
    private String contactPhone;
    /** 预计入住时间 */
    private LocalDate expectedMoveInDate;
    /** 租赁周期，如 12个月 */
    private String leaseTerm;
    /** 其他备注 */
    private String remark;
    /** 状态：PENDING/CONTACTED/VIEWING/CONFIRMED/COMPLETED/CANCELLED */
    private String status;
    /** 处理人ID（房东/管理员） */
    private Long handlerId;
    /** 处理备注 */
    private String handlerRemark;
    /** 处理时间 */
    private LocalDateTime handledAt;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}