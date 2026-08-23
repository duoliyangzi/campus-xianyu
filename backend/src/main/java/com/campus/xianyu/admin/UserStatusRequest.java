package com.campus.xianyu.admin;

import jakarta.validation.constraints.NotBlank;

public record UserStatusRequest(
        @NotBlank(message = "用户状态不能为空") String status
) {
}
