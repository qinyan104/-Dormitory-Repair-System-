package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.dto.log.OperationLogQueryRequest;
import com.example.dormitoryrepair.entity.OperationLog;
import com.example.dormitoryrepair.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(OperationLogQueryRequest request) {
        ensureAdmin();

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(request.getOperationType() != null && !request.getOperationType().isBlank(),
                        OperationLog::getOperationType, request.getOperationType())
                .eq(request.getStatus() != null, OperationLog::getStatus, request.getStatus())
                .and(request.getKeyword() != null && !request.getKeyword().isBlank(),
                        w -> w.like(OperationLog::getDescription, request.getKeyword())
                                .or().like(OperationLog::getMethodName, request.getKeyword())
                                .or().like(OperationLog::getRequestParams, request.getKeyword()))
                .orderByDesc(OperationLog::getCreateTime);

        IPage<OperationLog> result = operationLogService.page(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return ApiResponse.success(data);
    }

    private void ensureAdmin() {
        if (!Objects.equals("ADMIN", AuthContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
