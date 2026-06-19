package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.entity.Notification;
import com.example.dormitoryrepair.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
        AuthContext.setUserId(1L);
        AuthContext.setRole("STUDENT");
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    private Notification createTestNotification(Long id, String title, int isRead) {
        Notification n = new Notification();
        n.setId(id);
        n.setUserId(1L);
        n.setType("STATUS_UPDATE");
        n.setTitle(title);
        n.setContent("测试内容");
        n.setOrderId(10L);
        n.setIsRead(isRead);
        n.setCreateTime(LocalDateTime.now());
        return n;
    }

    @Test
    void pageReturnsNotifications() {
        Page<Notification> page = new Page<>(1, 10);
        page.setRecords(List.of(createTestNotification(1L, "工单已派单", 0)));
        page.setTotal(1);
        when(notificationService.getPage(eq(1L), eq(1), eq(10))).thenReturn(page);

        Map<String, Object> result = controller.page(1, 10).getData();

        List<?> records = (List<?>) result.get("records");
        assertEquals(1, records.size());
    }

    @Test
    void unreadCountReturnsCorrectCount() {
        when(notificationService.getUnreadCount(1L)).thenReturn(5L);

        Long count = controller.unreadCount().getData();

        assertEquals(5L, count);
    }

    @Test
    void markAsReadCallsService() {
        controller.markAsRead(1L);

        verify(notificationService).markAsRead(1L, 1L);
    }

    @Test
    void markAllAsReadCallsService() {
        controller.markAllAsRead();

        verify(notificationService).markAllAsRead(1L);
    }
}
