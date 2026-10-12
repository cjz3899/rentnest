package com.rentnest.rental.enums;

import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.exception.BizException;

import java.util.Map;
import java.util.Set;

public enum AppointmentStatus {
    /** 待审核 */
    PENDING,
    /** 已联系 */
    CONTACTED,
    /** 看房安排中 */
    VIEWING,
    /** 已确认 */
    CONFIRMED,
    /** 已完成（终态） */
    COMPLETED,
    /** 已取消（终态） */
    CANCELLED;

    /** 状态流转表：key 为当前状态，value 为允许转入的目标状态集合 */
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> TRANSITIONS = Map.of(
            PENDING, Set.of(CONTACTED, CANCELLED),
            CONTACTED, Set.of(VIEWING, CANCELLED),
            VIEWING, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(COMPLETED, CANCELLED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of()
    );

    /**
     * 判断当前状态是否可流转到目标状态
     * @param next 目标状态
     * @return true 表示允许流转
     */
    public boolean canMoveTo(AppointmentStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    /** 房东/管理员可推进的目标状态（不含 CANCELLED，取消由用户或管理端走 next 校验） */
    public static AppointmentStatus parse(String value) {
        try {
            return AppointmentStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(
                    ErrorCode.PARAM_ERROR, "未知的预约状态: " + value);
        }
    }
}