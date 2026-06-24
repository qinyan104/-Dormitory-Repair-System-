package com.example.dormitoryrepair.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

/**
 * Lightweight client for DeepSeek (OpenAI-compatible) chat completion API.
 * Extracted from AiService to separate HTTP/API concerns from business logic.
 */
@Slf4j
@Component
public class DeepSeekClient {

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${app.ai.model:deepseek-chat}")
    private String model;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public DeepSeekClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Call DeepSeek chat completion and return the response text.
     * Returns null if API key is not configured or the call fails.
     */
    public String call(String prompt) {
        if (!isConfigured()) {
            log.warn("AI API key not configured, skipping DeepSeek call");
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Collections.singletonList(message));
            requestBody.put("max_tokens", 512);
            requestBody.put("temperature", 0.3);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("DeepSeek API call failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract a JSON object from a text that may contain markdown fences or surrounding text.
     */
    public String extractJson(String text) {
        if (text == null) return "{}";
        text = text.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("```[a-zA-Z]*\\n?", "").replaceAll("```", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /**
     * Sanitize user-supplied text for inclusion in a prompt to prevent prompt injection.
     */
    public String sanitizeForPrompt(String input) {
        if (input == null) return "";
        String sanitized = input.replaceAll("</?[a-zA-Z_]+>", "");
        sanitized = sanitized.replace("\r\n", " ").replace("\n", " ").replace("\r", " ");
        return sanitized.trim();
    }
}
