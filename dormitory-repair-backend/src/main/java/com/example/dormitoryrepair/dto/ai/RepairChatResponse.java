package com.example.dormitoryrepair.dto.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RepairChatResponse {
    private Boolean repairIntent;
    private Boolean readyToSummarize;
    private String reply;
    private String issueType;
    private String issueName;
    private String issueDetail;
    private String impact;
    private String extra;
    private String summary;
    private Double confidence;
    private List<Choice> choices = new ArrayList<>();

    @Data
    public static class Choice {
        private String label;
        private String value;
        private String action;
        private String hint;
    }
}
