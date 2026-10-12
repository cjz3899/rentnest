package com.rentnest.rental.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HouseListItemResp {
    /** 房源ID */
    private Long id;
    /** 房间名称 */
    private String title;
    /** 楼栋编号 */
    private String building;
    /** 所在楼层 */
    private String floorNo;
    /** 朝向 */
    private String orientation;
    /** 房型，如 一室一厅 */
    private String layout;
    /** 面积（㎡） */
    private BigDecimal area;
    /** 月租金（元） */
    private BigDecimal rent;
    /** 押金（元） */
    private BigDecimal deposit;
    /** 房源状态：RENTABLE/RESERVED/RENTED/SUSPENDED */
    private String status;
    /** 是否首页推荐 */
    private Boolean isRecommended;
    /** 封面图 URL */
    private String coverUrl;
    /** 创建时间 */
    private LocalDateTime createdAt;
}