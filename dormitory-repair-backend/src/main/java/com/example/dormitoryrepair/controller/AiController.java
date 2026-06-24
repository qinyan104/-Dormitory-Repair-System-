package com.example.dormitoryrepair.controller;

import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.dto.ai.ClassifyRequest;
import com.example.dormitoryrepair.dto.ai.ClassifyResponse;
import com.example.dormitoryrepair.dto.ai.EvaluationResponse;
import com.example.dormitoryrepair.dto.ai.InsightRequest;
import com.example.dormitoryrepair.dto.ai.InsightResponse;
import com.example.dormitoryrepair.dto.ai.NaturalRepairRequest;
import com.example.dormitoryrepair.dto.ai.NaturalRepairResponse;
import com.example.dormitoryrepair.dto.ai.RecommendResponse;
import com.example.dormitoryrepair.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/classify")
    public ApiResponse<ClassifyResponse> classify(@Valid @RequestBody ClassifyRequest request) {
        ensureStudentOrAdmin();
        ClassifyResponse result = aiService.classify(request.getTitle(), request.getDescription());
        return ApiResponse.success(result);
    }

    @GetMapping("/recommend-worker")
    public ApiResponse<RecommendResponse> recommendWorker(@RequestParam Long orderId) {
        ensureAdmin();
        RecommendResponse result = aiService.recommendWorker(orderId);
        return ApiResponse.success(result);
    }

    @PostMapping("/evaluate-completion")
    public ApiResponse<EvaluationResponse> evaluateCompletion(@RequestParam Long orderId) {
        ensureAdmin();
        EvaluationResponse result = aiService.evaluateCompletion(orderId);
        return ApiResponse.success(result);
    }

    @PostMapping("/insights")
    public ApiResponse<InsightResponse> insights(@Valid @RequestBody InsightRequest request) {
        ensureAdmin();
        InsightResponse result = aiService.generateInsights(request);
        return ApiResponse.success(result);
    }

    @PostMapping("/natural-repair")
    public ApiResponse<NaturalRepairResponse> naturalRepair(@Valid @RequestBody NaturalRepairRequest request) {
        ensureStudentOrAdmin();
        NaturalRepairResponse result = aiService.naturalRepair(request.getText());
        return ApiResponse.success(result);
    }

    private void ensureAdmin() {
        if (!"ADMIN".equals(AuthContext.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private void ensureStudentOrAdmin() {
        String role = AuthContext.getRole();
        if (!"STUDENT".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
