package com.rentnest.tenant.dto;

import lombok.Data;

@Data
public class FeedbackQuery {
    private Long userId;
    private String status;

    private long page = 1;
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
