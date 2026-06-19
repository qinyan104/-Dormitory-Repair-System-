package com.example.dormitoryrepair.controller;

import com.example.dormitoryrepair.common.auth.AuthContext;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ApiResponse;
import com.example.dormitoryrepair.dto.ai.ClassifyRequest;
import com.example.dormitoryrepair.dto.ai.ClassifyResponse;
import com.example.dormitoryrepair.dto.ai.EvaluationResponse;
import com.example.dormitoryrepair.dto.ai.InsightRequest;
import com.example.dormitoryrepair.dto.ai.InsightResponse;
import com.example.dormitoryrepair.dto.ai.RecommendResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import com.example.dormitoryrepair.service.AiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private AiService aiService;

    @InjectMocks
    private AiController aiController;

    @BeforeEach
    void setUp() {
        AuthContext.setRole("STUDENT");
        AuthContext.setUserId(1L);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void classify_ReturnsResponse() {
        ClassifyResponse mockResp = new ClassifyResponse();
        mockResp.setCategory("水电维修");
        mockResp.setUrgency("一般");
        mockResp.setReason("测试理由");
        when(aiService.classify(anyString(), anyString())).thenReturn(mockResp);

        ClassifyRequest request = new ClassifyRequest();
        request.setTitle("水龙头漏水");
        request.setDescription("一直滴水");

        ApiResponse<ClassifyResponse> response = aiController.classify(request);
        assertNotNull(response);
        assertEquals(200, response.getCode());
        ClassifyResponse result = response.getData();
        assertNotNull(result);
        assertEquals("水电维修", result.getCategory());
        assertEquals("一般", result.getUrgency());
    }

    @Test
    void classify_WhenAiFails_ReturnsSuccessWithNullData() {
        when(aiService.classify(anyString(), anyString())).thenReturn(null);

        ClassifyRequest request = new ClassifyRequest();
        request.setTitle("test");
        request.setDescription("test");

        ApiResponse<ClassifyResponse> response = aiController.classify(request);
        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertNull(response.getData());
    }

    @Test
    void recommendWorker_ReturnsResponse() {
        AuthContext.setRole("ADMIN");
        RecommendResponse mockResp = new RecommendResponse();
        mockResp.setAutoAssigned(false);
        RecommendResponse.WorkerRanking rank = new RecommendResponse.WorkerRanking();
        rank.setWorkerId(1L);
        rank.setWorkerName("张师傅");
        rank.setMatchScore(85);
        rank.setReason("擅长该类维修");
        mockResp.setRankings(java.util.List.of(rank));
        when(aiService.recommendWorker(anyLong())).thenReturn(mockResp);

        ApiResponse<RecommendResponse> response = aiController.recommendWorker(1L);
        assertNotNull(response);
        assertEquals(200, response.getCode());
        RecommendResponse result = response.getData();
        assertNotNull(result);
        assertFalse(result.getAutoAssigned());
        assertEquals(1, result.getRankings().size());
        assertEquals(Long.valueOf(1), result.getRankings().get(0).getWorkerId());
    }

    @Test
    void recommendWorker_WhenAiFails_ReturnsSuccessWithNullData() {
        AuthContext.setRole("ADMIN");
        when(aiService.recommendWorker(anyLong())).thenReturn(null);
        ApiResponse<RecommendResponse> response = aiController.recommendWorker(999L);
        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertNull(response.getData());
    }

    @Test
    void classify_RejectsRepairer() {
        AuthContext.setRole("REPAIRER");

        ClassifyRequest request = new ClassifyRequest();
        request.setTitle("test");
        request.setDescription("test");

        assertThrows(BusinessException.class, () -> aiController.classify(request));
        verifyNoInteractions(aiService);
    }

    @Test
    void recommendWorker_RejectsStudent() {
        assertThrows(BusinessException.class, () -> aiController.recommendWorker(1L));
        verifyNoInteractions(aiService);
    }

    @Test
    void evaluateCompletion_RejectsNonAdmin() {
        assertThrows(BusinessException.class, () -> aiController.evaluateCompletion(1L));
        verifyNoInteractions(aiService);
    }

    @Test
    void insights_RejectsNonAdmin() {
        InsightRequest request = new InsightRequest();
        request.setTimeRange("today");

        assertThrows(BusinessException.class, () -> aiController.insights(request));
        verifyNoInteractions(aiService);
    }

    @Test
    void evaluateCompletion_ReturnsResponse() {
        AuthContext.setRole("ADMIN");
        EvaluationResponse mockResp = new EvaluationResponse();
        mockResp.setDecision("AUTO_ACCEPT");
        when(aiService.evaluateCompletion(1L)).thenReturn(mockResp);

        ApiResponse<EvaluationResponse> response = aiController.evaluateCompletion(1L);

        assertEquals(200, response.getCode());
        assertEquals("AUTO_ACCEPT", response.getData().getDecision());
    }

    @Test
    void insights_ReturnsResponse() {
        AuthContext.setRole("ADMIN");
        InsightResponse mockResp = new InsightResponse();
        mockResp.setSummary("summary");
        when(aiService.generateInsights(any(InsightRequest.class))).thenReturn(mockResp);

        InsightRequest request = new InsightRequest();
        request.setTimeRange("today");

        ApiResponse<InsightResponse> response = aiController.insights(request);

        assertEquals(200, response.getCode());
        assertEquals("summary", response.getData().getSummary());
    }
}
