package com.example.dormitoryrepair.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dormitoryrepair.entity.OperationLog;
import com.example.dormitoryrepair.mapper.OperationLogMapper;
import com.example.dormitoryrepair.service.OperationLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    @Async
    public void saveAsync(OperationLog log) {
        save(log);
    }
}
