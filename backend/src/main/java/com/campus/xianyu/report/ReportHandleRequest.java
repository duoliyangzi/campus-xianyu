package com.campus.xianyu.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportHandleRequest(
        @NotBlank(message = "处理状态不能为空") String status,
        @Size(max = 255, message = "处理结果不能超过255字") String handleResult
) {
}
