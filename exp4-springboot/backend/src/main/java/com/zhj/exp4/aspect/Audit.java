package com.zhj.exp4.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计注解。
 *
 * <p>用法：标在 Controller 方法上，切面会自动记录操作人、模块、动作、
 * 入参摘要、耗时与结果，异步写入 {@code audit_log} 表。
 *
 * <pre>{@code
 * @Audit(module = "商品", action = "审核商品")
 * @PostMapping("/api/admin/products/{id}/audit")
 * public Result<Void> audit(...) { ... }
 * }</pre>
 *
 * <p>与实体类 {@code AuditLog} 区分命名，避免同包同名混淆。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /** 业务模块，如 商品 / 订单 / 用户 */
    String module();

    /** 动作描述，如 审核商品 */
    String action();

    /** 是否记录入参摘要；涉及密码的接口应设为 false */
    boolean saveParams() default true;
}
