package com.rentnest.rental.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("house")
public class House {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 房东用户ID
     */
    private Long ownerUserId;

    /**
     * 房屋标题
     */
    private String title;
    /**
     * 楼盘名称
     */
    private String building;
    /**
     * 楼层
     */
    private String floorNo;
    /**
     * 朝向
     */
    private String orientation;
    /**
     * 房型，如 一室一厅
     */
    private String layout;
    /**
     * 面积
     */
    private BigDecimal area;
    /**
     * 租金
     */
    private BigDecimal rent;
    /**
     * 保证金
     */
    private BigDecimal deposit;
    /**
     * 描述
     */
    private String description;
    /**
     * 状态
     */
    private String status;
    /**
     * 租赁期限
     */
    private String leaseTerm;
    /**
     * 移居要求
     */
    private String moveInRequirement;
    /**
     * 备注
     */
    private String notes;
    /**
     * 是否推荐
     */
    private Boolean isRecommended;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
