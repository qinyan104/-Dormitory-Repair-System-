package com.example.dormitoryrepair.dto.notice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NoticeSaveRequest {

    private Long id;

    @NotBlank(message = "公告标题不能为空")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    private String type;

    @NotNull(message = "是否置顶不能为空")
    private Integer isTop = 0;

    @NotNull(message = "状态不能为空")
    private Integer status = 1;
}
