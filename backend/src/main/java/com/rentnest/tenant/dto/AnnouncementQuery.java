package com.rentnest.tenant.dto;

import lombok.Data;

@Data
public class AnnouncementQuery {
    /** NOTICE / FAQ，空为全部 */
    private String type;
    private String status;
    /** true 时含未发布（管理端）；false 时仅已发布（用户端） */
    private boolean includeUnpublished;

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
