package com.example.dormitoryrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dormitoryrepair.entity.RepairOrder;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrder> {

    List<Map<String, Object>> countGroupByCategory();

    List<Map<String, Object>> countGroupByStatus();
}
