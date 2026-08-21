package com.campus.xianyu.user;

import jakarta.validation.constraints.NotBlank;

public record StudentAuthRequest(
        @NotBlank(message = "学号不能为空") String studentNo,
        @NotBlank(message = "学院不能为空") String college
) {
}
