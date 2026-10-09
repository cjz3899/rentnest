package com.rentnest.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rentnest.auth.dto.LoginReq;
import com.rentnest.auth.dto.LoginResp;
import com.rentnest.auth.dto.RegisterReq;
import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.auth.JwtUtil;
import com.rentnest.common.auth.RefreshTokenStore;
import com.rentnest.common.auth.Roles;
import com.rentnest.common.exception.BizException;
import com.rentnest.user.entity.User;
import com.rentnest.user.mapper.UserMapper;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RefreshTokenStore refreshTokenStore;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResp register(RegisterReq req) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, req.getPhone()));
        if (exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该手机号已注册");
        }
        User user = new User();
        user.setPhone(req.getPhone());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setRole(Roles.USER);
        user.setStatus(User.STATUS_ACTIVE);
        userMapper.insert(user);
        return issueTokens(user);
    }

    public LoginResp login(LoginReq req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, req.getPhone()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "手机号或密码错误");
        }
        if (!User.STATUS_ACTIVE.equals(user.getStatus())) {
            refreshTokenStore.revokeAll(user.getId());
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        return issueTokens(user);
    }

    public LoginResp refresh(String refreshToken) {
        JwtUtil.RefreshClaims claims;
        try {
            claims = jwtUtil.parseRefresh(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (!refreshTokenStore.exists(claims.userId(), claims.jti())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        User user = userMapper.selectById(claims.userId());
        if (user == null || !User.STATUS_ACTIVE.equals(user.getStatus())) {
            refreshTokenStore.revokeAll(claims.userId());
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        refreshTokenStore.revoke(user.getId(), claims.jti());
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        try {
            JwtUtil.RefreshClaims claims = jwtUtil.parseRefresh(refreshToken);
            refreshTokenStore.revoke(claims.userId(), claims.jti());
        } catch (JwtException | IllegalArgumentException ignored) {
        }
    }

    private LoginResp issueTokens(User user) {
        String access = jwtUtil.issueAccess(user.getId(), user.getRole());
        JwtUtil.RefreshGrant grant = jwtUtil.issueRefresh(user.getId(), user.getRole());
        refreshTokenStore.save(user.getId(), grant.jti(), jwtUtil.refreshTtl());
        return new LoginResp(access, grant.token(), user.getId(), user.getNickname(), user.getRole());
    }
}
