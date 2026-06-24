package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dormitoryrepair.common.annotation.Log;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.common.util.DateTimeRangeParser;
import com.example.dormitoryrepair.common.util.ExcelExportUtil;
import com.example.dormitoryrepair.common.util.IdempotencyHelper;
import com.example.dormitoryrepair.dto.repair.RepairOrderCreateRequest;
import com.example.dormitoryrepair.dto.repair.RepairOrderQueryRequest;
import com.example.dormitoryrepair.dto.repair.RepairOrderStatusRequest;
import com.example.dormitoryrepair.dto.websocket.NotificationMessage;
import com.example.dormitoryrepair.dto.ai.EvaluationResponse;
import com.example.dormitoryrepair.dto.ai.RecommendResponse;
import com.example.dormitoryrepair.entity.Notification;
import com.example.dormitoryrepair.entity.RepairCategory;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.entity.SysUser;
import com.example.dormitoryrepair.service.AiService;
import com.example.dormitoryrepair.service.RepairCategoryService;
import com.example.dormitoryrepair.service.NotificationService;
import com.example.dormitoryrepair.service.RepairOrderService;
import com.example.dormitoryrepair.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/repair-order")
@RequiredArgsConstructor
public class RepairOrderController {

    private final RepairOrderService repairOrderService;
    private final RepairCategoryService repairCategoryService;
    private final SysUserService sysUserService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final AiService aiService;
    private final IdempotencyHelper idempotencyHelper;

    @Value("${app.ai.completion-review.enabled:true}")
    private boolean completionReviewEnabled;

    // ==================== 学生端 ====================

    @Transactional
    @Log(type = "REPAIR", value = "学生提交报修")
@PostMapping
    public ApiResponse<RepairOrder> create(@Valid @RequestBody RepairOrderCreateRequest request,
                                           @org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        idempotencyHelper.claim(idempotencyKey, "报修提交");
        Long userId = AuthContext.getUserId();
        RepairCategory category = repairCategoryService.getById(request.getCategoryId());
        if (category == null || Objects.equals(category.getStatus(), 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "报修分类不存在或已停用");
        }

        RepairOrder order = new RepairOrder();
        order.setOrderNo("RO" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        order.setUserId(userId);
        order.setCategoryId(request.getCategoryId());
        order.setTitle(request.getTitle());
        order.setContent(request.getContent());
        order.setImageUrl(request.getImageUrl());
        order.setUrgency(request.getUrgency());
        order.setRepairStatus(1);
        order.setSubmitTime(LocalDateTime.now());
        if (request.getAiPriorityScore() != null) {
            order.setPriorityScore(request.getAiPriorityScore());
        }
        if (request.getAiImpactScope() != null) {
            order.setImpactScope(request.getAiImpactScope());
        }
        repairOrderService.save(order);
        messagingTemplate.convertAndSend("/topic/admin/new-order",
                new NotificationMessage("NEW_ORDER", "新报修工单", order.getTitle(), order.getId(), System.currentTimeMillis()));
        return ApiResponse.success(order);
    }

    @GetMapping("/my-page")
    public ApiResponse<Map<String, Object>> myPage(RepairOrderQueryRequest request) {
        Long userId = AuthContext.getUserId();
        LambdaQueryWrapper<RepairOrder> wrapper = buildWrapper(request)
                .eq(RepairOrder::getUserId, userId)
                .orderByDesc(RepairOrder::getId)
                .orderByDesc(RepairOrder::getSubmitTime);
        IPage<RepairOrder> result = repairOrderService.page(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", buildOrderViews(result.getRecords()));
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return ApiResponse.success(data);
    }

    @Transactional
    @Log(type = "REPAIR", value = "撤销报修")
@PutMapping("/cancel/{id}")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        RepairOrder order = repairOrderService.getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ensureSelfOrAdmin(order.getUserId());
        if (!Objects.equals(order.getRepairStatus(), 1) && !Objects.equals(order.getRepairStatus(), 2)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前状态不允许撤销");
        }
        order.setRepairStatus(6);
        order.setCancelTime(LocalDateTime.now());
        if (!repairOrderService.updateById(order)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工单已被其他人操作，请刷新后重试");
        }
        return ApiResponse.success("撤销成功", null);
    }

    @Transactional
    @Log(type = "REPAIR", value = "学生确认完成")
@PutMapping("/student-confirm/{id}")
    public ApiResponse<Void> studentConfirm(@PathVariable Long id) {
        RepairOrder order = repairOrderService.getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ensureSelfOrAdmin(order.getUserId());
        if (!Objects.equals(order.getRepairStatus(), 4)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前状态不允许确认完成");
        }
        order.setRepairStatus(5);
        order.setStudentConfirmTime(LocalDateTime.now());
        if (order.getFinishTime() == null) {
            order.setFinishTime(LocalDateTime.now());
        }
        if (!repairOrderService.updateById(order)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工单已被其他人操作，请刷新后重试");
        }
        if (order.getWorkerId() != null) {
            pushToWorker(order.getWorkerId(), "工单已确认完成", "学生已确认工单完成。", order.getId());
        }
        return ApiResponse.success("确认成功", null);
    }

    // ==================== 管理员端 ====================

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(RepairOrderQueryRequest request) {
        ensureAdmin();
        LambdaQueryWrapper<RepairOrder> wrapper = buildWrapper(request)
                .orderByDesc(RepairOrder::getId)
                .orderByDesc(RepairOrder::getSubmitTime);
        IPage<RepairOrder> result = repairOrderService.page(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", buildOrderViews(result.getRecords()));
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return ApiResponse.success(data);
    }

    @Transactional
    @Log(type = "REPAIR", value = "管理员受理报修")
@PutMapping("/accept/{id}")
    public ApiResponse<Map<String, Object>> accept(@PathVariable Long id) {
        ensureAdmin();
        RepairOrder order = repairOrderService.getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!Objects.equals(order.getRepairStatus(), 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前状态不允许受理");
        }
        order.setAcceptTime(LocalDateTime.now());
        order.setHandlerId(AuthContext.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", id);

        // AI 自动派单
        RecommendResponse ai = aiService.recommendWorker(id);
        if (ai != null && Boolean.TRUE.equals(ai.getAutoAssigned())) {
            order.setRepairStatus(2);
            order.setWorkerId(ai.getAssignedWorkerId());
            order.setAssignTime(LocalDateTime.now());
            repairOrderService.updateById(order);
            SysUser worker = sysUserService.getById(ai.getAssignedWorkerId());
            pushToWorker(ai.getAssignedWorkerId(), "新工单指派",
                    "您有一份新的维修工单：" + order.getTitle(), order.getId());
            pushToStudent(order.getUserId(), "工单已派单",
                    "您的报修已指派给" + (worker != null ? worker.getRealName() : "维修人员"), order.getId());
            result.put("autoAssigned", true);
            result.put("workerName", worker != null ? worker.getRealName() : "");
        } else if (ai != null && !ai.getRankings().isEmpty()) {
            order.setRepairStatus(1);
            repairOrderService.updateById(order);
            result.put("autoAssigned", false);
            result.put("rankings", ai.getRankings());
        } else {
            order.setRepairStatus(1);
            repairOrderService.updateById(order);
            result.put("autoAssigned", false);
        }

        return ApiResponse.success("受理成功", result);
    }

    @Transactional
    @Log(type = "REPAIR", value = "管理员指派维修")
@PutMapping("/assign/{id}")
    public ApiResponse<Void> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        ensureAdmin();
        Long workerId = body.get("workerId");
        if (workerId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请选择维修人员");
        }
        RepairOrder order = repairOrderService.getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        boolean canAssignPending = Objects.equals(order.getRepairStatus(), 1);
        boolean canAssignAcceptedWithoutWorker = Objects.equals(order.getRepairStatus(), 2) && order.getWorkerId() == null;
        if (!canAssignPending && !canAssignAcceptedWithoutWorker) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前状态不允许指派");
        }
        SysUser worker = sysUserService.getById(workerId);
        if (worker == null || !"REPAIRER".equals(worker.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "维修人员不存在");
        }
        order.setRepairStatus(2);
        order.setWorkerId(workerId);
        order.setHandlerId(AuthContext.getUserId());
        order.setAssignTime(LocalDateTime.now());
        if (!repairOrderService.updateById(order)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工单已被其他人操作，请刷新后重试");
        }
        pushToWorker(workerId, "新工单指派", "您有一份新的维修工单：" + order.getTitle(), order.getId());
        pushToStudent(order.getUserId(), "工单已派单", "您的报修已被受理，维修人员正在赶来。", order.getId());
        return ApiResponse.success("指派成功", null);
    }

    @Transactional
    @Log(type = "REPAIR", value = "更新报修状态")
@PutMapping("/status/{id}")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody RepairOrderStatusRequest request) {
        ensureAdmin();
        RepairOrder order = repairOrderService.getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        order.setRemark(request.getRemark());
        order.setHandlerId(AuthContext.getUserId());
        try {
            repairOrderService.updateStatusWithLock(order, request.getRepairStatus());
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工单已被其他人操作，请刷新后重试");
        }
        return ApiResponse.success("状态更新成功", null);
    }

    @Transactional
    @Log(type = "REPAIR", value = "删除报修")
@DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ensureAdmin();
        repairOrderService.removeById(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(RepairOrderQueryRequest request) {
        ensureAdmin();

        LambdaQueryWrapper<RepairOrder> wrapper = buildWrapper(request)
                .orderByDesc(RepairOrder::getSubmitTime);
        List<RepairOrder> orders = repairOrderService.list(wrapper);

        // Batch preload to avoid N+1 queries in export
        Set<Long> categoryIds = orders.stream().map(RepairOrder::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = new java.util.HashSet<>();
        for (RepairOrder o : orders) {
            if (o.getUserId() != null) userIds.add(o.getUserId());
            if (o.getWorkerId() != null) userIds.add(o.getWorkerId());
        }
        Map<Long, RepairCategory> categoryMap = categoryIds.isEmpty() ? Map.of() :
                repairCategoryService.listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(RepairCategory::getId, c -> c));
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of() :
                sysUserService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<String> headers = Arrays.asList(
                "ID", "工单号", "标题", "内容", "报修分类", "维修状态",
                "提交人", "学号", "联系电话", "宿舍楼", "房间号",
                "维修人员", "维修人员电话", "提交时间", "受理时间",
                "指派时间", "接单时间", "完成时间", "确认时间", "取消时间", "备注"
        );

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        byte[] bytes = ExcelExportUtil.export("报修工单", headers, orders, order -> {
            SysUser student = userMap.get(order.getUserId());
            SysUser worker = order.getWorkerId() != null ? userMap.get(order.getWorkerId()) : null;
            RepairCategory category = categoryMap.get(order.getCategoryId());

            return Arrays.asList(
                    order.getId(),
                    order.getOrderNo(),
                    order.getTitle(),
                    order.getContent(),
                    category != null ? category.getCategoryName() : "",
                    com.example.dormitoryrepair.common.enums.RepairStatusEnum.statusLabel(order.getRepairStatus()),
                    student != null ? student.getRealName() : "",
                    student != null ? student.getStudentNo() : "",
                    student != null ? student.getPhone() : "",
                    student != null ? student.getDormitoryBuilding() : "",
                    student != null ? student.getRoomNo() : "",
                    worker != null ? worker.getRealName() : "",
                    worker != null ? worker.getPhone() : "",
                    order.getSubmitTime() != null ? order.getSubmitTime().format(dtf) : "",
                    order.getAcceptTime() != null ? order.getAcceptTime().format(dtf) : "",
                    order.getAssignTime() != null ? order.getAssignTime().format(dtf) : "",
                    order.getWorkerAcceptTime() != null ? order.getWorkerAcceptTime().format(dtf) : "",
                    order.getFinishTime() != null ? order.getFinishTime().format(dtf) : "",
                    order.getStudentConfirmTime() != null ? order.getStudentConfirmTime().format(dtf) : "",
                    order.getCancelTime() != null ? order.getCancelTime().format(dtf) : "",
                    order.getRemark() != null ? order.getRemark() : ""
            );
        });

        String filename = URLEncoder.encode("报修工单_" + LocalDateTime.now().format(dtf).replace(" ", "_") + ".xlsx",
                StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    // ==================== 维修人员端 ====================

    @GetMapping("/worker-page")
    public ApiResponse<Map<String, Object>> workerPage(RepairOrderQueryRequest request) {
        ensureRepairer();
        Long workerId = AuthContext.getUserId();
        LambdaQueryWrapper<RepairOrder> wrapper = buildWrapper(request)
                .eq(RepairOrder::getWorkerId, workerId)
                .orderByDesc(RepairOrder::getId)
                .orderByDesc(RepairOrder::getSubmitTime);
        IPage<RepairOrder> result = repairOrderService.page(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", buildOrderViews(result.getRecords()));
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return ApiResponse.success(data);
    }

    @Transactional
    @Log(type = "REPAIR", value = "维修人员接单")
@PutMapping("/worker-accept/{id}")
    public ApiResponse<Void> workerAccept(@PathVariable Long id) {
        ensureRepairer();
        Long workerId = AuthContext.getUserId();
        RepairOrder order = repairOrderService.getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!Objects.equals(order.getWorkerId(), workerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (!Objects.equals(order.getRepairStatus(), 2)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前状态不允许接单");
        }
        order.setRepairStatus(3);
        order.setWorkerAcceptTime(LocalDateTime.now());
        if (!repairOrderService.updateById(order)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工单已被其他人操作，请刷新后重试");
        }
        pushToStudent(order.getUserId(), "维修人员已接单", "维修人员已开始处理您的报修。", order.getId());
        return ApiResponse.success("接单成功", null);
    }

    @Transactional
    @Log(type = "REPAIR", value = "维修人员完成")
@PutMapping("/worker-complete/{id}")
    public ApiResponse<Void> workerComplete(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ensureRepairer();
        Long workerId = AuthContext.getUserId();
        RepairOrder order = repairOrderService.getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!Objects.equals(order.getWorkerId(), workerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (!Objects.equals(order.getRepairStatus(), 3)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前状态不允许标记完成");
        }
        order.setRepairStatus(4);
        order.setWorkerCompleteTime(LocalDateTime.now());
        // AI 验收预测
        order.setNeedsReview(0);
        if (completionReviewEnabled) {
            EvaluationResponse eval = aiService.evaluateCompletion(id);
            if (eval != null) {
                order.setRiskScore(eval.getRiskScore());
                order.setReviewType(eval.getDecision());
                order.setNeedsReview("REQUIRE_REVIEW".equals(eval.getDecision()) ? 1 : 0);
            }
        }
        String remark = body.get("remark");
        if (remark != null && !remark.isBlank()) {
            order.setRemark(remark);
        }
        if (!repairOrderService.updateById(order)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "工单已被其他人操作，请刷新后重试");
        }
        pushToStudent(order.getUserId(), "维修已完成", "您的报修已维修完成，请确认。", order.getId());
        return ApiResponse.success("维修完成标记成功", null);
    }

    // ==================== 公共 ====================

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        RepairOrder order = repairOrderService.getById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ensureSelfOrAdminOrRepairer(order);
        return ApiResponse.success(buildOrderView(order));
    }

    @GetMapping("/categories")
    public ApiResponse<List<RepairCategory>> categories() {
        List<RepairCategory> categories = repairCategoryService.list(new LambdaQueryWrapper<RepairCategory>()
                .eq(RepairCategory::getStatus, 1)
                .orderByAsc(RepairCategory::getSortNum));
        return ApiResponse.success(categories);
    }

    // ==================== 权限检查 ====================

    private void ensureAdmin() {
        if (!"ADMIN".equals(AuthContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private void ensureRepairer() {
        if (!"REPAIRER".equals(AuthContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private void ensureSelfOrAdmin(Long ownerUserId) {
        Long currentUserId = AuthContext.getUserId();
        if (!Objects.equals(ownerUserId, currentUserId) && !"ADMIN".equals(AuthContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private void ensureSelfOrAdminOrRepairer(RepairOrder order) {
        Long currentUserId = AuthContext.getUserId();
        String role = AuthContext.getRole();
        if (Objects.equals(order.getUserId(), currentUserId)) return;
        if ("ADMIN".equals(role)) return;
        if ("REPAIRER".equals(role) && Objects.equals(order.getWorkerId(), currentUserId)) return;
        throw new BusinessException(ResultCode.FORBIDDEN);
    }

    // ==================== 工具方法 ====================

    private void pushToStudent(Long userId, String title, String content, Long orderId) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(), "/queue/notification",
                new NotificationMessage("STATUS_UPDATE", title, content, orderId, System.currentTimeMillis()));
        saveNotification(userId, "STATUS_UPDATE", title, content, orderId);
    }

    private void pushToWorker(Long workerId, String title, String content, Long orderId) {
        messagingTemplate.convertAndSendToUser(
                workerId.toString(), "/queue/notification",
                new NotificationMessage("STATUS_UPDATE", title, content, orderId, System.currentTimeMillis()));
        saveNotification(workerId, "STATUS_UPDATE", title, content, orderId);
    }

    private void saveNotification(Long userId, String type, String title, String content, Long orderId) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setOrderId(orderId);
            notification.setIsRead(0);
            notification.setCreateTime(LocalDateTime.now());
            notificationService.save(notification);
        } catch (Exception e) {
            log.error("Failed to save notification for user {} (order {}): {}", userId, orderId, e.getMessage());
        }
    }

    private LambdaQueryWrapper<RepairOrder> buildWrapper(RepairOrderQueryRequest request) {
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();
        if (request.getCategoryId() != null) {
            wrapper.eq(RepairOrder::getCategoryId, request.getCategoryId());
        }
        if (request.getRepairStatus() != null) {
            wrapper.eq(RepairOrder::getRepairStatus, request.getRepairStatus());
        }
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(RepairOrder::getTitle, request.getKeyword())
                    .or().like(RepairOrder::getContent, request.getKeyword())
                    .or().like(RepairOrder::getOrderNo, request.getKeyword()));
        }
        if (request.getSubmitTime() != null && !request.getSubmitTime().isBlank()) {
            LocalDateTime[] range = DateTimeRangeParser.parse(request.getSubmitTime());
            wrapper.between(RepairOrder::getSubmitTime, range[0], range[1]);
        }
        return wrapper;
    }

    private List<Map<String, Object>> buildOrderViews(List<RepairOrder> orders) {
        if (orders.isEmpty()) return List.of();

        // Batch preload all related data to avoid N+1 queries
        Set<Long> categoryIds = orders.stream().map(RepairOrder::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = new java.util.HashSet<>();
        for (RepairOrder o : orders) {
            if (o.getUserId() != null) userIds.add(o.getUserId());
            if (o.getHandlerId() != null) userIds.add(o.getHandlerId());
            if (o.getWorkerId() != null) userIds.add(o.getWorkerId());
        }

        Map<Long, RepairCategory> categoryMap = categoryIds.isEmpty() ? Map.of() :
                repairCategoryService.listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(RepairCategory::getId, c -> c));
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of() :
                sysUserService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<Map<String, Object>> views = new ArrayList<>(orders.size());
        for (RepairOrder order : orders) {
            views.add(buildOrderView(order, categoryMap, userMap));
        }
        return views;
    }

    private Map<String, Object> buildOrderView(RepairOrder order) {
        return buildOrderView(order, null, null);
    }

    private Map<String, Object> buildOrderView(RepairOrder order,
                                                Map<Long, RepairCategory> categoryMap,
                                                Map<Long, SysUser> userMap) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", order.getId());
        view.put("orderNo", order.getOrderNo());
        view.put("userId", order.getUserId());
        view.put("categoryId", order.getCategoryId());
        view.put("handlerId", order.getHandlerId());
        view.put("workerId", order.getWorkerId());
        view.put("title", order.getTitle());
        view.put("content", order.getContent());
        view.put("imageUrl", order.getImageUrl());
        view.put("repairStatus", order.getRepairStatus());
        view.put("submitTime", order.getSubmitTime());
        view.put("assignTime", order.getAssignTime());
        view.put("acceptTime", order.getAcceptTime());
        view.put("workerAcceptTime", order.getWorkerAcceptTime());
        view.put("handleTime", order.getHandleTime());
        view.put("workerCompleteTime", order.getWorkerCompleteTime());
        view.put("finishTime", order.getFinishTime());
        view.put("studentConfirmTime", order.getStudentConfirmTime());
        view.put("cancelTime", order.getCancelTime());
        view.put("remark", order.getRemark());
        view.put("createTime", order.getCreateTime());
        view.put("updateTime", order.getUpdateTime());

        RepairCategory category = categoryMap != null ? categoryMap.get(order.getCategoryId())
                : repairCategoryService.getById(order.getCategoryId());
        if (category != null) {
            view.put("categoryName", category.getCategoryName());
        }

        SysUser user = userMap != null ? userMap.get(order.getUserId())
                : sysUserService.getById(order.getUserId());
        if (user != null) {
            view.put("username", user.getUsername());
            view.put("realName", user.getRealName());
            view.put("studentNo", user.getStudentNo());
            view.put("phone", user.getPhone());
            view.put("dormitoryBuilding", user.getDormitoryBuilding());
            view.put("roomNo", user.getRoomNo());
            view.put("avatar", user.getAvatar());
        }

        SysUser handler = order.getHandlerId() != null
                ? (userMap != null ? userMap.get(order.getHandlerId()) : sysUserService.getById(order.getHandlerId()))
                : null;
        if (handler != null) {
            view.put("handlerName", handler.getRealName());
        }

        SysUser worker = order.getWorkerId() != null
                ? (userMap != null ? userMap.get(order.getWorkerId()) : sysUserService.getById(order.getWorkerId()))
                : null;
        if (worker != null) {
            view.put("workerName", worker.getRealName());
            view.put("workerPhone", worker.getPhone());
        }

        return view;
    }

}
