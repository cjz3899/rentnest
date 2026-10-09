package com.rentnest.common.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CurrentUser {
    private final Long id;
    private final String role;

    public boolean isAdmin() {
        return Roles.ADMIN.equals(role);
    }

    public boolean isStaff() {
        return isAdmin() || Roles.LANDLORD.equals(role);
    }
}
