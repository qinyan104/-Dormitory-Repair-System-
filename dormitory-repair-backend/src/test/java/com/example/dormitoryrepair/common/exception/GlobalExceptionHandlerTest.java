package com.example.dormitoryrepair.common.exception;

import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void constraintViolationDoesNotLeakInternalDetails() {
        // Simulate a ConstraintViolationException with internal details
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Duplicate entry 'admin' for key 'sys_user.uk_username'");
        when(violation.getPropertyPath()).thenReturn(mock(Path.class));
        when(violation.getRootBeanClass()).thenReturn((Class) Object.class);

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ApiResponse<Void> response = handler.handleConstraintViolationException(ex);

        assertEquals(ResultCode.BAD_REQUEST.getCode(), response.getCode());
        // Response message should NOT contain internal database details
        assertFalse(response.getMessage().contains("Duplicate entry"),
                "Should not leak database constraint details");
        assertFalse(response.getMessage().contains("sys_user"),
                "Should not leak table names");
    }
}
