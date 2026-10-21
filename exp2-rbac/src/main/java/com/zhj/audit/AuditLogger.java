package com.zhj.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 审计日志工具类 (专门记录权限校验和关键业务)
 */
public class AuditLogger {
    private static final Logger logger = LoggerFactory.getLogger("AUDIT");

    /**
     * 记录权限校验日志
     *
     * @param username 操作人
     * @param action   尝试执行的动作(方法名或权限码)
     * @param success  是否成功
     */
    public static void logPermissionCheck(String username, String action, boolean success) {
        if (success) {
            logger.info("权限校验通过 - 用户:[{}] 操作:[{}]", username, action);
        } else {
            logger.warn("权限校验失败(越权) - 用户:[{}] 操作:[{}]", username, action);
        }
    }

    /**
     * 记录普通业务操作
     */
    public static void logBusiness(String username, String action) {
        logger.info("业务操作 - 用户:[{}] 执行了:[{}]", username, action);
    }
}