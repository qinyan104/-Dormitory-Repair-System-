package com.example.dormitoryrepair.controller;

import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.dto.ai.RecommendResponse;
import com.example.dormitoryrepair.dto.repair.RepairOrderCreateRequest;
import com.example.dormitoryrepair.dto.repair.RepairOrderQueryRequest;
import com.example.dormitoryrepair.dto.repair.RepairOrderStatusRequest;
import com.example.dormitoryrepair.entity.Notification;
import com.example.dormitoryrepair.entity.RepairCategory;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.entity.SysUser;
import com.example.dormitoryrepair.service.AiService;
import com.example.dormitoryrepair.service.RepairCategoryService;
import com.example.dormitoryrepair.service.RepairOrderService;
import com.example.dormitoryrepair.service.SysUserService;
import com.example.dormitoryrepair.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairOrderControllerTest {

    @Mock
    private RepairOrderService repairOrderService;

    @Mock
    private RepairCategoryService repairCategoryService;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AiService aiService;

    private RepairOrderController controller;

    @BeforeEach
    void setUp() {
        controller = new RepairOrderController(repairOrderService, repairCategoryService, sysUserService, messagingTemplate, notificationService, aiService);
        AuthContext.setUserId(1L);
        AuthContext.setRole("STUDENT");
        // Default mocks for batch loading (used by buildOrderViews)
        lenient().when(repairCategoryService.listByIds(any())).thenReturn(List.of());
        lenient().when(sysUserService.listByIds(any())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    private RepairOrder createTestOrder(Long id, Integer status, Long userId, Long workerId) {
        RepairOrder order = new RepairOrder();
        order.setId(id);
        order.setOrderNo("RO" + String.format("%016d", id));
        order.setRepairStatus(status);
        order.setUserId(userId);
        order.setWorkerId(workerId);
        order.setCategoryId(1L);
        order.setTitle("测试工单" + id);
        return order;
    }

    // ==================== create ====================

    @Test
    void createSuccess() {
        RepairCategory category = new RepairCategory();
        category.setId(1L);
        category.setStatus(1);
        when(repairCategoryService.getById(1L)).thenReturn(category);
        when(repairOrderService.save(any(RepairOrder.class))).thenReturn(true);

        RepairOrderCreateRequest request = new RepairOrderCreateRequest();
        request.setCategoryId(1L);
        request.setTitle("水龙头漏水");
        request.setContent("水龙头关不严");
        request.setUrgency("紧急");

        var result = controller.create(request);

        assertNotNull(result.getData().getOrderNo());
        assertEquals(1, result.getData().getRepairStatus());
        assertEquals("紧急", result.getData().getUrgency());
        verify(messagingTemplate).convertAndSend(eq("/topic/admin/new-order"), any(Object.class));
    }

    @Test
    void createPersistsAiFields() {
        RepairCategory category = new RepairCategory();
        category.setId(1L);
        category.setStatus(1);
        when(repairCategoryService.getById(1L)).thenReturn(category);
        when(repairOrderService.save(any(RepairOrder.class))).thenReturn(true);

        RepairOrderCreateRequest request = new RepairOrderCreateRequest();
        request.setCategoryId(1L);
        request.setTitle("公共区域漏水");
        request.setContent("走廊持续积水");
        request.setAiPriorityScore(9);
        request.setAiImpactScope(8);

        controller.create(request);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderService).save(captor.capture());
        assertEquals(9, captor.getValue().getPriorityScore());
        assertEquals(8, captor.getValue().getImpactScope());
    }

    @Test
    void createWithoutUrgencyDefaultsToNull() {
        RepairCategory category = new RepairCategory();
        category.setId(1L);
        category.setStatus(1);
        when(repairCategoryService.getById(1L)).thenReturn(category);
        when(repairOrderService.save(any(RepairOrder.class))).thenReturn(true);

        RepairOrderCreateRequest request = new RepairOrderCreateRequest();
        request.setCategoryId(1L);
        request.setTitle("门锁坏了");
        request.setContent("钥匙拧不动");

        var result = controller.create(request);

        assertNull(result.getData().getUrgency());
    }

    @Test
    void createRejectsDisabledCategory() {
        RepairCategory category = new RepairCategory();
        category.setId(1L);
        category.setStatus(0);
        when(repairCategoryService.getById(1L)).thenReturn(category);

        RepairOrderCreateRequest request = new RepairOrderCreateRequest();
        request.setCategoryId(1L);

        assertThrows(BusinessException.class, () -> controller.create(request));
    }

    @Test
    void createRejectsNonExistentCategory() {
        when(repairCategoryService.getById(999L)).thenReturn(null);

        RepairOrderCreateRequest request = new RepairOrderCreateRequest();
        request.setCategoryId(999L);

        assertThrows(BusinessException.class, () -> controller.create(request));
    }

    // ==================== myPage ====================

    @Test
    void myPageReturnsCurrentUserOrders() {
        Page<RepairOrder> page = new Page<>(1, 10);
        page.setRecords(List.of(createTestOrder(1L, 1, 1L, null)));
        page.setTotal(1);
        when(repairOrderService.page(any(Page.class), any(Wrapper.class))).thenReturn(page);

        RepairOrderQueryRequest request = new RepairOrderQueryRequest();
        var result = controller.myPage(request);

        assertNotNull(result.getData().get("records"));
    }

    // ==================== cancel ====================

    @Test
    void cancelOwnPendingOrder() {
        RepairOrder order = createTestOrder(1L, 1, 1L, null);
        when(repairOrderService.getById(1L)).thenReturn(order);
        when(repairOrderService.updateById(any(RepairOrder.class))).thenReturn(true);

        controller.cancel(1L);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderService).updateById(captor.capture());
        assertEquals(6, captor.getValue().getRepairStatus());
    }

    @Test
    void cancelRejectsAlreadyCompleted() {
        RepairOrder order = createTestOrder(1L, 5, 1L, null);
        when(repairOrderService.getById(1L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> controller.cancel(1L));
    }

    @Test
    void cancelRejectsNonOwner() {
        AuthContext.setUserId(2L);
        RepairOrder order = createTestOrder(1L, 1, 3L, null);
        when(repairOrderService.getById(1L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> controller.cancel(1L));
    }

    @Test
    void cancelThrowsWhenOrderNotFound() {
        when(repairOrderService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.cancel(999L));
    }

    // ==================== studentConfirm ====================

    @Test
    void studentConfirmSuccess() {
        RepairOrder order = createTestOrder(1L, 4, 1L, 2L);
        when(repairOrderService.getById(1L)).thenReturn(order);
        when(repairOrderService.updateById(any(RepairOrder.class))).thenReturn(true);

        controller.studentConfirm(1L);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderService).updateById(captor.capture());
        assertEquals(5, captor.getValue().getRepairStatus());
        verify(messagingTemplate).convertAndSendToUser(eq("2"), eq("/queue/notification"), any(Object.class));
    }

    @Test
    void studentConfirmRejectsWrongStatus() {
        RepairOrder order = createTestOrder(1L, 1, 1L, null);
        when(repairOrderService.getById(1L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> controller.studentConfirm(1L));
    }

    // ==================== page (admin) ====================

    @Test
    void adminPageSuccess() {
        AuthContext.setRole("ADMIN");
        Page<RepairOrder> page = new Page<>(1, 10);
        page.setRecords(List.of(createTestOrder(1L, 1, 2L, null)));
        page.setTotal(1);
        when(repairOrderService.page(any(Page.class), any(Wrapper.class))).thenReturn(page);

        RepairOrderQueryRequest request = new RepairOrderQueryRequest();
        var result = controller.page(request);

        assertNotNull(result.getData().get("records"));
    }

    @Test
    void adminPageRejectsNonAdmin() {
        assertThrows(BusinessException.class, () -> controller.page(new RepairOrderQueryRequest()));
    }

    // ==================== accept (admin) ====================

    @Test
    void acceptSuccess() {
        AuthContext.setRole("ADMIN");
        RepairOrder order = createTestOrder(1L, 1, 2L, null);
        when(repairOrderService.getById(1L)).thenReturn(order);
        when(repairOrderService.updateById(any(RepairOrder.class))).thenReturn(true);

        controller.accept(1L);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderService).updateById(captor.capture());
        assertEquals(1, captor.getValue().getRepairStatus());
        assertNotNull(captor.getValue().getAcceptTime());
        assertEquals(1L, captor.getValue().getHandlerId());
    }

    @Test
    void acceptAutoAssignsWhenAiReturnsWinner() {
        AuthContext.setRole("ADMIN");
        RepairOrder order = createTestOrder(1L, 1, 2L, null);
        SysUser worker = new SysUser();
        worker.setId(3L);
        worker.setRole("REPAIRER");
        worker.setRealName("张师傅");
        RecommendResponse ai = new RecommendResponse();
        ai.setAutoAssigned(true);
        ai.setAssignedWorkerId(3L);

        when(repairOrderService.getById(1L)).thenReturn(order);
        when(repairOrderService.updateById(any(RepairOrder.class))).thenReturn(true);
        when(aiService.recommendWorker(1L)).thenReturn(ai);
        when(sysUserService.getById(3L)).thenReturn(worker);

        var result = controller.accept(1L);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderService).updateById(captor.capture());
        assertEquals(2, captor.getValue().getRepairStatus());
        assertEquals(3L, captor.getValue().getWorkerId());
        assertEquals(true, result.getData().get("autoAssigned"));
    }

    @Test
    void acceptRejectsNonPendingOrder() {
        AuthContext.setRole("ADMIN");
        RepairOrder order = createTestOrder(1L, 3, 2L, null);
        when(repairOrderService.getById(1L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> controller.accept(1L));
    }

    // ==================== assign (admin) ====================

    @Test
    void assignSuccess() {
        AuthContext.setRole("ADMIN");
        RepairOrder order = createTestOrder(1L, 1, 2L, null);
        SysUser worker = new SysUser();
        worker.setId(3L);
        worker.setRole("REPAIRER");
        when(repairOrderService.getById(1L)).thenReturn(order);
        when(sysUserService.getById(3L)).thenReturn(worker);
        when(repairOrderService.updateById(any(RepairOrder.class))).thenReturn(true);

        controller.assign(1L, Map.of("workerId", 3L));

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderService).updateById(captor.capture());
        assertEquals(2, captor.getValue().getRepairStatus());
        assertEquals(3L, captor.getValue().getWorkerId());
        verify(notificationService, atLeastOnce()).save(any(Notification.class));
    }

    @Test
    void assignAllowsAcceptedOrderWithoutWorker() {
        AuthContext.setRole("ADMIN");
        RepairOrder order = createTestOrder(1L, 2, 2L, null);
        SysUser worker = new SysUser();
        worker.setId(3L);
        worker.setRole("REPAIRER");
        when(repairOrderService.getById(1L)).thenReturn(order);
        when(sysUserService.getById(3L)).thenReturn(worker);
        when(repairOrderService.updateById(any(RepairOrder.class))).thenReturn(true);

        controller.assign(1L, Map.of("workerId", 3L));

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderService).updateById(captor.capture());
        assertEquals(2, captor.getValue().getRepairStatus());
        assertEquals(3L, captor.getValue().getWorkerId());
    }

    @Test
    void assignRejectsMissingWorkerId() {
        AuthContext.setRole("ADMIN");

        assertThrows(BusinessException.class, () -> controller.assign(1L, Map.of()));
    }

    @Test
    void assignRejectsInvalidWorker() {
        AuthContext.setRole("ADMIN");
        RepairOrder order = createTestOrder(1L, 1, 2L, null);
        SysUser notWorker = new SysUser();
        notWorker.setId(3L);
        notWorker.setRole("STUDENT");
        when(repairOrderService.getById(1L)).thenReturn(order);
        when(sysUserService.getById(3L)).thenReturn(notWorker);

        assertThrows(BusinessException.class, () -> controller.assign(1L, Map.of("workerId", 3L)));
    }

    // ==================== updateStatus (admin) ====================

    @Test
    void updateStatusToInProgress() {
        AuthContext.setRole("ADMIN");
        RepairOrder order = createTestOrder(1L, 2, 2L, null);
        when(repairOrderService.getById(1L)).thenReturn(order);
        when(repairOrderService.updateStatusWithLock(any(RepairOrder.class), any(Integer.class))).thenReturn(true);

        RepairOrderStatusRequest request = new RepairOrderStatusRequest();
        request.setRepairStatus(3);
        request.setRemark("正在维修中");

        controller.updateStatus(1L, request);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderService).updateStatusWithLock(captor.capture(), eq(3));
        assertEquals("正在维修中", captor.getValue().getRemark());
    }

    @Test
    void updateStatusCanFinishOrder() {
        AuthContext.setRole("ADMIN");
        RepairOrder order = createTestOrder(1L, 4, 2L, null);
        when(repairOrderService.getById(1L)).thenReturn(order);
        when(repairOrderService.updateStatusWithLock(any(RepairOrder.class), any(Integer.class))).thenReturn(true);

        RepairOrderStatusRequest request = new RepairOrderStatusRequest();
        request.setRepairStatus(5);

        controller.updateStatus(1L, request);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderService).updateStatusWithLock(captor.capture(), eq(5));
    }

    // ==================== worker operations ====================

    @Test
    void workerPageSuccess() {
        AuthContext.setRole("REPAIRER");
        Page<RepairOrder> page = new Page<>(1, 10);
        page.setRecords(List.of(createTestOrder(1L, 2, 2L, 1L)));
        page.setTotal(1);
        when(repairOrderService.page(any(Page.class), any(Wrapper.class))).thenReturn(page);

        var result = controller.workerPage(new RepairOrderQueryRequest());

        assertNotNull(result.getData().get("records"));
    }

    @Test
    void workerAcceptSuccess() {
        AuthContext.setRole("REPAIRER");
        AuthContext.setUserId(2L);
        RepairOrder order = createTestOrder(1L, 2, 3L, 2L);
        when(repairOrderService.getById(1L)).thenReturn(order);
        when(repairOrderService.updateById(any(RepairOrder.class))).thenReturn(true);

        controller.workerAccept(1L);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderService).updateById(captor.capture());
        assertEquals(3, captor.getValue().getRepairStatus());
    }

    @Test
    void workerAcceptRejectsIfNotAssignedWorker() {
        AuthContext.setRole("REPAIRER");
        AuthContext.setUserId(5L);
        RepairOrder order = createTestOrder(1L, 2, 3L, 2L);
        when(repairOrderService.getById(1L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> controller.workerAccept(1L));
    }

    // ==================== optimistic locking ====================

    @Test
    void cancelRejectsStaleVersion() {
        RepairOrder order = createTestOrder(1L, 1, 1L, null);
        when(repairOrderService.getById(1L)).thenReturn(order);
        when(repairOrderService.updateById(any(RepairOrder.class))).thenReturn(false);

        assertThrows(BusinessException.class, () -> controller.cancel(1L));
    }

    @Test
    void workerCompleteSuccess() {
        AuthContext.setRole("REPAIRER");
        AuthContext.setUserId(2L);
        RepairOrder order = createTestOrder(1L, 3, 3L, 2L);
        when(repairOrderService.getById(1L)).thenReturn(order);
        when(repairOrderService.updateById(any(RepairOrder.class))).thenReturn(true);

        Map<String, String> body = Map.of("remark", "已修好");

        controller.workerComplete(1L, body);

        ArgumentCaptor<RepairOrder> captor = ArgumentCaptor.forClass(RepairOrder.class);
        verify(repairOrderService).updateById(captor.capture());
        assertEquals(4, captor.getValue().getRepairStatus());
        assertEquals("已修好", captor.getValue().getRemark());
    }

    // ==================== detail ====================

    @Test
    void detailStudentCanViewOwnOrder() {
        RepairOrder order = createTestOrder(1L, 1, 1L, null);
        when(repairOrderService.getById(1L)).thenReturn(order);

        var result = controller.detail(1L);

        assertEquals(1L, result.getData().get("id"));
    }

    @Test
    void detailThrowsWhenOrderNotFound() {
        when(repairOrderService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.detail(999L));
    }

    // ==================== categories ====================

    @Test
    void categoriesReturnsEnabledOnly() {
        RepairCategory cat = new RepairCategory();
        cat.setId(1L);
        cat.setCategoryName("水电维修");
        when(repairCategoryService.list(any(Wrapper.class))).thenReturn(List.of(cat));

        var result = controller.categories();

        assertEquals(1, result.getData().size());
    }

    // ==================== delete ====================

    @Test
    void adminCanDeleteOrder() {
        AuthContext.setRole("ADMIN");

        controller.delete(1L);

        verify(repairOrderService).removeById(1L);
    }

    @Test
    void deleteRejectsNonAdmin() {
        assertThrows(BusinessException.class, () -> controller.delete(1L));
    }
}
