package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计实体（audit_log）。由 {@code AuditLogAspect} 异步写入，
 * 记录"谁在什么时候对什么做了什么、结果如何、耗时多少"。
 *
 * <p>该表只提供查询接口，不提供修改/删除，保证审计链不可篡改。
 */
@Data
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    /** 业务模块，如 商品 / 订单 / 用户 */
    private String module;

    /** 动作描述，如 审核商品 */
    private String action;

    /** 执行方法全名 */
    private String method;

    private String uri;

    private String ip;

    /** 入参摘要（密码等敏感字段已脱敏） */
    private String params;

    /** SUCCESS / FAIL */
    private String result;

    private Integer costMs;

    private LocalDateTime createTime;
}
