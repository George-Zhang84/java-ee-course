package com.zhj.exp2.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 * 贴在需要权限控制的菜单方法上
 */
@Target(ElementType.METHOD) // 作用在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时保留，方便反射读取
public @interface RequiresPermission {
    String value(); // 权限码，如 "user:delete"
}