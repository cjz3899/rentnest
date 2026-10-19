package com.rentnest.tenant.enums;

import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.exception.BizException;

public enum AnnouncementType {
    NOTICE, FAQ;

    public static boolean isValid(String value) {
        return value != null && java.util.Arrays.stream(values()).anyMatch(t -> t.name().equals(value));
    }

    public static AnnouncementType parse(String value) {
        if (!isValid(value)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未知的公告类型: " + value + "（NOTICE/FAQ）");
        }
        return AnnouncementType.valueOf(value);
    }
}
