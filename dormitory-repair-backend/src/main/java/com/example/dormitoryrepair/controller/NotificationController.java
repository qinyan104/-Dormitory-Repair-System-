package com.example.dormitoryrepair.controller;

import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.entity.Notification;
import com.example.dormitoryrepair.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = AuthContext.getUserId();
        var result = notificationService.getPage(userId, pageNum, pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return ApiResponse.success(data);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        Long userId = AuthContext.getUserId();
        return ApiResponse.success(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/read/{id}")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        Long userId = AuthContext.getUserId();
        notificationService.markAsRead(id, userId);
        return ApiResponse.success("已标记为已读", null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        Long userId = AuthContext.getUserId();
        notificationService.markAllAsRead(userId);
        return ApiResponse.success("全部已读", null);
    }
}
