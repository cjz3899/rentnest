package com.rentnest.user.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.audit.AuditLog;
import com.rentnest.common.auth.RequireRole;
import com.rentnest.common.auth.Roles;
import com.rentnest.user.service.UserService;
import com.rentnest.user.dto.ProfileResp;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@RequireRole(Roles.ADMIN)
public class UserAdminController {
    private final UserService userService;

    @GetMapping("/{id}/contact")
    @AuditLog(action = "VIEW_USER_CONTACT", targetType = "USER", targetId = "#id")
    public Result<ProfileResp> getContact(@PathVariable Long id) {
        return Result.ok(userService.adminGetContact(id));
    }
}
