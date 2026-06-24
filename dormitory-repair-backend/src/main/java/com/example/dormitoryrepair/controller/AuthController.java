package com.example.dormitoryrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.auth.JwtUtil;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.common.util.IpUtil;
import com.example.dormitoryrepair.dto.auth.CaptchaResponse;
import com.example.dormitoryrepair.dto.auth.ForgotPasswordRequest;
import com.example.dormitoryrepair.dto.auth.LoginRequest;
import com.example.dormitoryrepair.dto.auth.RegisterRequest;
import com.example.dormitoryrepair.entity.SysUser;
import com.example.dormitoryrepair.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final int LOGIN_RATE_LIMIT = 10;
    private static final String LOGIN_LIMIT_PREFIX = "login:limit:";

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final HttpServletRequest httpServletRequest;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        verifyCaptcha(request.getCaptchaKey(), request.getCaptchaCode());

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
        user.setGender(request.getGender());
        user.setDormitoryBuilding(request.getDormitoryBuilding());
        user.setRoomNo(request.getRoomNo());
        user.setRole("STUDENT");
        user.setStatus(1);
        sysUserService.save(user);
        return ApiResponse.success("注册成功", null);
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String clientIp = IpUtil.getClientIp(httpServletRequest);
        String limitKey = LOGIN_LIMIT_PREFIX + clientIp;
        Boolean created = stringRedisTemplate.opsForValue().setIfAbsent(limitKey, "1", Duration.ofSeconds(60));
        Long count;
        if (Boolean.TRUE.equals(created)) {
            count = 1L;
        } else {
            count = stringRedisTemplate.opsForValue().increment(limitKey);
            // Ensure TTL is always applied, even after increment
            stringRedisTemplate.expire(limitKey, Duration.ofSeconds(60));
        }
        if (count != null && count > LOGIN_RATE_LIMIT) {
            throw new BusinessException(429, "登录尝试过于频繁，请稍后再试");
        }

        verifyCaptcha(request.getCaptchaKey(), request.getCaptchaCode());

        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getStatus, 1)
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", buildUserInfo(user));
        return ApiResponse.success(data);
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        String code = generateCaptchaCode(4);
        String base64Image = generateCaptchaImage(code);

        stringRedisTemplate.opsForValue().set("captcha:" + captchaKey, code, Duration.ofMinutes(5));

        return ApiResponse.success(new CaptchaResponse(captchaKey, base64Image));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Long userId = AuthContext.getUserId();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return ApiResponse.success(buildUserInfo(user));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success("退出成功", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        verifyCaptcha(request.getCaptchaKey(), request.getCaptchaCode());

        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户不存在");
        }
        if (user.getStudentNo() == null || !user.getStudentNo().equals(request.getStudentNo())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "学号验证失败");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserService.updateById(user);
        return ApiResponse.success("密码重置成功，请使用新密码登录", null);
    }

    private void verifyCaptcha(String captchaKey, String captchaCode) {
        String storedCode = stringRedisTemplate.opsForValue().get("captcha:" + captchaKey);
        if (storedCode == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码已过期，请刷新后重试");
        }
        if (!storedCode.equalsIgnoreCase(captchaCode)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码错误");
        }
        stringRedisTemplate.delete("captcha:" + captchaKey);
    }

    private String generateCaptchaCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateCaptchaImage(String code) {
        int width = 160;
        int height = 50;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(250, 250, 250));
        g.fillRect(0, 0, width, height);

        // Noise lines
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        g.setColor(new Color(220, 220, 220));
        for (int i = 0; i < 8; i++) {
            g.drawLine(rng.nextInt(width), rng.nextInt(height),
                    rng.nextInt(width), rng.nextInt(height));
        }

        // Text
        g.setColor(new Color(30, 30, 30));
        g.setFont(new Font("Arial", Font.BOLD, 28));
        for (int i = 0; i < code.length(); i++) {
            double rotate = Math.toRadians(rng.nextDouble(-20, 20));
            g.rotate(rotate, 25 + i * 30, 34);
            g.drawString(String.valueOf(code.charAt(i)), 18 + i * 30, 38);
            g.rotate(-rotate, 25 + i * 30, 34);
        }

        // Noise dots
        g.setColor(new Color(180, 180, 180));
        for (int i = 0; i < 60; i++) {
            g.fillOval(rng.nextInt(width), rng.nextInt(height), 1, 1);
        }

        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.ERROR.getCode(), "验证码生成失败");
        }
    }

    private Map<String, Object> buildUserInfo(SysUser user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("studentNo", user.getStudentNo());
        userInfo.put("phone", user.getPhone());
        userInfo.put("gender", user.getGender());
        userInfo.put("dormitoryBuilding", user.getDormitoryBuilding());
        userInfo.put("roomNo", user.getRoomNo());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("role", user.getRole());
        return userInfo;
    }
}
