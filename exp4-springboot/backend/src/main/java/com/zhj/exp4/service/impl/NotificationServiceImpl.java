package com.zhj.exp4.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.dto.response.NotificationResponse;
import com.zhj.exp4.entity.Notification;
import com.zhj.exp4.mapper.NotificationMapper;
import com.zhj.exp4.security.SecurityUtils;
import com.zhj.exp4.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 站内通知实现。
 *
 * <p>所有查询都强制带 {@code user_id = 当前用户}，
 * 避免出现"改个 id 就能看别人通知"的越权问题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public void send(Long userId, String type, String title, String content, Long bizId) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setBizId(bizId);
            notification.setReadFlag(0);
            notificationMapper.insert(notification);
        } catch (Exception e) {
            // 通知失败不影响业务，只记录日志
            log.warn("发送站内通知失败 userId={} title={}", userId, title, e);
        }
    }

    @Override
    public PageResult<NotificationResponse> page(int page, int size) {
        Long userId = SecurityUtils.currentUserId();
        Page<Notification> result = notificationMapper.selectPage(
                Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 50)),
                Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime));
        return PageResult.of(result, this::toResponse);
    }

    @Override
    public long unreadCount() {
        Long userId = SecurityUtils.currentUserId();
        Long count = notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, 0));
        return count == null ? 0L : count;
    }

    @Override
    public void markRead(Long id) {
        Long userId = SecurityUtils.currentUserId();
        // 带 user_id 条件，防止标记别人的通知
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getReadFlag, 1));
    }

    @Override
    public void markAllRead() {
        Long userId = SecurityUtils.currentUserId();
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, 0)
                .set(Notification::getReadFlag, 1));
    }

    @Override
    public List<NotificationResponse> latestUnread(int limit) {
        Long userId = SecurityUtils.currentUserId();
        List<Notification> list = notificationMapper.selectList(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, 0)
                .orderByDesc(Notification::getCreateTime)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 20)));
        return list.stream().map(this::toResponse).toList();
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .bizId(n.getBizId())
                .readFlag(n.getReadFlag())
                .createTime(n.getCreateTime())
                .build();
    }
}
