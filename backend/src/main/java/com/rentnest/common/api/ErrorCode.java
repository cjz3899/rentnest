package com.rentnest.common.api;

import lombok.Getter;

@Getter
public enum ErrorCode {
    PARAM_ERROR(40000, "参数错误"),
    UNAUTHORIZED(40100, "未登录或凭证无效"),
    FORBIDDEN(40300, "无权访问"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "状态冲突"),
    RATE_LIMITED(42900, "请求过于频繁"),
    BIZ_ERROR(50000, "业务处理失败");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
