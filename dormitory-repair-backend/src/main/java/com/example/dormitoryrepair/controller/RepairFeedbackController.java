package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.dto.feedback.RepairFeedbackCreateRequest;
import com.example.dormitoryrepair.dto.feedback.RepairFeedbackQueryRequest;
import com.example.dormitoryrepair.entity.RepairFeedback;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.entity.SysUser;
import com.example.dormitoryrepair.service.RepairFeedbackService;
import com.example.dormitoryrepair.service.RepairOrderService;
import com.example.dormitoryrepair.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/repair-feedback")
@RequiredArgsConstructor
public class RepairFeedbackController {

    private final RepairFeedbackService repairFeedbackService;
    private final RepairOrderService repairOrderService;
    private final SysUserService sysUserService;

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(RepairFeedbackQueryRequest request) {
        ensureAdmin();
        LambdaQueryWrapper<RepairFeedback> wrapper = new LambdaQueryWrapper<RepairFeedback>()
                .orderByDesc(RepairFeedback::getCreateTime);

        IPage<RepairFeedback> result = repairFeedbackService.page(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);

        // Batch preload orders and users to avoid N+1 queries
        Set<Long> orderIds = result.getRecords().stream()
                .map(RepairFeedback::getRepairOrderId).collect(Collectors.toSet());
        Set<Long> userIds = result.getRecords().stream()
                .map(RepairFeedback::getUserId).collect(Collectors.toSet());

        Map<Long, RepairOrder> orderMap = orderIds.isEmpty() ? Map.of() :
                repairOrderService.listByIds(orderIds).stream()
                        .collect(Collectors.toMap(RepairOrder::getId, o -> o));
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of() :
                sysUserService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));

        List<Map<String, Object>> enriched = result.getRecords().stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("repairOrderId", f.getRepairOrderId());
            map.put("userId", f.getUserId());
            map.put("score", f.getScore());
            map.put("content", f.getContent());
            map.put("createTime", f.getCreateTime());

            RepairOrder order = orderMap.get(f.getRepairOrderId());
            if (order != null) {
                map.put("orderTitle", order.getTitle());
                map.put("orderNo", order.getOrderNo());
            }

            SysUser user = userMap.get(f.getUserId());
            if (user != null) {
                map.put("realName", user.getRealName());
                map.put("username", user.getUsername());
            }

            return map;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("records", enriched);
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return ApiResponse.success(data);
    }

    @PostMapping
    public ApiResponse<RepairFeedback> create(@Valid @RequestBody RepairFeedbackCreateRequest request) {
        RepairOrder order = repairOrderService.getById(request.getRepairOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ensureOwner(order.getUserId());
        if (!Objects.equals(order.getRepairStatus(), 5)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅已完成的报修单可以评价");
        }
        long exists = repairFeedbackService.count(new LambdaQueryWrapper<RepairFeedback>()
                .eq(RepairFeedback::getRepairOrderId, request.getRepairOrderId()));
        if (exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该报修单已评价");
        }

        RepairFeedback feedback = new RepairFeedback();
        feedback.setRepairOrderId(request.getRepairOrderId());
        feedback.setUserId(AuthContext.getUserId());
        feedback.setScore(request.getScore());
        feedback.setContent(request.getContent());
        repairFeedbackService.save(feedback);
        return ApiResponse.success("评价成功", feedback);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<RepairFeedback> detail(@PathVariable Long orderId) {
        RepairOrder order = repairOrderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ensureOwnerOrAdminOrRepairer(order);
        RepairFeedback feedback = repairFeedbackService.getOne(new LambdaQueryWrapper<RepairFeedback>()
                .eq(RepairFeedback::getRepairOrderId, orderId)
                .last("limit 1"));
        return ApiResponse.success(feedback);
    }

    private void ensureOwner(Long ownerUserId) {
        if (!Objects.equals(ownerUserId, AuthContext.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private void ensureOwnerOrAdminOrRepairer(RepairOrder order) {
        Long currentUserId = AuthContext.getUserId();
        String role = AuthContext.getRole();
        if (Objects.equals(order.getUserId(), currentUserId)) return;
        if (Objects.equals("ADMIN", role)) return;
        if (Objects.equals("REPAIRER", role) && Objects.equals(order.getWorkerId(), currentUserId)) return;
        throw new BusinessException(ResultCode.FORBIDDEN);
    }

    private void ensureAdmin() {
        if (!Objects.equals("ADMIN", AuthContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
