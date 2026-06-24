package com.example.dormitoryrepair.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NaturalRepairRequest {
    @NotBlank(message = "报修描述不能为空")
    private String text;
}
