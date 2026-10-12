package com.rentnest.rental.enums;

import java.util.Set;

public enum HouseStatus {
    RENTABLE, RESERVED, RENTED, SUSPENDED;

    /** 用户端（游客/租客）可见的状态 */
    public static final Set<String> PUBLIC_VISIBLE = Set.of(
            RENTABLE.name(), RESERVED.name(), RENTED.name());

    public static boolean isValid(String value) {
        return value != null && java.util.Arrays.stream(values()).anyMatch(s -> s.name().equals(value));
    }
}