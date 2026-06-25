package com.example.dormitoryrepair.controller;

import com.example.dormitoryrepair.common.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("timestamp", LocalDateTime.now().toString());
        info.put("status", "UP");

        // Check database connectivity
        Map<String, Object> db = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            db.put("status", "UP");
            db.put("database", conn.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            db.put("status", "DOWN");
            db.put("error", e.getMessage());
            log.warn("Health check: database unreachable: {}", e.getMessage());
        }
        info.put("database", db);

        // Check Redis connectivity
        Map<String, Object> redis = new LinkedHashMap<>();
        try {
            String pong = stringRedisTemplate.getConnectionFactory().getConnection().ping();
            redis.put("status", isRedisHealthy(pong) ? "UP" : "DEGRADED");
        } catch (Exception e) {
            redis.put("status", "DOWN");
            redis.put("error", e.getMessage());
            log.warn("Health check: Redis unreachable: {}", e.getMessage());
        }
        info.put("redis", redis);

        // Overall status
        boolean allUp = "UP".equals(db.get("status")) && "UP".equals(redis.get("status"));
        info.put("status", allUp ? "UP" : "DEGRADED");

        return ApiResponse.success(info);
    }

    /** Liveness probe — lightweight check that the process is alive. */
    @GetMapping("/live")
    public ApiResponse<Map<String, String>> liveness() {
        return ApiResponse.success(Map.of("status", "UP"));
    }

    /** Readiness probe — checks that dependencies are ready. */
    @GetMapping("/ready")
    public ApiResponse<Map<String, Object>> readiness() {
        Map<String, Object> info = new LinkedHashMap<>();
        boolean dbUp;
        boolean redisUp;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            dbUp = true;
        } catch (Exception e) {
            dbUp = false;
        }

        try {
            String pong = stringRedisTemplate.getConnectionFactory().getConnection().ping();
            redisUp = isRedisHealthy(pong);
        } catch (Exception e) {
            redisUp = false;
        }

        info.put("database", dbUp ? "UP" : "DOWN");
        info.put("redis", redisUp ? "UP" : "DOWN");
        info.put("status", dbUp && redisUp ? "UP" : "DOWN");
        return ApiResponse.success(info);
    }

    private boolean isRedisHealthy(String pong) {
        return "PONG".equalsIgnoreCase(pong) || "UP".equalsIgnoreCase(pong);
    }
}
