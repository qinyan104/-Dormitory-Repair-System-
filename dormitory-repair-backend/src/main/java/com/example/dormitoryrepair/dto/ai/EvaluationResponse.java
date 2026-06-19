package com.example.dormitoryrepair.dto.ai;

import lombok.Data;
import java.util.List;

@Data
public class EvaluationResponse {
    private Integer riskScore;
    private String riskLevel;
    private String decision;
    private List<RiskFactor> riskFactors;
    private String suggestion;

    @Data
    public static class RiskFactor {
        private String signal;
        private Integer score;
        private String detail;
    }
}
