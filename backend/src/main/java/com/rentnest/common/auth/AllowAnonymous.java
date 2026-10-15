package com.rentnest.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 方法上：允许匿名访问。
 * 带合法 token 时仍会填充用户上下文，权限判断交由业务方法自行完成
 * （如 PUBLIC 文件人人可读、PRIVATE 文件仅 owner/管理员可读）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowAnonymous {
}
