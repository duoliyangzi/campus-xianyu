package com.campus.xianyu.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductAuditRequest(
        @NotBlank(message = "审核状态不能为空") String status,
        @Size(max = 255, message = "审核备注不能超过255字") String auditRemark
) {
}
