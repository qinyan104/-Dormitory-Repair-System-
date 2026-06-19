package com.example.dormitoryrepair.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
