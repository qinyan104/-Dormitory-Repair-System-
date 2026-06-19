package com.example.dormitoryrepair.dto.category;

import lombok.Data;

@Data
public class CategoryQueryRequest {

    private String categoryName;
    private Integer status;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
