package com.rentnest.common.audit;

import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.auth.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    private final AuditLogMapper auditLogMapper;

    private final ExpressionParser spelParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        Object result = pjp.proceed();
        try {
            record(pjp, auditLog, result);
        } catch (Exception e) {
            log.warn("audit log persist failed, action={}", auditLog.action(), e);
        }
        return result;
    }

    private void record(ProceedingJoinPoint pjp, AuditLog auditLog, Object result) {
        CurrentUser user = UserContextHolder.get();
        if (user == null) {
            return;
        }
        AuditLogEntity entity = new AuditLogEntity();
        entity.setOperatorId(user.getId());
        entity.setOperatorRole(user.getRole());
        entity.setAction(auditLog.action());
        entity.setTargetType(auditLog.targetType());
        entity.setTargetId(evalTargetId(auditLog.targetId(), pjp, result));
        entity.setIp(currentIp());
        entity.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(entity);
    }

    private String evalTargetId(String expr, ProceedingJoinPoint pjp, Object result) {
        if (expr.isBlank()) {
            return null;
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            String[] names = paramNameDiscoverer.getParameterNames(
                    ((MethodSignature) pjp.getSignature()).getMethod());
            Object[] args = pjp.getArgs();
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    context.setVariable(names[i], args[i]);
                }
            }
            context.setVariable("result", result);
            Expression expression = spelParser.parseExpression(expr);
            Object value = expression.getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            log.warn("audit targetId eval failed, expr={}", expr, e);
            return null;
        }
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
