package com.campus.xianyu.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, message = "密码不少于8个字符")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "密码至少包含大写字母、小写字母和数字")
        String password,
        String nickname,
        String phone
) {
}
