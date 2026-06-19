package com.example.dormitoryrepair.common.aspect;

import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.dto.user.UserCreateRequest;
import com.example.dormitoryrepair.entity.OperationLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogAspectTest {

    @Mock
    private LogSaveService logSaveService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private LogAspect logAspect;

    @Test
    void saveLogRedactsPasswordField() throws Exception {
        // Arrange: create a JoinPoint with a UserCreateRequest containing a password
        UserCreateRequest createRequest = new UserCreateRequest();
        createRequest.setUsername("testuser");
        createRequest.setPassword("my-secret-password-123");
        createRequest.setRealName("Test User");

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        Method method = LogAspectTestController.class.getMethod("createUser", UserCreateRequest.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringType()).thenReturn(LogAspectTestController.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{createRequest});
        when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");

        // Act
        logAspect.saveLog(joinPoint, 100L, 1, null);

        // Assert: password should be redacted in stored params
        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(logSaveService).save(captor.capture());

        String storedParams = captor.getValue().getRequestParams();
        assertNotNull(storedParams);
        assertFalse(storedParams.contains("my-secret-password-123"),
                "Password should be redacted from operation log");
        assertTrue(storedParams.contains("testuser"),
                "Non-sensitive fields should remain");
    }

    @Test
    void saveLogCapturesOperatorInfo() throws Exception {
        // Set up AuthContext with user info
        AuthContext.setUserId(42L);
        AuthContext.setUsername("admin_user");

        try {
            UserCreateRequest createRequest = new UserCreateRequest();
            createRequest.setUsername("testuser");
            createRequest.setPassword("pass123");
            createRequest.setRealName("Test User");

            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            MethodSignature signature = mock(MethodSignature.class);

            Method method = LogAspectTestController.class.getMethod("createUser", UserCreateRequest.class);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getMethod()).thenReturn(method);
            when(signature.getDeclaringType()).thenReturn(LogAspectTestController.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{createRequest});
            when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");

            logAspect.saveLog(joinPoint, 100L, 1, null);

            ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
            verify(logSaveService).save(captor.capture());

            assertEquals(42L, captor.getValue().getOperatorId());
            assertEquals("admin_user", captor.getValue().getOperatorName());
        } finally {
            AuthContext.clear();
        }
    }

    /**
     * Helper class to provide a real annotated method for reflection.
     */
    static class LogAspectTestController {
        @com.example.dormitoryrepair.common.annotation.Log(type = "USER", value = "新增用户")
        public void createUser(UserCreateRequest request) {
        }
    }
}
