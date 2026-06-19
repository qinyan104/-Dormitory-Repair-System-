package com.example.dormitoryrepair.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class InsightRequest {
    @NotBlank
    private String timeRange;
    private List<String> dimensions;
}
