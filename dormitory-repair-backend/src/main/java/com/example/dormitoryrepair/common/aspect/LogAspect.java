package com.example.dormitoryrepair.common.aspect;

import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.util.IpUtil;
import com.example.dormitoryrepair.entity.OperationLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final LogSaveService logSaveService;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Pointcut("@annotation(com.example.dormitoryrepair.common.annotation.Log)")
    public void logPointcut() {
    }

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result;
        String errorMessage = null;
        int status = 1;

        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            status = 0;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            saveLog(joinPoint, elapsed, status, errorMessage);
        }

        return result;
    }

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "oldPassword", "newPassword", "captchaCode"
    );

    public void saveLog(ProceedingJoinPoint joinPoint, long elapsed, int status, String errorMessage) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            com.example.dormitoryrepair.common.annotation.Log logAnnotation =
                    method.getAnnotation(com.example.dormitoryrepair.common.annotation.Log.class);

            OperationLog log = new OperationLog();
            log.setOperatorId(AuthContext.getUserId());
            log.setOperatorName(AuthContext.getUsername());
            log.setOperationType(logAnnotation.type());
            log.setDescription(logAnnotation.value());
            log.setMethodName(signature.getDeclaringType().getSimpleName() + "." + method.getName());
            log.setRequestParams(truncate(serializeArgsRedacted(joinPoint.getArgs()), 2000));
            log.setIpAddress(IpUtil.getClientIp(request));
            log.setExecutionTime(elapsed);
            log.setStatus(status);
            log.setErrorMessage(truncate(errorMessage, 500));
            log.setCreateTime(LocalDateTime.now());

            logSaveService.save(log);
        } catch (Exception e) {
            log.error("Failed to save operation log: {}", e.getMessage());
        }
    }

    private String serializeArgsRedacted(Object[] args) throws Exception {
        if (args == null || args.length == 0) return "[]";
        Object[] redacted = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                redacted[i] = null;
            } else if (isSimpleType(args[i])) {
                redacted[i] = args[i];
            } else {
                redacted[i] = deepRedact(args[i]);
            }
        }
        return objectMapper.writeValueAsString(redacted);
    }

    private Object deepRedact(Object obj) throws Exception {
        String json = objectMapper.writeValueAsString(obj);
        // Round-trip through a mutable map to redact sensitive keys
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> map = objectMapper.readValue(json, java.util.Map.class);
        redactMap(map);
        return map;
    }

    @SuppressWarnings("unchecked")
    private void redactMap(java.util.Map<String, Object> map) {
        for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
            if (SENSITIVE_FIELDS.contains(entry.getKey())) {
                entry.setValue("******");
            } else if (entry.getValue() instanceof java.util.Map) {
                redactMap((java.util.Map<String, Object>) entry.getValue());
            }
        }
    }

    private boolean isSimpleType(Object obj) {
        return obj instanceof String || obj instanceof Number || obj instanceof Boolean;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
