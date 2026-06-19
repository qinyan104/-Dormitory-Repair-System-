package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dormitoryrepair.common.annotation.Log;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.dto.category.CategoryQueryRequest;
import com.example.dormitoryrepair.dto.category.CategorySaveRequest;
import com.example.dormitoryrepair.entity.RepairCategory;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.service.RepairCategoryService;
import com.example.dormitoryrepair.service.RepairOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final RepairCategoryService repairCategoryService;
    private final RepairOrderService repairOrderService;

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(CategoryQueryRequest request) {
        ensureAdmin();
        LambdaQueryWrapper<RepairCategory> wrapper = new LambdaQueryWrapper<RepairCategory>()
                .like(request.getCategoryName() != null && !request.getCategoryName().isBlank(),
                        RepairCategory::getCategoryName, request.getCategoryName())
                .eq(request.getStatus() != null, RepairCategory::getStatus, request.getStatus())
                .orderByAsc(RepairCategory::getSortNum)
                .orderByDesc(RepairCategory::getCreateTime);
        IPage<RepairCategory> result = repairCategoryService.page(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return ApiResponse.success(data);
    }

    @Log(type = "CATEGORY", value = "新增分类")
    @PostMapping
    public ApiResponse<RepairCategory> create(@Valid @RequestBody CategorySaveRequest request) {
        ensureAdmin();
        ensureUniqueName(request.getCategoryName(), null);
        RepairCategory category = toEntity(request, new RepairCategory());
        repairCategoryService.save(category);
        return ApiResponse.success("新增成功", category);
    }

    @Log(type = "CATEGORY", value = "修改分类")
    @PutMapping
    public ApiResponse<RepairCategory> update(@Valid @RequestBody CategorySaveRequest request) {
        ensureAdmin();
        if (request.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "分类ID不能为空");
        }
        RepairCategory category = repairCategoryService.getById(request.getId());
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ensureUniqueName(request.getCategoryName(), request.getId());
        repairCategoryService.updateById(toEntity(request, category));
        return ApiResponse.success("修改成功", category);
    }

    @Log(type = "CATEGORY", value = "删除分类")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ensureAdmin();
        long orderCount = repairOrderService.count(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getCategoryId, id));
        if (orderCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该分类下已有报修单，不能删除");
        }
        repairCategoryService.removeById(id);
        return ApiResponse.success("删除成功", null);
    }

    @Log(type = "CATEGORY", value = "修改分类状态")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        ensureAdmin();
        RepairCategory category = repairCategoryService.getById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        category.setStatus(status);
        repairCategoryService.updateById(category);
        return ApiResponse.success("状态更新成功", null);
    }

    @GetMapping("/list")
    public ApiResponse<List<RepairCategory>> list() {
        List<RepairCategory> categories = repairCategoryService.list(new LambdaQueryWrapper<RepairCategory>()
                .eq(RepairCategory::getStatus, 1)
                .orderByAsc(RepairCategory::getSortNum)
                .orderByDesc(RepairCategory::getCreateTime));
        return ApiResponse.success(categories);
    }

    private RepairCategory toEntity(CategorySaveRequest request, RepairCategory category) {
        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setSortNum(request.getSortNum());
        category.setStatus(request.getStatus());
        return category;
    }

    private void ensureUniqueName(String categoryName, Long excludeId) {
        long exists = repairCategoryService.count(new LambdaQueryWrapper<RepairCategory>()
                .eq(RepairCategory::getCategoryName, categoryName)
                .ne(excludeId != null, RepairCategory::getId, excludeId));
        if (exists > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "分类名称已存在");
        }
    }

    private void ensureAdmin() {
        if (!Objects.equals("ADMIN", AuthContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

}
