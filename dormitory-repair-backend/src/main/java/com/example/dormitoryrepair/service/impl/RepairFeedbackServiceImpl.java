package com.example.dormitoryrepair.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dormitoryrepair.entity.RepairFeedback;
import com.example.dormitoryrepair.mapper.RepairFeedbackMapper;
import com.example.dormitoryrepair.service.RepairFeedbackService;
import org.springframework.stereotype.Service;

@Service
public class RepairFeedbackServiceImpl extends ServiceImpl<RepairFeedbackMapper, RepairFeedback> implements RepairFeedbackService {
}
