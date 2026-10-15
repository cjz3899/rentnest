package com.rentnest.pservice.enums;

import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.exception.BizException;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    CREATED,
    DELIVERING,
    COMPLETED,
    CANCELLED;

    /**
     * 订单状态转换关系
     */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            CREATED, Set.of(DELIVERING, CANCELLED),
            DELIVERING, Set.of(COMPLETED, CANCELLED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of()
    );

    /**
     * 判断订单状态是否可以转换到下一个状态
     *
     * @param next 下一个状态
     * @return 是否可以转换
     */
    public boolean canMoveTo(OrderStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    /**
     * 根据字符串值解析订单状态
     *
     * @param value 字符串值
     * @return 订单状态
     */
    public static OrderStatus parse(String value) {
        try {
            return OrderStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(
                    ErrorCode.PARAM_ERROR, "未知的订单状态: " + value);
        }
    }
}
