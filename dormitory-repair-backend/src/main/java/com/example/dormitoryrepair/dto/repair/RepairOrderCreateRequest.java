package com.example.dormitoryrepair.dto.repair;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepairOrderCreateRequest {

    @NotNull(message = "报修分类不能为空")
    private Long categoryId;

    @NotBlank(message = "报修标题不能为空")
    private String title;

    @NotBlank(message = "报修内容不能为空")
    private String content;

    private String imageUrl;

    private String urgency;

    /** AI 优先级评分（前端可选传入） */
    private Integer aiPriorityScore;

    /** AI 影响范围（前端可选传入） */
    private Integer aiImpactScope;
}
