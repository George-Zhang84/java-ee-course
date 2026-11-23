package com.zhj.exp4.aspect;

import com.zhj.exp4.entity.AuditLog;
import com.zhj.exp4.security.LoginUser;
import com.zhj.exp4.security.SecurityUtils;
import com.zhj.exp4.service.AuditLogRecorder;
import com.zhj.exp4.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计切面：拦截标注了 {@link Audit} 的方法，记录操作痕迹。
 *
 * <p>这里用"代理模式"在运行期织入横切逻辑——业务代码只多一个注解，
 * 审计、耗时统计与异常标记全部由切面完成，不污染 Service。
 *
 * <p>实现要点：
 * <ul>
 *   <li>用 {@code @Around} 而非 {@code @AfterReturning}，以便同时记录失败调用与耗时；</li>
 *   <li>入参摘要由 {@link AuditLogRecorder#summarizeArgs} 脱敏与截断；</li>
 *   <li>落库交给异步线程池，审计 IO 不拖慢接口响应；</li>
 *   <li>登录接口此时尚未认证，用户 id 允许为空。</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRecorder recorder;

    @Around("@annotation(auditAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Audit auditAnnotation) throws Throwable {
        long start = System.currentTimeMillis();
        String result = "SUCCESS";
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            result = "FAIL";
            throw e;
        } finally {
            try {
                writeLog(joinPoint, auditAnnotation, result, (int) (System.currentTimeMillis() - start));
            } catch (Exception e) {
                // 审计自身异常不得影响主流程
                log.warn("构建审计记录失败：{}", e.getMessage());
            }
        }
    }

    private void writeLog(ProceedingJoinPoint joinPoint, Audit annotation, String result, int costMs) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AuditLog entity = new AuditLog();
        entity.setModule(annotation.module());
        entity.setAction(annotation.action());
        entity.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());
        entity.setResult(result);
        entity.setCostMs(costMs);
        entity.setCreateTime(LocalDateTime.now());

        // 用户信息：登录接口未认证时为空，记 anonymous
        try {
            LoginUser loginUser = SecurityUtils.currentUser();
            entity.setUserId(loginUser.getUserId());
            entity.setUsername(loginUser.getUsername());
        } catch (Exception ignored) {
            entity.setUsername("anonymous");
        }

        // 请求信息：切面也可能在非 Web 场景（单测）被调用，需空判断
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            entity.setUri(request.getMethod() + " " + request.getRequestURI());
            entity.setIp(IpUtil.getClientIp(request));
        }

        if (annotation.saveParams()) {
            entity.setParams(recorder.summarizeArgs(joinPoint.getArgs()));
        }
        recorder.record(entity);
    }
}
