package com.example.dormitoryrepair.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dormitoryrepair.entity.Notice;
import com.example.dormitoryrepair.mapper.NoticeMapper;
import com.example.dormitoryrepair.service.NoticeService;
import org.springframework.stereotype.Service;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {
}
