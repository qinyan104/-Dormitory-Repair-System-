package com.example.dormitoryrepair.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String phone;
    private Integer gender;
    private String dormitoryBuilding;
    private String roomNo;
    private String avatar;
}
