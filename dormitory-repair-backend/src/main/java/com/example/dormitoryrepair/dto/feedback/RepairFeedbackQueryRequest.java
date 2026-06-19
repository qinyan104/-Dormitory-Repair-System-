package com.example.dormitoryrepair.dto.feedback;

import lombok.Data;

@Data
public class RepairFeedbackQueryRequest {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
