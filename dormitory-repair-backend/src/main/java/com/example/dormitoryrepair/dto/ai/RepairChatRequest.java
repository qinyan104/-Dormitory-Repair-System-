package com.example.dormitoryrepair.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RepairChatRequest {
    @NotBlank(message = "对话内容不能为空")
    private String message;
    private String phase;
    private String dormitoryBuilding;
    private String roomNo;
    private String problem;
    private String issueType;
    private String issueDetail;
    private String impact;
    private String extra;
}
