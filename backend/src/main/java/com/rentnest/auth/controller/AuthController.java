package com.rentnest.auth.controller;

import com.rentnest.auth.service.AuthService;
import com.rentnest.auth.dto.LoginReq;
import com.rentnest.auth.dto.LoginResp;
import com.rentnest.auth.dto.RegisterReq;
import com.rentnest.auth.dto.TokenReq;
import com.rentnest.common.api.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public Result<LoginResp> register(@Valid @RequestBody RegisterReq req) {
        return Result.ok(authService.register(req));
    }

    @PostMapping("/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public Result<LoginResp> refresh(@Valid @RequestBody TokenReq req) {
        return Result.ok(authService.refresh(req.getRefreshToken()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@Valid @RequestBody TokenReq req) {
        authService.logout(req.getRefreshToken());
        return Result.ok();
    }
}
