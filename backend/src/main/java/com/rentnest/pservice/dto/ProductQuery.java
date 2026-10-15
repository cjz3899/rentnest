package com.rentnest.pservice.dto;

import lombok.Data;

@Data
public class ProductQuery {
    private String keyword;
    private String status;
    /**
     * true 时含下架商品（管理端）；false 时仅 ON_SALE（用户端）
     */
    private boolean includeOffSale;

    private long page = 1;
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
