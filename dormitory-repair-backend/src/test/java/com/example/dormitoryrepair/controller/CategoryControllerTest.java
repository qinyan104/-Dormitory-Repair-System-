package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.dto.category.CategoryQueryRequest;
import com.example.dormitoryrepair.dto.category.CategorySaveRequest;
import com.example.dormitoryrepair.entity.RepairCategory;
import com.example.dormitoryrepair.service.RepairCategoryService;
import com.example.dormitoryrepair.service.RepairOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private RepairCategoryService repairCategoryService;

    @Mock
    private RepairOrderService repairOrderService;

    private CategoryController controller;

    @BeforeEach
    void setUp() {
        controller = new CategoryController(repairCategoryService, repairOrderService);
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    // ==================== create ====================

    @Test
    void createSuccess() {
        CategorySaveRequest request = new CategorySaveRequest();
        request.setCategoryName("水电维修");
        request.setSortNum(1);
        request.setStatus(1);
        when(repairCategoryService.count(any())).thenReturn(0L);

        controller.create(request);

        ArgumentCaptor<RepairCategory> captor = ArgumentCaptor.forClass(RepairCategory.class);
        verify(repairCategoryService).save(captor.capture());
        assertEquals("水电维修", captor.getValue().getCategoryName());
        assertEquals(1, captor.getValue().getSortNum());
    }

    @Test
    void createRejectsDuplicateCategoryName() {
        CategorySaveRequest request = new CategorySaveRequest();
        request.setCategoryName("水电维修");
        when(repairCategoryService.count(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> controller.create(request));
        verify(repairCategoryService, never()).save(any());
    }

    @Test
    void createRejectsNonAdmin() {
        AuthContext.setRole("STUDENT");
        CategorySaveRequest request = new CategorySaveRequest();
        request.setCategoryName("水电维修");

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.create(request));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    // ==================== update ====================

    @Test
    void updateSuccess() {
        CategorySaveRequest request = new CategorySaveRequest();
        request.setId(1L);
        request.setCategoryName("水电维修-更新");
        request.setSortNum(2);
        request.setStatus(1);
        RepairCategory existing = new RepairCategory();
        existing.setId(1L);
        existing.setCategoryName("水电维修");

        when(repairCategoryService.getById(1L)).thenReturn(existing);
        when(repairCategoryService.count(any())).thenReturn(0L);

        controller.update(request);

        ArgumentCaptor<RepairCategory> captor = ArgumentCaptor.forClass(RepairCategory.class);
        verify(repairCategoryService).updateById(captor.capture());
        assertEquals("水电维修-更新", captor.getValue().getCategoryName());
    }

    @Test
    void updateRejectsMissingId() {
        CategorySaveRequest request = new CategorySaveRequest();
        request.setCategoryName("水电维修");

        assertThrows(BusinessException.class, () -> controller.update(request));
    }

    @Test
    void updateRejectsNotFound() {
        CategorySaveRequest request = new CategorySaveRequest();
        request.setId(999L);
        request.setCategoryName("水电维修");
        when(repairCategoryService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.update(request));
    }

    @Test
    void updateRejectsNonAdmin() {
        AuthContext.setRole("REPAIRER");
        CategorySaveRequest request = new CategorySaveRequest();
        request.setId(1L);
        request.setCategoryName("水电维修");

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.update(request));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    // ==================== updateStatus (new endpoint) ====================

    @Test
    void updateStatusSuccess() {
        RepairCategory existing = new RepairCategory();
        existing.setId(1L);
        existing.setStatus(1);
        when(repairCategoryService.getById(1L)).thenReturn(existing);

        controller.updateStatus(1L, 0);

        ArgumentCaptor<RepairCategory> captor = ArgumentCaptor.forClass(RepairCategory.class);
        verify(repairCategoryService).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    void updateStatusRejectsNotFound() {
        when(repairCategoryService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.updateStatus(999L, 0));
    }

    @Test
    void updateStatusRejectsNonAdmin() {
        AuthContext.setRole("STUDENT");

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.updateStatus(1L, 0));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    // ==================== page ====================

    @Test
    void pageReturnsCategories() {
        RepairCategory cat1 = new RepairCategory();
        cat1.setId(1L);
        cat1.setCategoryName("水电维修");
        RepairCategory cat2 = new RepairCategory();
        cat2.setId(2L);
        cat2.setCategoryName("家具维修");
        Page<RepairCategory> page = new Page<>(1, 10);
        page.setRecords(List.of(cat1, cat2));
        page.setTotal(2);
        when(repairCategoryService.page(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        CategoryQueryRequest request = new CategoryQueryRequest();
        Map<String, Object> result = controller.page(request).getData();

        List<?> records = (List<?>) result.get("records");
        assertEquals(2, records.size());
    }

    @Test
    void pageFiltersByCategoryName() {
        Page<RepairCategory> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(repairCategoryService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        CategoryQueryRequest request = new CategoryQueryRequest();
        request.setCategoryName("水电");
        controller.page(request);

        verify(repairCategoryService).page(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void pageRejectsNonAdmin() {
        AuthContext.setRole("STUDENT");
        CategoryQueryRequest request = new CategoryQueryRequest();

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.page(request));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    // ==================== delete ====================

    @Test
    void deleteSuccess() {
        when(repairOrderService.count(any())).thenReturn(0L);

        controller.delete(1L);

        verify(repairCategoryService).removeById(1L);
    }

    @Test
    void deleteRejectsCategoryReferencedByRepairOrders() {
        when(repairOrderService.count(any())).thenReturn(2L);

        assertThrows(BusinessException.class, () -> controller.delete(1L));
        verify(repairCategoryService, never()).removeById(any());
    }

    @Test
    void deleteRejectsNonAdmin() {
        AuthContext.setRole("REPAIRER");

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.delete(1L));
        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }

    // ==================== list (public) ====================

    @Test
    void listReturnsOnlyEnabledCategories() {
        RepairCategory cat = new RepairCategory();
        cat.setId(1L);
        cat.setCategoryName("水电维修");
        List<RepairCategory> categories = List.of(cat);
        when(repairCategoryService.list(any(LambdaQueryWrapper.class))).thenReturn(categories);

        assertSame(categories, controller.list().getData());
        assertEquals("水电维修", controller.list().getData().get(0).getCategoryName());
    }
}
