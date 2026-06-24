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
    private final RepairCategoryService repairCategoryService;
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

    @Scheduled(fixedDelay = 300_000)
    public void checkAdminAlerts() {
        if (!reminderEnabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoHoursAgo = now.minusHours(2);
        LocalDateTime oneDayAgo = now.minusHours(24);
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        // 1. 紧急工单超过2小时未受理 → 提醒管理员
        List<RepairOrder> urgentTimeout = repairOrderService.lambdaQuery()
                .eq(RepairOrder::getRepairStatus, 1)
                .eq(RepairOrder::getUrgency, "紧急")
                .le(RepairOrder::getSubmitTime, twoHoursAgo)
                .list();
        for (RepairOrder order : urgentTimeout) {
            remindWithCooldown(
                    "admin-urgent-timeout-" + order.getId(),
                    null, // null means notify admin via broadcast
                    "⚠️ 紧急工单超时",
                    "工单《" + order.getTitle() + "》已超过2小时未受理，请尽快处理。",
                    order.getId()
            );
        }

        // 2. 普通工单超过24小时未受理 → 提醒管理员
        List<RepairOrder> normalTimeout = repairOrderService.lambdaQuery()
                .eq(RepairOrder::getRepairStatus, 1)
                .ne(RepairOrder::getUrgency, "紧急")
                .le(RepairOrder::getSubmitTime, oneDayAgo)
                .list();
        for (RepairOrder order : normalTimeout) {
            remindWithCooldown(
                    "admin-normal-timeout-" + order.getId(),
                    null,
                    "⏰ 工单超时未受理",
                    "工单《" + order.getTitle() + "》已超过24小时未受理。",
                    order.getId()
            );
        }

        // 3. 同一宿舍7天内同类报修≥3次 → 标记重复故障
        List<RepairOrder> recentOrders = repairOrderService.lambdaQuery()
                .ge(RepairOrder::getSubmitTime, sevenDaysAgo)
                .in(RepairOrder::getRepairStatus, 1, 2, 3, 4, 5)
                .list();
        // 批量预加载用户信息避免 N+1
        Map<Long, SysUser> userMap = new java.util.HashMap<>();
        if (!recentOrders.isEmpty()) {
            List<Long> uids = recentOrders.stream().map(RepairOrder::getUserId).filter(java.util.Objects::nonNull).distinct().toList();
            if (!uids.isEmpty()) {
                sysUserService.listByIds(uids).forEach(u -> userMap.put(u.getId(), u));
            }
        }
        Map<String, Long> dormCategoryCount = new java.util.HashMap<>();
        for (RepairOrder o : recentOrders) {
            SysUser u = userMap.get(o.getUserId());
            if (u != null && u.getDormitoryBuilding() != null && o.getCategoryId() != null) {
                String key = u.getDormitoryBuilding() + "|" + u.getRoomNo() + "|" + o.getCategoryId();
                dormCategoryCount.merge(key, 1L, Long::sum);
            }
        }
        for (Map.Entry<String, Long> entry : dormCategoryCount.entrySet()) {
            if (entry.getValue() >= 3) {
                String[] parts = entry.getKey().split("\\|");
                if (parts.length == 3) {
                    remindWithCooldown(
                            "repeat-fault-" + entry.getKey(),
                            null,
                            "🔁 重复故障提醒",
                            "7天内" + parts[0] + parts[1] + "已报修同类问题" + entry.getValue() + "次，建议排查。",
                            null
                    );
                }
            }
        }

        // 4. 同一分类7天内报修≥8次 → 高频故障
        Map<Long, Long> categoryCount = new java.util.HashMap<>();
        for (RepairOrder o : recentOrders) {
            if (o.getCategoryId() != null) {
                categoryCount.merge(o.getCategoryId(), 1L, Long::sum);
            }
        }
        for (Map.Entry<Long, Long> entry : categoryCount.entrySet()) {
            if (entry.getValue() >= 8) {
                String catName = "该类型";
                try {
                    var cat = repairCategoryService.getById(entry.getKey());
                    if (cat != null) catName = cat.getCategoryName();
                } catch (Exception ignored) {}
                remindWithCooldown(
                        "high-freq-" + entry.getKey(),
                        null,
                        "📈 高频故障提醒",
                        "7天内" + catName + "类报修达" + entry.getValue() + "次，请关注。",
                        null
                );
            }
        }
    }

    private void remindWithCooldown(String key, Long userId, String title, String content, Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSentAt = reminderCooldowns.get(key);
        if (lastSentAt != null && lastSentAt.plusMinutes(reminderCooldownMinutes).isAfter(now)) {
            return;
        }

        if (userId != null) {
            notifyUser(userId, title, content, orderId);
        } else {
            notifyAdmin(title, content, orderId);
        }
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

    private void notifyAdmin(String title, String content, Long orderId) {
        messagingTemplate.convertAndSend("/topic/admin/alert",
                new NotificationMessage("ADMIN_ALERT", title, content, orderId, System.currentTimeMillis()));

        try {
            Notification notification = new Notification();
            notification.setUserId(0L); // admin alert sent to virtual user 0
            notification.setType("ADMIN_ALERT");
            notification.setTitle(title);
            notification.setContent(content);
            notification.setOrderId(orderId);
            notification.setIsRead(0);
            notification.setCreateTime(LocalDateTime.now());
            notificationService.save(notification);
        } catch (Exception e) {
            log.error("Failed to save admin alert: {}", e.getMessage());
        }
    }
}
