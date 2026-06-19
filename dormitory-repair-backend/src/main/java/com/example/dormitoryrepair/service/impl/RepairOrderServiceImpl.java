package com.example.dormitoryrepair.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.mapper.RepairOrderMapper;
import com.example.dormitoryrepair.service.RepairOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class RepairOrderServiceImpl extends ServiceImpl<RepairOrderMapper, RepairOrder> implements RepairOrderService {

    private static final Map<Integer, Set<Integer>> VALID_TRANSITIONS = Map.of(
            1, Set.of(2, 6),
            2, Set.of(3, 6),
            3, Set.of(4, 6),
            4, Set.of(5, 6),
            5, Set.of(),
            6, Set.of()
    );

    @Override
    @Transactional
    public boolean updateStatusWithLock(RepairOrder order, int targetStatus) {
        Set<Integer> allowed = VALID_TRANSITIONS.get(order.getRepairStatus());
        if (allowed == null || !allowed.contains(targetStatus)) {
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
        Set<Integer> allowed = VALID_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }
}
