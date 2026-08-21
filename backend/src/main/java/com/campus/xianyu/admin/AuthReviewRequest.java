package com.campus.xianyu.admin;

import jakarta.validation.constraints.NotBlank;

public record AuthReviewRequest(
        @NotBlank(message = "审核状态不能为空")
        String authStatus,

        String authRemark
) {
}