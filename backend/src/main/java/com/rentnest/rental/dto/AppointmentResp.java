package com.rentnest.rental.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AppointmentResp {
    /** 预约ID */
    private Long id;
    /** 房源ID */
    private Long houseId;
    /** 房源标题 */
    private String houseTitle;
    /** 房源楼栋 */
    private String houseBuilding;
    /** 房源月租金（元） */
    private BigDecimal houseRent;
    /** 预约人用户ID */
    private Long userId;
    /** 预约人昵称 */
    private String userNickname;
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
}