package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.dto.user.ChangePasswordRequest;
import com.example.dormitoryrepair.dto.user.UserCreateRequest;
import com.example.dormitoryrepair.dto.user.UserProfileUpdateRequest;
import com.example.dormitoryrepair.dto.user.UserQueryRequest;
import com.example.dormitoryrepair.dto.user.UserStatusUpdateRequest;
import com.example.dormitoryrepair.entity.SysUser;
import com.example.dormitoryrepair.service.SysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private SysUserService sysUserService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(sysUserService, passwordEncoder);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    // ==================== profile ====================

    @Test
    void profileReturnsCurrentUser() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("STUDENT");
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("student01");
        user.setRealName("张三");
        when(sysUserService.getById(1L)).thenReturn(user);

        Map<String, Object> data = controller.profile().getData();

        assertEquals("student01", data.get("username"));
        assertEquals("张三", data.get("realName"));
    }

    @Test
    void profileThrowsWhenUserNotFound() {
        AuthContext.setUserId(999L);
        when(sysUserService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.profile());
    }

    // ==================== updateProfile ====================

    @Test
    void updateProfilePersistsCurrentUserFields() {
        AuthContext.setUserId(2L);
        AuthContext.setRole("STUDENT");
        SysUser currentUser = new SysUser();
        currentUser.setId(2L);
        currentUser.setUsername("student01");
        when(sysUserService.getById(2L)).thenReturn(currentUser);

        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setRealName("李四");
        request.setPhone("13812345678");
        request.setGender(1);
        request.setDormitoryBuilding("2号楼");
        request.setRoomNo("402");

        controller.updateProfile(request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).updateById(captor.capture());
        assertEquals("李四", captor.getValue().getRealName());
        assertEquals("402", captor.getValue().getRoomNo());
        assertEquals(1, captor.getValue().getGender());
    }

    @Test
    void updateProfilePreservesGenderWhenNull() {
        AuthContext.setUserId(2L);
        AuthContext.setRole("STUDENT");
        SysUser currentUser = new SysUser();
        currentUser.setId(2L);
        currentUser.setGender(1);
        when(sysUserService.getById(2L)).thenReturn(currentUser);

        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setRealName("李四");
        request.setPhone("13812345678");

        controller.updateProfile(request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).updateById(captor.capture());
        assertNull(captor.getValue().getGender(), "gender should be null when not provided");
    }

    // ==================== changePassword ====================

    @Test
    void changePasswordSuccess() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("STUDENT");
        SysUser currentUser = new SysUser();
        currentUser.setId(1L);
        currentUser.setPassword("encoded-old");
        when(sysUserService.getById(1L)).thenReturn(currentUser);
        when(passwordEncoder.matches("old-pass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old-pass");
        request.setNewPassword("new-pass");

        controller.changePassword(request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).updateById(captor.capture());
        assertEquals("encoded-new", captor.getValue().getPassword());
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {
        AuthContext.setUserId(1L);
        SysUser currentUser = new SysUser();
        currentUser.setId(1L);
        currentUser.setPassword("encoded-old");
        when(sysUserService.getById(1L)).thenReturn(currentUser);
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrong");
        request.setNewPassword("new-pass");

        assertThrows(BusinessException.class, () -> controller.changePassword(request));
    }

    // ==================== page (admin) ====================

    @Test
    void pageReturnsPaginatedUsersForAdmin() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
        Page<SysUser> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(new SysUser()));
        page.setTotal(1);
        when(sysUserService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Map<String, Object> data = controller.page(new UserQueryRequest()).getData();

        assertEquals(1L, data.get("total"));
    }

    @Test
    void pageRejectsNonAdmin() {
        AuthContext.setRole("STUDENT");

        assertThrows(BusinessException.class, () -> controller.page(new UserQueryRequest()));
    }

    // ==================== detail ====================

    @Test
    void detailReturnsUserForAdmin() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("student01");
        when(sysUserService.getById(2L)).thenReturn(user);

        Map<String, Object> data = controller.detail(2L).getData();

        assertEquals("student01", data.get("username"));
    }

    @Test
    void detailThrowsNotFound() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
        when(sysUserService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.detail(999L));
    }

    @Test
    void detailRejectsNonAdmin() {
        AuthContext.setRole("STUDENT");

        assertThrows(BusinessException.class, () -> controller.detail(1L));
    }

    // ==================== save (create user) ====================

    @Test
    void createUserSuccess() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
        when(sysUserService.count(any())).thenReturn(0L);

        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("newuser");
        request.setPassword("pass123");
        request.setRealName("新用户");
        request.setRole("STUDENT");

        controller.save(request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).save(captor.capture());
        assertEquals("newuser", captor.getValue().getUsername());
        assertEquals("STUDENT", captor.getValue().getRole());
    }

    @Test
    void createUserRejectsDuplicateUsername() {
        AuthContext.setRole("ADMIN");
        when(sysUserService.count(any())).thenReturn(1L);

        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("existing");

        assertThrows(BusinessException.class, () -> controller.save(request));
    }

    @Test
    void createUserRejectsInvalidRole() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");

        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("newuser");
        request.setPassword("pass123");
        request.setRealName("Test");
        request.setRole("SUPERADMIN");

        // After fix, invalid role should be rejected
        assertThrows(Exception.class, () -> controller.save(request));
    }

    @Test
    void createUserRejectsNonAdmin() {
        AuthContext.setRole("STUDENT");

        assertThrows(BusinessException.class, () -> controller.save(new UserCreateRequest()));
    }

    // ==================== updateStatus ====================

    @Test
    void updateStatusSuccess() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
        SysUser target = new SysUser();
        target.setId(2L);
        target.setRole("STUDENT");
        when(sysUserService.getById(2L)).thenReturn(target);

        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setStatus(0);
        controller.updateStatus(2L, request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    void updateStatusRejectsChangingSelfStatus() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setStatus(0);

        assertThrows(BusinessException.class, () -> controller.updateStatus(1L, request));
    }

    @Test
    void updateStatusRejectsDisablingAdmin() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
        SysUser target = new SysUser();
        target.setId(2L);
        target.setRole("ADMIN");
        when(sysUserService.getById(2L)).thenReturn(target);

        UserStatusUpdateRequest request = new UserStatusUpdateRequest();
        request.setStatus(0);

        assertThrows(BusinessException.class, () -> controller.updateStatus(2L, request));
    }

    // ==================== resetPassword (new endpoint) ====================

    @SuppressWarnings("unchecked")
    @Test
    void resetPasswordSuccess() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
        SysUser target = new SysUser();
        target.setId(2L);
        target.setPassword("old-encoded");
        when(sysUserService.getById(2L)).thenReturn(target);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-random");

        ApiResponse<?> response = controller.resetPassword(2L);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).updateById(captor.capture());
        assertEquals("encoded-random", captor.getValue().getPassword());
        // Response no longer returns plaintext password for security
        assertNotNull(response.getMessage());
    }

    @Test
    void resetPasswordRejectsNotFound() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
        when(sysUserService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.resetPassword(999L));
    }

    @Test
    void resetPasswordRejectsNonAdmin() {
        AuthContext.setRole("STUDENT");

        assertThrows(BusinessException.class, () -> controller.resetPassword(1L));
    }

    // ==================== delete ====================

    @Test
    void deleteSuccess() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
        SysUser target = new SysUser();
        target.setId(2L);
        target.setRole("STUDENT");
        when(sysUserService.getById(2L)).thenReturn(target);

        controller.delete(2L);

        verify(sysUserService).removeById(2L);
    }

    @Test
    void deleteRejectsDeletingSelf() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");

        assertThrows(BusinessException.class, () -> controller.delete(1L));
    }

    @Test
    void deleteRejectsDeletingAdmin() {
        AuthContext.setUserId(1L);
        AuthContext.setRole("ADMIN");
        SysUser target = new SysUser();
        target.setId(2L);
        target.setRole("ADMIN");
        when(sysUserService.getById(2L)).thenReturn(target);

        assertThrows(BusinessException.class, () -> controller.delete(2L));
    }

    @Test
    void deleteRejectsNonAdmin() {
        AuthContext.setRole("STUDENT");

        assertThrows(BusinessException.class, () -> controller.delete(1L));
    }
}
