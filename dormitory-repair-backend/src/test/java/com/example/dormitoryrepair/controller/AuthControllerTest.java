package com.example.dormitoryrepair.controller;

import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.auth.JwtUtil;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.dto.auth.LoginRequest;
import com.example.dormitoryrepair.dto.auth.RegisterRequest;
import com.example.dormitoryrepair.entity.SysUser;
import com.example.dormitoryrepair.service.SysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private SysUserService sysUserService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest httpServletRequest;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        lenient().when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        controller = new AuthController(sysUserService, passwordEncoder, jwtUtil, stringRedisTemplate, httpServletRequest);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    // ==================== captcha ====================

    @Test
    void captchaGeneratesValidResponse() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        var result = controller.captcha();

        assertNotNull(result.getData().getCaptchaKey());
        assertTrue(result.getData().getCaptchaImage().startsWith("data:image/png;base64,"));
        verify(stringRedisTemplate).opsForValue();
    }

    // ==================== register ====================

    @Test
    void registerSuccess() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha:key123")).thenReturn("ABCD");
        when(sysUserService.count(any())).thenReturn(0L);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded-pass");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("pass123");
        request.setRealName("新用户");
        request.setStudentNo("2024001");
        request.setPhone("13800138000");
        request.setCaptchaKey("key123");
        request.setCaptchaCode("ABCD");

        controller.register(request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserService).save(captor.capture());
        assertEquals("newuser", captor.getValue().getUsername());
        assertEquals("STUDENT", captor.getValue().getRole());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    void registerRejectsExpiredCaptcha() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha:key123")).thenReturn(null);

        RegisterRequest request = new RegisterRequest();
        request.setCaptchaKey("key123");
        request.setCaptchaCode("ABCD");

        assertThrows(BusinessException.class, () -> controller.register(request));
    }

    @Test
    void registerRejectsWrongCaptcha() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha:key123")).thenReturn("XYZ");

        RegisterRequest request = new RegisterRequest();
        request.setCaptchaKey("key123");
        request.setCaptchaCode("ABCD");

        assertThrows(BusinessException.class, () -> controller.register(request));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("ABCD");
        when(sysUserService.count(any())).thenReturn(1L);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setCaptchaKey("key123");
        request.setCaptchaCode("ABCD");

        assertThrows(BusinessException.class, () -> controller.register(request));
    }

    // ==================== login ====================

    @Test
    void loginSuccess() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.get("captcha:key456")).thenReturn("EFGH");

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encoded-pass");
        user.setRole("STUDENT");
        user.setRealName("测试");
        when(sysUserService.getOne(any())).thenReturn(user);
        when(passwordEncoder.matches("pass123", "encoded-pass")).thenReturn(true);
        when(jwtUtil.createToken(1L, "testuser", "STUDENT")).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("pass123");
        request.setCaptchaKey("key456");
        request.setCaptchaCode("EFGH");

        Map<String, Object> data = controller.login(request).getData();

        assertEquals("jwt-token", data.get("token"));
        assertNotNull(data.get("userInfo"));
    }

    @Test
    void loginRejectsInvalidCredentials() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.get("captcha:key456")).thenReturn("EFGH");
        when(sysUserService.getOne(any())).thenReturn(null);

        LoginRequest request = new LoginRequest();
        request.setUsername("baduser");
        request.setPassword("badpass");
        request.setCaptchaKey("key456");
        request.setCaptchaCode("EFGH");

        assertThrows(BusinessException.class, () -> controller.login(request));
    }

    @Test
    void loginRejectsWrongPassword() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.get("captcha:key456")).thenReturn("EFGH");

        SysUser user = new SysUser();
        user.setPassword("encoded-pass");
        when(sysUserService.getOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded-pass")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrong");
        request.setCaptchaKey("key456");
        request.setCaptchaCode("EFGH");

        assertThrows(BusinessException.class, () -> controller.login(request));
    }

    // ==================== me ====================

    @Test
    void meReturnsCurrentUser() {
        AuthContext.setUserId(1L);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole("STUDENT");
        when(sysUserService.getById(1L)).thenReturn(user);

        Map<String, Object> data = controller.me().getData();

        assertEquals("testuser", data.get("username"));
    }

    @Test
    void meThrowsWhenUserNotFound() {
        AuthContext.setUserId(999L);
        when(sysUserService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> controller.me());
    }

    // ==================== logout ====================

    @Test
    void logoutAlwaysSucceeds() {
        var result = controller.logout();
        assertEquals("退出成功", result.getMessage());
    }
}
