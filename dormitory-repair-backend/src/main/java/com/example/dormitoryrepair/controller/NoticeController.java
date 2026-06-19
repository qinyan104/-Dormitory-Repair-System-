package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dormitoryrepair.common.annotation.Log;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.dto.notice.NoticeQueryRequest;
import com.example.dormitoryrepair.dto.notice.NoticeSaveRequest;
import com.example.dormitoryrepair.entity.Notice;
import com.example.dormitoryrepair.entity.SysUser;
import com.example.dormitoryrepair.service.NoticeService;
import com.example.dormitoryrepair.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final SysUserService sysUserService;

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(NoticeQueryRequest request) {
        if (!isAdmin()) {
            request.setStatus(1);
        }
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .like(request.getTitle() != null && !request.getTitle().isBlank(), Notice::getTitle, request.getTitle())
                .eq(request.getType() != null && !request.getType().isBlank(), Notice::getType, request.getType())
                .eq(request.getStatus() != null, Notice::getStatus, request.getStatus())
                .orderByDesc(Notice::getIsTop)
                .orderByDesc(Notice::getPublishTime)
                .orderByDesc(Notice::getCreateTime);
        IPage<Notice> result = noticeService.page(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return ApiResponse.success(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<Notice> detail(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!isAdmin() && !Objects.equals(notice.getStatus(), 1)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return ApiResponse.success(notice);
    }

    @Log(type = "NOTICE", value = "新增公告")
@PostMapping
    public ApiResponse<Notice> create(@Valid @RequestBody NoticeSaveRequest request) {
        ensureAdmin();
        SysUser currentUser = sysUserService.getById(AuthContext.getUserId());
        Notice notice = new Notice();
        applyRequest(notice, request);
        notice.setPublisher(resolvePublisher(currentUser));
        if (Objects.equals(request.getStatus(), 1)) {
            notice.setPublishTime(LocalDateTime.now());
        }
        noticeService.save(notice);
        return ApiResponse.success("新增成功", notice);
    }

    @Log(type = "NOTICE", value = "修改公告")
@PutMapping
    public ApiResponse<Notice> update(@Valid @RequestBody NoticeSaveRequest request) {
        ensureAdmin();
        if (request.getId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "公告ID不能为空");
        }
        Notice notice = noticeService.getById(request.getId());
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        applyRequest(notice, request);
        if (Objects.equals(request.getStatus(), 1) && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        if (!Objects.equals(request.getStatus(), 1)) {
            notice.setPublishTime(null);
        }
        noticeService.updateById(notice);
        return ApiResponse.success("修改成功", notice);
    }

    @Log(type = "NOTICE", value = "删除公告")
@DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ensureAdmin();
        noticeService.removeById(id);
        return ApiResponse.success("删除成功", null);
    }

    private void applyRequest(Notice notice, NoticeSaveRequest request) {
        notice.setTitle(request.getTitle());
        notice.setContent(request.getContent());
        notice.setType(request.getType() == null || request.getType().isBlank() ? "GENERAL" : request.getType());
        notice.setIsTop(request.getIsTop());
        notice.setStatus(request.getStatus());
    }

    private String resolvePublisher(SysUser currentUser) {
        if (currentUser == null) {
            return "管理员";
        }
        if (currentUser.getRealName() != null && !currentUser.getRealName().isBlank()) {
            return currentUser.getRealName();
        }
        return currentUser.getUsername();
    }

    private void ensureAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private boolean isAdmin() {
        return Objects.equals("ADMIN", AuthContext.getRole());
    }
}
