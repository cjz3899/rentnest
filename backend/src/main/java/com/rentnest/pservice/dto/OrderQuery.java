package com.rentnest.pservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderQuery {
    private Long userId;
    private Long productId;
    private String status;
    private LocalDateTime from;
    private LocalDateTime to;

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
