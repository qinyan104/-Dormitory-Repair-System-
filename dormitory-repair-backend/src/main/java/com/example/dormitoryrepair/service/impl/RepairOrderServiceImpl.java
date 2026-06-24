package com.example.dormitoryrepair.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dormitoryrepair.common.enums.RepairStatusEnum;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.mapper.RepairOrderMapper;
import com.example.dormitoryrepair.service.RepairOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class RepairOrderServiceImpl extends ServiceImpl<RepairOrderMapper, RepairOrder> implements RepairOrderService {

    @Override
    @Transactional
    public boolean updateStatusWithLock(RepairOrder order, int targetStatus) {
        if (!RepairStatusEnum.isValidTransition(order.getRepairStatus(), targetStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前状态不允许更新为目标状态");
        }
        order.setRepairStatus(targetStatus);

        if (Objects.equals(targetStatus, 3)) {
            order.setHandleTime(LocalDateTime.now());
        }
        if (Objects.equals(targetStatus, 5)) {
            order.setFinishTime(LocalDateTime.now());
        }
        if (Objects.equals(targetStatus, 6)) {
            order.setCancelTime(LocalDateTime.now());
        }

        // updateById with @Version will throw OptimisticLockException on conflict
        return updateById(order);
    }

    @Override
    public boolean validateTransition(int currentStatus, int targetStatus) {
        return RepairStatusEnum.isValidTransition(currentStatus, targetStatus);
    }
}
