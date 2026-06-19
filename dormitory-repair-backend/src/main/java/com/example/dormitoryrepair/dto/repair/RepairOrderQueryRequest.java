package com.example.dormitoryrepair.dto.repair;

import lombok.Data;

@Data
public class RepairOrderQueryRequest {

    private Long categoryId;
    private Integer repairStatus;
    private String keyword;
    private String submitTime;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
