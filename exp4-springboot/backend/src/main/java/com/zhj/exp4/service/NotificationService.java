package com.zhj.exp4.service;

import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.dto.response.NotificationResponse;

import java.util.List;

/**
 * 站内通知服务。
 *
 * <p>通知是"尽力而为"的旁路能力：发送失败只记日志，绝不影响业务主流程。
 */
public interface NotificationService {

    /**
     * 发送通知。
     *
     * @param userId  接收人
     * @param type    ORDER / BARGAIN / AUDIT / SYSTEM
     * @param title   标题
     * @param content 内容
     * @param bizId   关联业务 id
     */
    void send(Long userId, String type, String title, String content, Long bizId);

    /** 我的通知分页 */
    PageResult<NotificationResponse> page(int page, int size);

    /** 未读数（前端顶部铃铛红点） */
    long unreadCount();

    /** 标记单条已读 */
    void markRead(Long id);

    /** 全部标记已读 */
    void markAllRead();

    /** 最近 N 条未读（登录后弹窗提醒用） */
    List<NotificationResponse> latestUnread(int limit);
}
