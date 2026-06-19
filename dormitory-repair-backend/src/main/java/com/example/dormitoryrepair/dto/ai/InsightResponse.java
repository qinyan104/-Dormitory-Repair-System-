package com.example.dormitoryrepair.dto.ai;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class InsightResponse {
    private String summary;
    private List<Highlight> highlights;
    private Map<String, Object> charts;

    @Data
    public static class Highlight {
        private String type;
        private String severity;
        private String title;
        private String detail;
        private List<Long> relatedOrderIds;
    }
}
