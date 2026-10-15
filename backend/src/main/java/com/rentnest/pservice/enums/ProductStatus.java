package com.rentnest.pservice.enums;

import java.util.Arrays;

public enum ProductStatus {
    ON_SALE,
    OFF_SALE;

    public static boolean isValid(String value) {
        return value != null && Arrays.stream(values()).anyMatch(s -> s.name().equals(value));
    }
}
