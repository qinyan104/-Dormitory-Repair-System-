package com.example.dormitoryrepair.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategorySaveRequest {

    private Long id;

    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    private String description;

    @NotNull(message = "排序号不能为空")
    private Integer sortNum;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
