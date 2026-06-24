package com.example.dormitoryrepair.common.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Sets security-related HTTP response headers on every request (except OPTIONS preflight).
 * Provides defense-in-depth beyond the JWT authentication layer.
 */
@Component
@Order(1)
public class SecurityHeaderFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // Modern XSS protection (disables legacy reflective XSS filter)
        response.setHeader("X-XSS-Protection", "0");

        // NOTE: Strict-Transport-Security (HSTS) is deliberately omitted here.
        // The development server runs on plain HTTP; setting HSTS on HTTP responses
        // causes Android WebView to cache a one-year HTTPS upgrade rule for the
        // backend's IP/hostname, silently breaking all subsequent API calls.

        // Content Security Policy — restrict resource sources
        // Deliberately omitted: Android WebView may incorrectly merge CSP from API responses
        // into the main document, breaking fetch() calls to 10.0.2.2.

        // Referrer policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions Policy — disable unnecessary browser features
        response.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=(), usb=()");

        filterChain.doFilter(request, response);
    }
}
