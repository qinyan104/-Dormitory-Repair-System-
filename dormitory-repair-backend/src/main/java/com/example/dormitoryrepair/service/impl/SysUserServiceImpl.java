package com.example.dormitoryrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.entity.SysUser;
import com.example.dormitoryrepair.mapper.RepairOrderMapper;
import com.example.dormitoryrepair.mapper.SysUserMapper;
import com.example.dormitoryrepair.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final RepairOrderMapper repairOrderMapper;

    /**
     * Soft-delete a user by setting status = 0 (disabled).
     * Physical deletion is not performed to preserve referential integrity
     * with repair orders and feedback records.
     */
    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        SysUser user = getById(id);
        if (user == null) {
            return false;
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("User {} is already disabled, skipping soft-delete", id);
            return true;
        }

        // Check if user has active (in-progress) repair orders
        boolean hasActiveOrders = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getUserId, id)
                .in(RepairOrder::getRepairStatus, 1, 2, 3, 4)) > 0;
        if (hasActiveOrders) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "该用户有进行中的报修单，无法禁用。请先处理或关闭相关工单");
        }

        user.setStatus(0);
        boolean updated = updateById(user);
        if (updated) {
            log.info("Soft-deleted user id={}, username={}", id, user.getUsername());
        }
        return updated;
    }
}
