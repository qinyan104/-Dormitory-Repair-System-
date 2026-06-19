package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.entity.RepairCategory;
import com.example.dormitoryrepair.mapper.RepairOrderMapper;
import com.example.dormitoryrepair.service.RepairCategoryService;
import com.example.dormitoryrepair.service.RepairOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {

    @Mock
    private RepairOrderService repairOrderService;

    @Mock
    private RepairCategoryService repairCategoryService;

    @Mock
    private RepairOrderMapper repairOrderMapper;

    private StatisticsController controller;

    @BeforeEach
    void setUp() {
        controller = new StatisticsController(repairOrderService, repairCategoryService, repairOrderMapper);
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void summaryReturnsExpectedCountFields() {
        when(repairOrderService.count()).thenReturn(10L);
        when(repairOrderService.count(any(LambdaQueryWrapper.class))).thenReturn(2L, 3L, 4L, 1L, 5L, 0L);

        Map<String, Object> data = controller.summary().getData();

        assertEquals(10L, data.get("total"));
        assertEquals(2L, data.get("pending"));
        assertEquals(3L, data.get("assigned"));
        assertEquals(4L, data.get("repairing"));
        assertEquals(1L, data.get("waitingConfirm"));
        assertEquals(5L, data.get("completed"));
        assertEquals(0L, data.get("cancelled"));
        assertEquals(0L, data.get("today"));
    }

    @Test
    void categoryAggregatesRepairOrdersByCategory() {
        RepairCategory category1 = new RepairCategory();
        category1.setId(1L);
        category1.setCategoryName("水电维修");
        RepairCategory category2 = new RepairCategory();
        category2.setId(2L);
        category2.setCategoryName("家具维修");

        List<Map<String, Object>> mapperRows = List.of(
                Map.of("categoryId", 1L, "count", 2L),
                Map.of("categoryId", 2L, "count", 1L)
        );

        when(repairCategoryService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(category1, category2));
        when(repairOrderMapper.countGroupByCategory()).thenReturn(mapperRows);

        List<Map<String, Object>> data = controller.category().getData();

        assertEquals(2, data.size());
        assertEquals(2L, data.get(0).get("count"));
        assertEquals(1L, data.get(1).get("count"));
    }

    @Test
    void statusAggregatesRepairOrdersByStatus() {
        List<Map<String, Object>> mapperRows = List.of(
                Map.of("status", 1, "count", 1L),
                Map.of("status", 2, "count", 2L)
        );

        when(repairOrderMapper.countGroupByStatus()).thenReturn(mapperRows);

        List<Map<String, Object>> data = controller.status().getData();

        assertEquals(2, data.size());
        assertEquals(1L, data.get(0).get("count"));
        assertEquals(2L, data.get(1).get("count"));
    }
}
