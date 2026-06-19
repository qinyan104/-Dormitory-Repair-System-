package com.example.dormitoryrepair.dto.repair;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepairOrderStatusRequest {

    @NotNull(message = "状态不能为空")
    private Integer repairStatus;

    private String remark;
}
