package com.zhj.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditLogger {

    private static final Logger logger = LoggerFactory.getLogger("AUDIT");

    private AuditLogger() {
    }

    /**
     * 记录审计日志。操作明细里可能带有用户输入（商品名、店铺名等），
     * 这里统一把换行替换掉，防止伪造日志行（日志注入）。
     */
    public static void log(String operator, String action, String detail) {
        logger.info("操作人: [{}] | 动作: [{}] | 详情: {}",
                safe(operator), safe(action), safe(detail));
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').replace('\n', ' ');
    }
}
