package com.rentnest.community.enums;

import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.exception.BizException;

/**
 * 帖子与评论共用的内容状态：NORMAL 正常展示，REMOVED 管理端下架
 */
public enum ContentStatus {
    NORMAL, REMOVED;

    public static ContentStatus parse(String value) {
        try {
            return ContentStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未知的内容状态: " + value);
        }
    }
}
