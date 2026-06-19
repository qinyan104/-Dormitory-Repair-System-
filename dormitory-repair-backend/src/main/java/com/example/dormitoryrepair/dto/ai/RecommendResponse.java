package com.example.dormitoryrepair.dto.ai;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RecommendResponse {
    /** 是否自动指派 */
    private Boolean autoAssigned;
    /** 自动指派的工人ID */
    private Long assignedWorkerId;
    /** 工人排名列表 */
    private List<WorkerRanking> rankings;

    @Data
    public static class WorkerRanking {
        private Long workerId;
        private String workerName;
        private Integer matchScore;
        private Map<String, Integer> breakdown;
        private String reason;
    }
}
