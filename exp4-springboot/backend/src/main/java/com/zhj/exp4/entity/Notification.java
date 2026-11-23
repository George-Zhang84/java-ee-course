package com.zhj.exp4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体（notification）。订单状态变化、议价结果、审核结果都会生成一条，
 * 前端顶部铃铛按 {@code readFlag=0} 显示未读数。
 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人 */
    private Long userId;

    /** ORDER / BARGAIN / AUDIT / SYSTEM */
    private String type;

    private String title;

    private String content;

    /** 关联业务 id，前端点击通知可据此跳转 */
    private Long bizId;

    /** 0 未读 / 1 已读 */
    private Integer readFlag;

    private LocalDateTime createTime;
}
