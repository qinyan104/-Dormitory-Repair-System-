package com.example.dormitoryrepair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;
    private String operatorName;
    private String operationType;
    private String description;
    private String methodName;
    private String requestParams;
    private String ipAddress;
    private Long executionTime;
    private Integer status;
    private String errorMessage;
    private LocalDateTime createTime;
}
