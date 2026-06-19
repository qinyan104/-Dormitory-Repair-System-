package com.example.dormitoryrepair.dto.log;

import lombok.Data;

@Data
public class OperationLogQueryRequest {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String operationType;
    private Integer status;
    private String keyword;
}
