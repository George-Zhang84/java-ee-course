package com.zhj.menu;

import com.zhj.annotation.RequiresPermission;
import com.zhj.audit.AuditLogger;
import com.zhj.infrastructure.exception.BusinessException;
import com.zhj.infrastructure.exception.PermissionDeniedException;
import com.zhj.model.User;
import com.zhj.service.AuthService;

import java.lang.reflect.Method;

public abstract class BaseMenu {

    protected User currentUser;
    protected AuthService authService = new AuthService();

    public BaseMenu(User currentUser) {
        this.currentUser = currentUser;
    }

    public abstract void show();

    /**
     * 反射执行方法，并自动处理注解校验和异常捕获
     */
    protected void executeMethod(String methodName) {
        try {
            // 通过反射获取当前类的方法
            Method method = this.getClass().getDeclaredMethod(methodName);

            // 检查方法上是否有 @RequiresPermission 注解
            if (method.isAnnotationPresent(RequiresPermission.class)) {
                RequiresPermission anno = method.getAnnotation(RequiresPermission.class);
                String permCode = anno.value();

                // 调用 Service 鉴权
                if (!authService.hasPermission(currentUser, permCode)) {
                    AuditLogger.logPermissionCheck(currentUser.getUsername(), permCode, false);
                    throw new PermissionDeniedException("权限不足，无法执行操作 [" + permCode + "]");
                }
                AuditLogger.logPermissionCheck(currentUser.getUsername(), permCode, true);
            }

            // 允许反射调用子类的 private 方法
            method.setAccessible(true);

            // 校验通过，执行真实方法
            method.invoke(this);

        } catch (NoSuchMethodException e) {
            System.err.println("系统内部错误：找不到方法 " + methodName);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException) {
                System.out.println("✘ " + cause.getMessage());
            } else if (e instanceof BusinessException) {
                System.out.println("✘ " + e.getMessage());
            } else {
                System.out.println("✘ 系统异常：" + e.getMessage());
            }
        }
    }
}