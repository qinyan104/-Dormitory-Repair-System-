package com.example.dormitoryrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.entity.RepairFeedback;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.entity.SysUser;
import com.example.dormitoryrepair.mapper.RepairFeedbackMapper;
import com.example.dormitoryrepair.mapper.RepairOrderMapper;
import com.example.dormitoryrepair.mapper.SysUserMapper;
import com.example.dormitoryrepair.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final RepairOrderMapper repairOrderMapper;
    private final RepairFeedbackMapper repairFeedbackMapper;

    @Override
    public boolean removeById(Serializable id) {
        // Check if user has associated repair orders (as creator or handler)
        boolean hasOrders = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getUserId, id)
                .or()
                .eq(RepairOrder::getHandlerId, id)) > 0;
        if (hasOrders) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该用户有关联的报修单，无法删除");
        }

        // Check if user has associated feedbacks
        boolean hasFeedbacks = repairFeedbackMapper.selectCount(new LambdaQueryWrapper<RepairFeedback>()
                .eq(RepairFeedback::getUserId, id)) > 0;
        if (hasFeedbacks) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该用户有关联的评价记录，无法删除");
        }

        return super.removeById(id);
    }
}
