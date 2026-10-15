package com.rentnest.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.api.Result;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        CurrentUser user = resolveUser(auth);
        if (user == null) {
            // 标注 @AllowAnonymous 的接口（如 PUBLIC 文件下载）放行，权限判断交给业务层
            if (AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), AllowAnonymous.class)) {
                return true;
            }
            return reject(response, ErrorCode.UNAUTHORIZED, 401);
        }

        RequireRole requireRole = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(), RequireRole.class);
        }
        if (requireRole != null && !user.isAdmin()
                && Arrays.stream(requireRole.value()).noneMatch(r -> r.equals(user.getRole()))) {
            return reject(response, ErrorCode.FORBIDDEN, 403);
        }

        UserContextHolder.set(user);
        return true;
    }

    /**
     * token 缺失或失效一律返回 null：调用方决定是拒绝还是按匿名处理
     */
    private CurrentUser resolveUser(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwtUtil.parseAccess(auth.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    private boolean reject(HttpServletResponse response, ErrorCode errorCode, int status) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(errorCode)));
        return false;
    }
}
