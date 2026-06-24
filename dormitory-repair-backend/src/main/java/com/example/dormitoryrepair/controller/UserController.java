package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dormitoryrepair.common.annotation.Log;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        SysUser user = currentUser();
        return ApiResponse.success(userView(user));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        SysUser user = currentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserService.updateById(user);
        return ApiResponse.success("密码修改成功", null);
    }

    private static final Set<String> VALID_ROLES = Set.of("STUDENT", "ADMIN", "REPAIRER");

    @Log(type = "USER", value = "新增用户")
    @PostMapping
    public ApiResponse<Void> save(@Valid @RequestBody UserCreateRequest request) {
        ensureAdmin();
        if (request.getRole() != null && !VALID_ROLES.contains(request.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "角色只能是 STUDENT、ADMIN 或 REPAIRER");
        }
        boolean exists = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())) > 0;
        if (exists) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setStudentNo(request.getStudentNo());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender() != null ? request.getGender() : 0);
        user.setDormitoryBuilding(request.getDormitoryBuilding());
        user.setRoomNo(request.getRoomNo());
        user.setRole(request.getRole() != null ? request.getRole() : "STUDENT");
        user.setStatus(1);
        user.setSkillType(request.getSkillType());
        user.setServiceArea(request.getServiceArea());
        sysUserService.save(user);
        return ApiResponse.success("用户创建成功", null);
    }

    @PutMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        SysUser user = currentUser();
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());
        user.setDormitoryBuilding(request.getDormitoryBuilding());
        user.setRoomNo(request.getRoomNo());
        user.setAvatar(request.getAvatar());
        sysUserService.updateById(user);
        return ApiResponse.success("更新成功", userView(user));
    }

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page(UserQueryRequest request) {
        ensureAdmin();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(request.getRealName() != null && !request.getRealName().isBlank(), SysUser::getRealName, request.getRealName())
                .like(request.getStudentNo() != null && !request.getStudentNo().isBlank(), SysUser::getStudentNo, request.getStudentNo())
                .eq(request.getStatus() != null, SysUser::getStatus, request.getStatus())
                .eq(request.getRole() != null && !request.getRole().isBlank(), SysUser::getRole, request.getRole())
                .orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> result = sysUserService.page(
                new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords().stream().map(this::userView).collect(Collectors.toList()));
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return ApiResponse.success(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        ensureAdmin();
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return ApiResponse.success(userView(user));
    }

    @Log(type = "USER", value = "修改用户状态")
    @PutMapping("/status/{id}")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusUpdateRequest request) {
        ensureAdmin();
        if (Objects.equals(id, AuthContext.getUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能修改自己的状态");
        }
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (Objects.equals("ADMIN", user.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "管理员账号不允许禁用");
        }
        user.setStatus(request.getStatus());
        sysUserService.updateById(user);
        return ApiResponse.success("状态更新成功", null);
    }

    @Log(type = "USER", value = "重置密码")
    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id) {
        ensureAdmin();
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        String defaultPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(defaultPassword));
        sysUserService.updateById(user);
        log.info("Password reset for user {} (id={}), new password is random", user.getUsername(), id);
        return ApiResponse.success("密码已重置为随机密码，请通知用户登录后修改", null);
    }

    @Log(type = "USER", value = "删除用户")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ensureAdmin();
        if (Objects.equals(id, AuthContext.getUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能删除当前登录账号");
        }
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (Objects.equals("ADMIN", user.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "管理员账号不允许删除");
        }
        sysUserService.removeById(id);
        return ApiResponse.success("删除成功", null);
    }

    private void ensureAdmin() {
        if (!Objects.equals("ADMIN", AuthContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private SysUser currentUser() {
        SysUser user = sysUserService.getById(AuthContext.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return user;
    }

    private Map<String, Object> userView(SysUser user) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("realName", user.getRealName());
        view.put("studentNo", user.getStudentNo());
        view.put("phone", user.getPhone());
        view.put("gender", user.getGender());
        view.put("dormitoryBuilding", user.getDormitoryBuilding());
        view.put("roomNo", user.getRoomNo());
        view.put("avatar", user.getAvatar());
        view.put("role", user.getRole());
        view.put("status", user.getStatus());
        return view;
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
