package com.example.dormitoryrepair.dto.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRepairerProfileRequest {

    @Size(max = 200, message = "维修技能不能超过200个字符")
    private String skillType;

    @Size(max = 200, message = "负责区域不能超过200个字符")
    private String serviceArea;
}
