package com.rentnest.common.auth;

public final class UserContextHolder {
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static CurrentUser require() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new IllegalStateException("当前线程没有用户上下文");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
