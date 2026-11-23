package com.zhj.exp4.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhj.exp4.config.AsyncConfig;
import com.zhj.exp4.entity.AuditLog;
import com.zhj.exp4.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 审计日志落库服务。
 *
 * <p>独立成一个 Bean 的原因是 Spring AOP 的代理限制：
 * 同类内部调用 {@code this.record()} 不会走代理，{@code @Async} 不生效。
 * 由切面（另一个 Bean）调用本类，才能进入异步线程池。
 *
 * <p>写入失败只记日志、不影响主业务：审计是旁路能力，不能因为审计表异常导致下单失败。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogRecorder {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    /** 异步写审计表；线程池见 {@link AsyncConfig#AUDIT_EXECUTOR} */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    public void record(AuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            // 兜底：审计写失败不能影响业务，但必须留下痕迹
            log.error("审计日志写入失败 module={} action={}", auditLog.getModule(), auditLog.getAction(), e);
        }
    }

    /**
     * 把入参序列化成 JSON 摘要，超长截断，并过滤密码字段。
     *
     * @param args 方法入参
     * @return 摘要字符串
     */
    public String summarizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        try {
            String json = objectMapper.writeValueAsString(args);
            // 简单脱敏：常见密码字段名统一替换
            json = json.replaceAll("(\"(?:password|oldPassword|newPassword|token)\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
            return json.length() > 1000 ? json.substring(0, 1000) + "...(truncated)" : json;
        } catch (Exception e) {
            return "(参数序列化失败)";
        }
    }
}
