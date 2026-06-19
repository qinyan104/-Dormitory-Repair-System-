package com.example.dormitoryrepair.service;

import com.example.dormitoryrepair.dto.ai.RecommendResponse;
import com.example.dormitoryrepair.dto.websocket.NotificationMessage;
import com.example.dormitoryrepair.entity.Notification;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles rule-based AI automation that should continue working even when the LLM
 * provider is unavailable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoAssignScheduler {

    private final RepairOrderService repairOrderService;
    private final SysUserService sysUserService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AiService aiService;

    @Value("${app.ai.auto-assign.enabled:true}")
    private boolean autoAssignEnabled;

    @Value("${app.ai.reminders.enabled:true}")
    private boolean reminderEnabled;

    @Value("${app.ai.reminders.worker-accept-minutes:120}")
    private long workerAcceptReminderMinutes;

    @Value("${app.ai.reminders.student-confirm-minutes:1440}")
    private long studentConfirmReminderMinutes;

    @Value("${app.ai.reminders.cooldown-minutes:240}")
    private long reminderCooldownMinutes;

    private final Map<String, LocalDateTime> reminderCooldowns = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 60_000)
    public void autoProcessPendingOrders() {
        if (!autoAssignEnabled) {
            return;
        }

        List<RepairOrder> pendingOrders = repairOrderService.lambdaQuery()
                .eq(RepairOrder::getRepairStatus, 1)
                .list();
        if (pendingOrders.isEmpty()) {
            return;
        }

        int autoAssigned = 0;
        int skipped = 0;
        for (RepairOrder order : pendingOrders) {
            try {
                RecommendResponse ai = aiService.recommendWorker(order.getId());
                if (ai == null || !Boolean.TRUE.equals(ai.getAutoAssigned()) || ai.getAssignedWorkerId() == null) {
                    skipped++;
                    continue;
                }

                order.setRepairStatus(2);
                order.setAcceptTime(LocalDateTime.now());
                order.setWorkerId(ai.getAssignedWorkerId());
                order.setAssignTime(LocalDateTime.now());
                if (!repairOrderService.updateById(order)) {
                    skipped++;
                    continue;
                }

                SysUser worker = sysUserService.getById(ai.getAssignedWorkerId());
                String workerName = worker != null ? worker.getRealName() : "维修人员";
                notifyUser(order.getUserId(), "工单已自动派单",
                        "您的报修已自动派单给" + workerName, order.getId());
                notifyUser(ai.getAssignedWorkerId(), "收到新的维修工单",
                        "系统已为您分配工单：" + order.getTitle(), order.getId());

                log.info("Auto-assigned order #{} [{}] to worker {}",
                        order.getId(), order.getTitle(), workerName);
                autoAssigned++;
            } catch (Exception e) {
                log.error("Auto-assign failed for order #{}: {}", order.getId(), e.getMessage());
                skipped++;
            }
        }

        if (autoAssigned > 0 || skipped > 0) {
            log.info("Auto-assign round: {} assigned, {} skipped (out of {} pending)",
                    autoAssigned, skipped, pendingOrders.size());
        }
    }

    @Scheduled(fixedDelay = 300_000)
    public void remindStalledOrders() {
        if (!reminderEnabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<RepairOrder> waitingWorkerAccept = repairOrderService.lambdaQuery()
                .eq(RepairOrder::getRepairStatus, 2)
                .isNotNull(RepairOrder::getWorkerId)
                .le(RepairOrder::getAssignTime, now.minusMinutes(workerAcceptReminderMinutes))
                .list();
        for (RepairOrder order : waitingWorkerAccept) {
            if (order.getWorkerId() == null) {
                continue;
            }
            remindWithCooldown(
                    "worker-accept-" + order.getId(),
                    order.getWorkerId(),
                    "工单待接单提醒",
                    "工单《" + order.getTitle() + "》已派单较久，请尽快接单处理。",
                    order.getId()
            );
        }

        List<RepairOrder> waitingStudentConfirm = repairOrderService.lambdaQuery()
                .eq(RepairOrder::getRepairStatus, 4)
                .le(RepairOrder::getWorkerCompleteTime, now.minusMinutes(studentConfirmReminderMinutes))
                .list();
        for (RepairOrder order : waitingStudentConfirm) {
            remindWithCooldown(
                    "student-confirm-" + order.getId(),
                    order.getUserId(),
                    "工单待确认提醒",
                    "工单《" + order.getTitle() + "》已完成，请及时确认维修结果。",
                    order.getId()
            );
        }
    }

    private void remindWithCooldown(String key, Long userId, String title, String content, Long orderId) {
        if (userId == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSentAt = reminderCooldowns.get(key);
        if (lastSentAt != null && lastSentAt.plusMinutes(reminderCooldownMinutes).isAfter(now)) {
            return;
        }

        notifyUser(userId, title, content, orderId);
        reminderCooldowns.put(key, now);
    }

    private void notifyUser(Long userId, String title, String content, Long orderId) {
        if (userId == null) {
            return;
        }

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notification",
                new NotificationMessage("STATUS_UPDATE", title, content, orderId, System.currentTimeMillis()));

        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType("STATUS_UPDATE");
            notification.setTitle(title);
            notification.setContent(content);
            notification.setOrderId(orderId);
            notification.setIsRead(0);
            notification.setCreateTime(LocalDateTime.now());
            notificationService.save(notification);
        } catch (Exception e) {
            log.error("Failed to save scheduler notification for user {} (order {}): {}",
                    userId, orderId, e.getMessage());
        }
    }
}
