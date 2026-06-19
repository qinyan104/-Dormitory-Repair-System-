package com.example.dormitoryrepair.controller;

import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.dto.feedback.RepairFeedbackCreateRequest;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.service.RepairFeedbackService;
import com.example.dormitoryrepair.service.RepairOrderService;
import com.example.dormitoryrepair.service.SysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepairFeedbackControllerTest {

    @Mock
    private RepairFeedbackService repairFeedbackService;

    @Mock
    private RepairOrderService repairOrderService;

    @Mock
    private SysUserService sysUserService;

    private RepairFeedbackController controller;

    @BeforeEach
    void setUp() {
        controller = new RepairFeedbackController(repairFeedbackService, repairOrderService, sysUserService);
        AuthContext.setUserId(2L);
        AuthContext.setRole("STUDENT");
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void createSavesFeedbackForCompletedOwnOrder() {
        RepairOrder order = new RepairOrder();
        order.setId(8L);
        order.setUserId(2L);
        order.setRepairStatus(5);
        when(repairOrderService.getById(8L)).thenReturn(order);
        when(repairFeedbackService.count(any())).thenReturn(0L);

        RepairFeedbackCreateRequest request = new RepairFeedbackCreateRequest();
        request.setRepairOrderId(8L);
        request.setScore(5);
        request.setContent("处理很及时");

        controller.create(request);

        ArgumentCaptor<com.example.dormitoryrepair.entity.RepairFeedback> captor =
                ArgumentCaptor.forClass(com.example.dormitoryrepair.entity.RepairFeedback.class);
        verify(repairFeedbackService).save(captor.capture());
        assertEquals(2L, captor.getValue().getUserId());
        assertEquals(8L, captor.getValue().getRepairOrderId());
    }

    @Test
    void createRejectsDuplicateFeedback() {
        RepairOrder order = new RepairOrder();
        order.setId(8L);
        order.setUserId(2L);
        order.setRepairStatus(5);
        when(repairOrderService.getById(8L)).thenReturn(order);
        when(repairFeedbackService.count(any())).thenReturn(1L);

        RepairFeedbackCreateRequest request = new RepairFeedbackCreateRequest();
        request.setRepairOrderId(8L);
        request.setScore(5);

        assertThrows(BusinessException.class, () -> controller.create(request));
    }

    @Test
    void detailRejectsNonOwnerStudent() {
        RepairOrder order = new RepairOrder();
        order.setId(8L);
        order.setUserId(9L);
        when(repairOrderService.getById(8L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> controller.detail(8L));
    }
}
