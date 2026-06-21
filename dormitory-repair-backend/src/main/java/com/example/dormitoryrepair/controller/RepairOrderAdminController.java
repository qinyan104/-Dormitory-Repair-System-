package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dormitoryrepair.common.annotation.Log;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.common.util.ExcelExportUtil;
import com.example.dormitoryrepair.dto.ai.RecommendResponse;
import com.example.dormitoryrepair.dto.repair.RepairOrderQueryRequest;
import com.example.dormitoryrepair.dto.repair.RepairOrderStatusRequest;
import com.example.dormitoryrepair.entity.RepairCategory;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.entity.SysUser;
import com.example.dormitoryrepair.service.AiService;
import com.example.dormitoryrepair.service.RepairCategoryService;
import com.example.dormitoryrepair.service.RepairOrderService;
import com.example.dormitoryrepair.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/repair-order")
@RequiredArgsConstructor
public class RepairOrderAdminController {

    private final RepairOrderService repairOrderService;
    private final RepairCategoryService repairCategoryService;
    private final SysUserService sysUserService;
    private final AiService aiService;
    private final RepairOrderViewHelper helper;

    @Value("${app.ai.completion-review.enabled:true}")
    private boolean completionReviewEnabled;

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(RepairOrderQueryRequest request) {
        helper.ensureAdmin();
        LambdaQueryWrapper<RepairOrder> wrapper = helper.buildWrapper(request)
                .orderByDesc(RepairOrder::getId)
                .orderByDesc(RepairOrder::getSubmitTime);
        IPage<RepairOrder> result = repairOrderService.page(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", helper.buildOrderViews(result.getRecords()));
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return ApiResponse.success(data);
    }

    @Log(type = "REPAIR", value = "管理员受理报修")
    @PutMapping("/accept/{id}")
    @Transactional
    public ApiResponse<Map<String, Object>> accept(@PathVariable Long id) {
        helper.ensureAdmin();
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
            helper.pushToWorker(ai.getAssignedWorkerId(), "新工单指派",
                    "您有一份新的维修工单：" + order.getTitle(), order.getId());
            helper.pushToStudent(order.getUserId(), "工单已派单",
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

    @Log(type = "REPAIR", value = "管理员指派维修")
    @PutMapping("/assign/{id}")
    @Transactional
    public ApiResponse<Void> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        helper.ensureAdmin();
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
        helper.pushToWorker(workerId, "新工单指派", "您有一份新的维修工单：" + order.getTitle(), order.getId());
        helper.pushToStudent(order.getUserId(), "工单已派单", "您的报修已被受理，维修人员正在赶来。", order.getId());
        return ApiResponse.success("指派成功", null);
    }

    @Log(type = "REPAIR", value = "更新报修状态")
    @PutMapping("/status/{id}")
    @Transactional
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody RepairOrderStatusRequest request) {
        helper.ensureAdmin();
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

    @Log(type = "REPAIR", value = "删除报修")
    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable Long id) {
        helper.ensureAdmin();
        repairOrderService.removeById(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(RepairOrderQueryRequest request) {
        helper.ensureAdmin();

        LambdaQueryWrapper<RepairOrder> wrapper = helper.buildWrapper(request)
                .orderByDesc(RepairOrder::getSubmitTime);
        List<RepairOrder> orders = repairOrderService.list(wrapper);

        // Batch preload to avoid N+1 queries in export
        Set<Long> categoryIds = orders.stream().map(RepairOrder::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = new HashSet<>();
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

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<String> headers = List.of("工单编号", "标题", "报修人", "宿舍", "分类", "状态",
                "提交时间", "受理时间", "派单时间", "接单时间", "完成时间", "确认时间", "取消时间", "备注");

        byte[] bytes = ExcelExportUtil.export("报修工单", headers, orders, order -> {
            SysUser user = userMap.get(order.getUserId());
            RepairCategory category = categoryMap.get(order.getCategoryId());
            List<Object> values = new ArrayList<>();
            values.add(order.getOrderNo() != null ? order.getOrderNo() : "");
            values.add(order.getTitle() != null ? order.getTitle() : "");
            values.add(user != null ? (user.getRealName() != null ? user.getRealName() : user.getUsername()) : "");
            values.add(user != null ? (user.getDormitoryBuilding() != null ? user.getDormitoryBuilding() : "") + " " + (user.getRoomNo() != null ? user.getRoomNo() : "") : "");
            values.add(category != null ? category.getCategoryName() : "");
            values.add(helper.statusLabel(order.getRepairStatus()));
            values.add(order.getSubmitTime() != null ? order.getSubmitTime().format(dtf) : "");
            values.add(order.getAcceptTime() != null ? order.getAcceptTime().format(dtf) : "");
            values.add(order.getAssignTime() != null ? order.getAssignTime().format(dtf) : "");
            values.add(order.getWorkerAcceptTime() != null ? order.getWorkerAcceptTime().format(dtf) : "");
            values.add(order.getFinishTime() != null ? order.getFinishTime().format(dtf) : "");
            values.add(order.getStudentConfirmTime() != null ? order.getStudentConfirmTime().format(dtf) : "");
            values.add(order.getCancelTime() != null ? order.getCancelTime().format(dtf) : "");
            values.add(order.getRemark() != null ? order.getRemark() : "");
            return values;
        });

        String filename = URLEncoder.encode("报修工单_" + LocalDateTime.now().format(dtf).replace(" ", "_") + ".xlsx",
                StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
