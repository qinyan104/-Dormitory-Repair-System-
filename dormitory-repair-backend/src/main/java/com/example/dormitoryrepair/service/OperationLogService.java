package com.example.dormitoryrepair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dormitoryrepair.entity.OperationLog;

public interface OperationLogService extends IService<OperationLog> {

    void saveAsync(OperationLog log);
}
