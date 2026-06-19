package com.example.dormitoryrepair.dto.notice;

import lombok.Data;

@Data
public class NoticeQueryRequest {

    private String title;
    private String type;
    private Integer status;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
