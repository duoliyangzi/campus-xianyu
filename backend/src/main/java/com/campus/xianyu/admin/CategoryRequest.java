package com.campus.xianyu.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        Long parentId,
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 50, message = "分类名称不能超过50字") String name,
        Integer sortOrder
) {
}
