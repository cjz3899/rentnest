package com.rentnest.tenant.enums;

import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.exception.BizException;

public enum FeedbackStatus {
    PENDING, REPLIED, CLOSED;

    public boolean canReply() {
        return this != CLOSED;
    }

    public boolean canClose() {
        return this != CLOSED;
    }

    public static FeedbackStatus parse(String value) {
        try {
            return FeedbackStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未知的反馈状态: " + value);
        }
    }
}
