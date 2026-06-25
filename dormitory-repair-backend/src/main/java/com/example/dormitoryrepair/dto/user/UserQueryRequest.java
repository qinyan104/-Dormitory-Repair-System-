package com.example.dormitoryrepair.dto.user;

import lombok.Data;

@Data
public class UserQueryRequest {

    private String username;
    private String realName;
    private String studentNo;
    private Integer status;
    private String role;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
