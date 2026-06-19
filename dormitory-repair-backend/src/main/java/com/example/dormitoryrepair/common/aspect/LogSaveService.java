package com.example.dormitoryrepair.common.aspect;

import com.example.dormitoryrepair.entity.OperationLog;
import com.example.dormitoryrepair.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogSaveService {
    private final OperationLogService operationLogService;

    @Async
    public void save(OperationLog log) {
        operationLogService.saveAsync(log);
    }
}
