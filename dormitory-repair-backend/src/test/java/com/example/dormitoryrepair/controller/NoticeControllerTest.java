package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.dto.notice.NoticeQueryRequest;
import com.example.dormitoryrepair.dto.notice.NoticeSaveRequest;
import com.example.dormitoryrepair.entity.Notice;
import com.example.dormitoryrepair.entity.SysUser;
import com.example.dormitoryrepair.service.NoticeService;
import com.example.dormitoryrepair.service.SysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeControllerTest {

    @Mock
    private NoticeService noticeService;

    @Mock
    private SysUserService sysUserService;

    private NoticeController controller;

    @BeforeEach
    void setUp() {
        controller = new NoticeController(noticeService, sysUserService);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    // ==================== create ====================

    @Test
    void createSetsPublisherAndPublishTimeForPublishedNotice() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
        SysUser admin = new SysUser();
        admin.setId(1L);
        admin.setRealName("系统管理员");
        when(sysUserService.getById(1L)).thenReturn(admin);

        NoticeSaveRequest request = new NoticeSaveRequest();
        request.setTitle("测试公告");
        request.setContent("内容");
        request.setStatus(1);
        when(noticeService.save(any())).thenReturn(true);

        controller.create(request);

        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeService).save(captor.capture());
        Notice notice = captor.getValue();
        assertEquals("系统管理员", notice.getPublisher());
        assertNotNull(notice.getPublishTime());
    }

    @Test
    void createDoesNotSetPublishTimeForDraft() {
        AuthContext.setRole("ADMIN");
        when(sysUserService.getById(any())).thenReturn(new SysUser());

        NoticeSaveRequest request = new NoticeSaveRequest();
        request.setTitle("草稿公告");
        request.setContent("草稿内容");
        request.setStatus(0);

        controller.create(request);

        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeService).save(captor.capture());
        assertNull(captor.getValue().getPublishTime());
    }

    @Test
    void createRejectsNonAdmin() {
        AuthContext.setRole("STUDENT");

        assertThrows(BusinessException.class, () -> controller.create(new NoticeSaveRequest()));
    }

    // ==================== update ====================

    @Test
    void updateSuccess() {
        AuthContext.setRole("ADMIN");
        Notice existing = new Notice();
        existing.setId(1L);
        existing.setTitle("旧标题");
        existing.setStatus(0);
        when(noticeService.getById(1L)).thenReturn(existing);

        NoticeSaveRequest request = new NoticeSaveRequest();
        request.setId(1L);
        request.setTitle("新标题");
        request.setContent("新内容");
        request.setIsTop(0);
        request.setStatus(1);

        controller.update(request);

        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeService).updateById(captor.capture());
        assertEquals("新标题", captor.getValue().getTitle());
        assertNotNull(captor.getValue().getPublishTime());
    }

    @Test
    void updateRejectsMissingId() {
        AuthContext.setRole("ADMIN");
        NoticeSaveRequest request = new NoticeSaveRequest();
        request.setTitle("无ID");

        assertThrows(BusinessException.class, () -> controller.update(request));
    }

    @Test
    void updateRejectsNotFound() {
        AuthContext.setRole("ADMIN");
        when(noticeService.getById(999L)).thenReturn(null);

        NoticeSaveRequest request = new NoticeSaveRequest();
        request.setId(999L);
        request.setTitle("不存在");

        assertThrows(BusinessException.class, () -> controller.update(request));
    }

    @Test
    void updateRejectsNonAdmin() {
        AuthContext.setRole("STUDENT");

        assertThrows(BusinessException.class, () -> controller.update(new NoticeSaveRequest()));
    }

    // ==================== delete ====================

    @Test
    void deleteSuccess() {
        AuthContext.setRole("ADMIN");

        controller.delete(1L);

        verify(noticeService).removeById(1L);
    }

    @Test
    void deleteRejectsNonAdmin() {
        AuthContext.setRole("STUDENT");

        assertThrows(BusinessException.class, () -> controller.delete(1L));
    }

    // ==================== page ====================

    @Test
    void pageForAdminReturnsAll() {
        AuthContext.setRole("ADMIN");
        Page<Notice> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(new Notice()));
        page.setTotal(1);
        when(noticeService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        NoticeQueryRequest request = new NoticeQueryRequest();
        var result = controller.page(request);

        assertNotNull(result.getData().get("records"));
    }

    @Test
    void pageForStudentForcesPublishedStatus() {
        AuthContext.setUserId(2L);
        AuthContext.setRole("STUDENT");
        Page<Notice> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of());
        page.setTotal(0);
        when(noticeService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        NoticeQueryRequest request = new NoticeQueryRequest();
        controller.page(request);

        assertEquals(1, request.getStatus());
    }

    // ==================== detail ====================

    @Test
    void detailReturnsNotice() {
        AuthContext.setRole("STUDENT");
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setStatus(1);
        when(noticeService.getById(1L)).thenReturn(notice);

        var result = controller.detail(1L);
        assertEquals(1L, result.getData().getId());
    }

    @Test
    void detailBlocksDraftNoticeForStudent() {
        AuthContext.setUserId(2L);
        AuthContext.setRole("STUDENT");
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setStatus(0);
        when(noticeService.getById(1L)).thenReturn(notice);

        assertThrows(BusinessException.class, () -> controller.detail(1L));
    }

    @Test
    void detailAllowsDraftForAdmin() {
        AuthContext.setRole("ADMIN");
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setStatus(0);
        when(noticeService.getById(1L)).thenReturn(notice);

        var result = controller.detail(1L);
        assertEquals(1L, result.getData().getId());
    }

    @Test
    void detailThrowsNotFound() {
        when(noticeService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.detail(999L));
    }
}
