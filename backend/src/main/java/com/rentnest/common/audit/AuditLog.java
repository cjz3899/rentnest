package com.rentnest.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在管理端敏感操作接口上，成功执行后写审计日志。
 * targetId 支持 SpEL：如 "#id"（方法参数）、"#result.id"（返回值字段）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    String action();

    String targetType() default "";

    String targetId() default "";
}
