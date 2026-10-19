package com.rentnest.tenant.enums;

import java.util.Map;
import java.util.Set;

public enum RepairStatus {
    PENDING, PROCESSING, COMPLETED, CLOSED;

    private static final Map<RepairStatus, Set<RepairStatus>> TRANSITIONS = Map.of(
            PENDING, Set.of(PROCESSING, CLOSED),
            PROCESSING, Set.of(COMPLETED, CLOSED),
            COMPLETED, Set.of(CLOSED),
            CLOSED, Set.of()
    );

    public boolean canMoveTo(RepairStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    public static RepairStatus parse(String value) {
        try {
            return RepairStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new com.rentnest.common.exception.BizException(
                    com.rentnest.common.api.ErrorCode.PARAM_ERROR, "未知的报修状态: " + value);
        }
    }
}
