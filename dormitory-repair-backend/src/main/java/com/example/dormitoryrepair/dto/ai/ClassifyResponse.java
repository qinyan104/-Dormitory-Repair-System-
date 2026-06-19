package com.example.dormitoryrepair.dto.ai;

import lombok.Data;

@Data
public class ClassifyResponse {
    private String category;
    private Long categoryId;
    private String categoryName;
    private String urgency;
    private Integer priorityScore;
    private Integer impactScope;
    private Double confidence;
    private String reason;
    private Boolean autoApplied;
}
