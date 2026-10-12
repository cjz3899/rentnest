package com.rentnest.rental.dto;

import lombok.Data;

@Data
public class AppointmentQuery {
    /** 预约人用户ID，按提交人筛选 */
    private Long userId;
    /** 房源ID，按房源筛选 */
    private Long houseId;
    /** 房东用户ID，按房源 owner 筛选 */
    private Long ownerUserId;
    /** 预约状态筛选 */
    private String status;

    /** 页码，从 1 开始，小于 1 时按 1 处理 */
    private long page = 1;
    /** 每页条数，上限 100 */
    private long size = 20;

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
        return Math.min(size, 100);
    }
}