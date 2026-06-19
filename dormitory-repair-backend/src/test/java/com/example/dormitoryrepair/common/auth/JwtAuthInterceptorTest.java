package com.example.dormitoryrepair.common.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthInterceptorTest {

    private final JwtAuthInterceptor interceptor = new JwtAuthInterceptor(Mockito.mock(JwtUtil.class));

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void preHandleAllowsCorsPreflightWithoutAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/category/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        assertNull(AuthContext.getUserId());
        assertNull(AuthContext.getRole());
        assertNull(AuthContext.getUsername());
    }
}
