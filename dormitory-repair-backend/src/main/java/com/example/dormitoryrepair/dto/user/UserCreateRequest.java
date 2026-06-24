package com.example.dormitoryrepair.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    private String studentNo;
    private String phone;
    private Integer gender;
    private String dormitoryBuilding;
    private String roomNo;
    @Pattern(regexp = "^(STUDENT|ADMIN|REPAIRER)$", message = "角色只能是 STUDENT、ADMIN 或 REPAIRER")
    private String role;
    private String skillType;
    private String serviceArea;
}
