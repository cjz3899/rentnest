package com.rentnest.tenant.enums;

import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.exception.BizException;

public enum AnnouncementStatus {
    PUBLISHED, UNPUBLISHED;

    public static boolean isValid(String value) {
        return value != null && java.util.Arrays.stream(values()).anyMatch(s -> s.name().equals(value));
    }

    public static AnnouncementStatus parse(String value) {
        if (!isValid(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未知的公告状态: " + value);
        }
        return AnnouncementStatus.valueOf(value);
    }
}
