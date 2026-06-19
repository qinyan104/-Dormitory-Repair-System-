package com.example.dormitoryrepair.dto.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepairFeedbackCreateRequest {

    @NotNull(message = "报修单不能为空")
    private Long repairOrderId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最少为1分")
    @Max(value = 5, message = "评分最多为5分")
    private Integer score;

    private String content;
}
