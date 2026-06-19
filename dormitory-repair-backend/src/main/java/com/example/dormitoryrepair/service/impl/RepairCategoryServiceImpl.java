package com.example.dormitoryrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dormitoryrepair.common.exception.BusinessException;
import com.example.dormitoryrepair.common.result.ResultCode;
import com.example.dormitoryrepair.entity.RepairCategory;
import com.example.dormitoryrepair.entity.RepairOrder;
import com.example.dormitoryrepair.mapper.RepairCategoryMapper;
import com.example.dormitoryrepair.mapper.RepairOrderMapper;
import com.example.dormitoryrepair.service.RepairCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
@RequiredArgsConstructor
public class RepairCategoryServiceImpl extends ServiceImpl<RepairCategoryMapper, RepairCategory> implements RepairCategoryService {

    private final RepairOrderMapper repairOrderMapper;

    @Override
    public boolean removeById(Serializable id) {
        // Check if category has associated repair orders
        boolean hasOrders = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getCategoryId, id)) > 0;
        if (hasOrders) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "该分类下有关联的报修单，无法删除");
        }

        return super.removeById(id);
    }
}
