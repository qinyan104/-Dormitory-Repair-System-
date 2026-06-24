package com.example.dormitoryrepair.dto.ai;

import lombok.Data;

@Data
public class NaturalRepairResponse {
    private String building;
    private String room;
    private String repairType;
    private Long categoryId;
    private String categoryName;
    private String description;
    private String urgencyLevel;
    private Integer priorityScore;
    private String reason;
    private Double confidence;
}
