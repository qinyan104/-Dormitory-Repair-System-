package com.example.dormitoryrepair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dormitoryrepair.entity.RepairOrder;

public interface RepairOrderService extends IService<RepairOrder> {

    /**
     * Update order status with optimistic lock and transition validation.
     * Returns true on success; throws OptimisticLockException on concurrent conflict.
     */
    boolean updateStatusWithLock(RepairOrder order, int targetStatus);

    /**
     * Check if a status transition is valid.
     */
    boolean validateTransition(int currentStatus, int targetStatus);
}
