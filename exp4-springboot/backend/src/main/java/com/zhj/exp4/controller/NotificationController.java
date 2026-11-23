package com.zhj.exp4.controller;

import com.zhj.exp4.common.PageResult;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.dto.response.NotificationResponse;
import com.zhj.exp4.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 站内通知接口。所有操作都限定在当前用户范围内。
 */
@Tag(name = "10. 通知", description = "站内消息与未读数")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "通知分页")
    @GetMapping
    public Result<PageResult<NotificationResponse>> list(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return Result.ok(notificationService.page(page, size));
    }

    @Operation(summary = "未读数量")
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.ok(Map.of("count", notificationService.unreadCount()));
    }

    @Operation(summary = "最近未读通知")
    @GetMapping("/unread")
    public Result<List<NotificationResponse>> unread(@RequestParam(defaultValue = "5") int limit) {
        return Result.ok(notificationService.latestUnread(limit));
    }

    @Operation(summary = "标记单条已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.ok("已标记为已读", null);
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.ok("已全部标记为已读", null);
    }
}
