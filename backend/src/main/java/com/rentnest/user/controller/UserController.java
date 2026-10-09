package com.rentnest.user.controller;

import com.rentnest.common.api.Result;
import com.rentnest.user.service.UserService;
import com.rentnest.user.dto.ProfileResp;
import com.rentnest.user.dto.UpdateProfileReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public Result<ProfileResp> myProfile() {
        return Result.ok(userService.myProfile());
    }

    @PutMapping("/profile")
    public Result<ProfileResp> updateMyProfile(@Valid @RequestBody UpdateProfileReq req) {
        return Result.ok(userService.updateMyProfile(req));
    }
}
