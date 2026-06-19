package com.example.dormitoryrepair.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassifyRequest {
    @NotBlank(message = "报修标题不能为空")
    private String title;

    @NotBlank(message = "报修描述不能为空")
    private String description;
}
