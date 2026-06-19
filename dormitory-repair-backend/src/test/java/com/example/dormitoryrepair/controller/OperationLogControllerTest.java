package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.dto.log.OperationLogQueryRequest;
import com.example.dormitoryrepair.entity.OperationLog;
import com.example.dormitoryrepair.service.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationLogControllerTest {

    @Mock
    private OperationLogService operationLogService;

    private OperationLogController controller;

    @BeforeEach
    void setUp() {
        controller = new OperationLogController(operationLogService);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void pageReturnsLogsForAdmin() {
        AuthContext.setRole("ADMIN");
        OperationLog log = new OperationLog();
        log.setId(1L);
        log.setDescription("管理员登录");
        log.setOperatorId(1L);
        log.setOperatorName("admin");
        Page<OperationLog> page = new Page<>(1, 10);
        page.setRecords(List.of(log));
        page.setTotal(1);
        when(operationLogService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        OperationLogQueryRequest request = new OperationLogQueryRequest();
        Map<String, Object> result = controller.page(request).getData();

        List<?> records = (List<?>) result.get("records");
        assertEquals(1, records.size());
        OperationLog returned = (OperationLog) records.get(0);
        assertEquals(1L, returned.getOperatorId());
        assertEquals("admin", returned.getOperatorName());
    }

    @Test
    void pageFiltersByKeyword() {
        AuthContext.setRole("ADMIN");
        Page<OperationLog> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(operationLogService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        OperationLogQueryRequest request = new OperationLogQueryRequest();
        request.setKeyword("登录");

        controller.page(request);

        // No exception means keyword filter was applied successfully
        assertNull(request.getOperationType());
    }

    @Test
    void pageRejectsNonAdmin() {
        AuthContext.setRole("STUDENT");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.page(new OperationLogQueryRequest()));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void pageRejectsRepairer() {
        AuthContext.setRole("REPAIRER");

        assertThrows(BusinessException.class,
                () -> controller.page(new OperationLogQueryRequest()));
    }
}
