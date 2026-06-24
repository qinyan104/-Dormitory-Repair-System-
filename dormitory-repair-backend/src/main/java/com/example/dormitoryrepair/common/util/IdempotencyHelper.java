package com.example.dormitoryrepair.common.util;

import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Lightweight idempotency helper using Redis.
 * The client sends an {@code Idempotency-Key} header; the server
 * deduplicates within the TTL window.
 */
@Component
@RequiredArgsConstructor
public class IdempotencyHelper {

    private static final String PREFIX = "idempotent:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Generate a fresh idempotency key (for clients that don't supply one).
     */
    public String generateKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Try to claim an idempotency key.
     * @param key the idempotency key from the request
     * @param operation a short name for the operation (for logging)
     * @throws BusinessException if the key was already used within the TTL window
     */
    public void claim(String key, String operation) {
        if (key == null || key.isBlank()) {
            return; // idempotency is optional
        }
        String redisKey = PREFIX + key;
        Boolean created = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", DEFAULT_TTL);
        if (Boolean.FALSE.equals(created)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "请勿重复提交（" + operation + "），上次提交正在处理中");
        }
    }

    /**
     * Release a claimed key early (e.g. after the operation completes).
     */
    public void release(String key) {
        if (key != null && !key.isBlank()) {
            stringRedisTemplate.delete(PREFIX + key);
        }
    }
}
