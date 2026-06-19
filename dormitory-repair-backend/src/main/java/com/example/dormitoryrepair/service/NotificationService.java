package com.example.dormitoryrepair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.dormitoryrepair.entity.Notification;
import com.baomidou.mybatisplus.extension.service.IService;

public interface NotificationService extends IService<Notification> {

    long getUnreadCount(Long userId);

    IPage<Notification> getPage(Long userId, int pageNum, int pageSize);

    void markAsRead(Long id, Long userId);

    void markAllAsRead(Long userId);
}
