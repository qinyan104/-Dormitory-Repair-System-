package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.entity.RepairCategory;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.mapper.RepairOrderMapper;
import com.example.dormitoryrepair.service.RepairCategoryService;
import com.example.dormitoryrepair.service.RepairOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final RepairOrderService repairOrderService;
    private final RepairCategoryService repairCategoryService;
    private final RepairOrderMapper repairOrderMapper;

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        ensureAdmin();
        Map<String, Object> result = new HashMap<>();
        result.put("total", repairOrderService.count());
        result.put("pending", repairOrderService.count(statusWrapper(1)));
        result.put("assigned", repairOrderService.count(statusWrapper(2)));
        result.put("repairing", repairOrderService.count(statusWrapper(3)));
        result.put("waitingConfirm", repairOrderService.count(statusWrapper(4)));
        result.put("completed", repairOrderService.count(statusWrapper(5)));
        result.put("cancelled", repairOrderService.count(statusWrapper(6)));
        result.put("today", repairOrderService.count(new LambdaQueryWrapper<RepairOrder>()
                .between(RepairOrder::getSubmitTime,
                        LocalDate.now().atStartOfDay(),
                        LocalDate.now().atTime(23, 59, 59))));
        return ApiResponse.success(result);
    }

    @GetMapping("/category")
    public ApiResponse<List<Map<String, Object>>> category() {
        ensureAdmin();
        List<RepairCategory> categories = repairCategoryService.list(new LambdaQueryWrapper<RepairCategory>()
                .orderByAsc(RepairCategory::getSortNum)
                .orderByAsc(RepairCategory::getId));
        List<Map<String, Object>> rows = repairOrderMapper.countGroupByCategory();
        Map<Long, Long> counts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            counts.put(((Number) row.get("categoryId")).longValue(), ((Number) row.get("count")).longValue());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (RepairCategory category : categories) {
            Map<String, Object> item = new HashMap<>();
            item.put("categoryId", category.getId());
            item.put("categoryName", category.getCategoryName());
            item.put("count", counts.getOrDefault(category.getId(), 0L));
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/status")
    public ApiResponse<List<Map<String, Object>>> status() {
        ensureAdmin();
        List<Map<String, Object>> rows = repairOrderMapper.countGroupByStatus();
        Map<Integer, Long> counts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            counts.put(((Number) row.get("status")).intValue(), ((Number) row.get("count")).longValue());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int status = 1; status <= 6; status++) {
            long count = counts.getOrDefault(status, 0L);
            if (count == 0 && !counts.containsKey(status)) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("status", status);
            item.put("label", statusLabel(status));
            item.put("count", count);
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    private LambdaQueryWrapper<RepairOrder> statusWrapper(int status) {
        return new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getRepairStatus, status);
    }

    private String statusLabel(int status) {
        return switch (status) {
            case 1 -> "待受理";
            case 2 -> "已派单";
            case 3 -> "维修中";
            case 4 -> "待确认";
            case 5 -> "已完成";
            case 6 -> "已取消";
            default -> "未知";
        };
    }

    private void ensureAdmin() {
        if (!Objects.equals("ADMIN", AuthContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
