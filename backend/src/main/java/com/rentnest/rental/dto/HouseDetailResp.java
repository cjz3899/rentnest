package com.rentnest.rental.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HouseDetailResp {
    /** 房源ID */
    private Long id;
    /** 房东/运营人员用户ID */
    private Long ownerUserId;
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
    /** 房源介绍 */
    private String description;
    /** 租赁周期说明 */
    private String leaseTerm;
    /** 入住要求 */
    private String moveInRequirement;
    /** 注意事项 */
    private String notes;
    /** 是否首页推荐 */
    private Boolean isRecommended;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 配套设施列表 */
    private List<String> facilities;
    /** 媒体资源列表（图片/视频，按排序值排列） */
    private List<HouseMediaResp> media;
}