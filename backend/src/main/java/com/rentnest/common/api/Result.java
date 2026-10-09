package com.rentnest.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Result<T> {
    private final int code;
    private final String msg;
    private final T data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static Result<Void> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> fail(ErrorCode ec) {
        return new Result<>(ec.getCode(), ec.getMsg(), null);
    }

    public static <T> Result<T> fail(ErrorCode ec, String msg) {
        return new Result<>(ec.getCode(), msg, null);
    }
}
