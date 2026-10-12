package com.rentnest.rental.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class HouseQuery {
    /** 房型，如 一室一厅 */
    private String layout;
    /** 楼栋编号 */
    private String building;
    /** 房源状态：RENTABLE/RESERVED/RENTED/SUSPENDED */
    private String status;
    /** 关键词模糊搜索（标题等） */
    private String keyword;
    /** 月租金下限（元） */
    private BigDecimal minRent;
    /** 月租金上限（元） */
    private BigDecimal maxRent;
    /** 面积下限（㎡） */
    private BigDecimal minArea;
    /** 面积上限（㎡） */
    private BigDecimal maxArea;
    /** 配套设施列表，需全部匹配 */
    private List<String> facilities;

    /** 房东用户ID，按房东筛选 */
    private Long ownerUserId;
    /** 仅查询首页推荐房源 */
    private boolean recommendedOnly;
    /** true 时结果包含 SUSPENDED（管理端）；false 为用户端可见集 */
    private boolean includeSuspended;

    /** 页码，从 1 开始，小于 1 时按 1 处理 */
    private long page = 1;
    /** 每页条数，上限 50 */
    private long size = 12;

    public long getOffset() {
        return (getPage() - 1) * getSize();
    }

    public long getPage() {
        return page < 1 ? 1 : page;
    }

    public long getSize() {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, 50);
    }
}