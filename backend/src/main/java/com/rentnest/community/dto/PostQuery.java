package com.rentnest.community.dto;

import lombok.Data;

@Data
public class PostQuery {
    private Long userId;
    private String status;
    private String keyword;
    /** 当前访问者，用于批量判断是否已点赞；由服务端覆盖 */
    private Long viewerId;

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
