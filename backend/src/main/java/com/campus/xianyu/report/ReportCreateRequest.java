package com.campus.xianyu.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @NotNull(message = "举报原因不能为空") Long reasonId,
        @NotBlank(message = "举报对象类型不能为空") String targetType,
        @NotNull(message = "举报对象ID不能为空") Long targetId,
        @Size(max = 500, message = "补充说明不能超过500字") String description
) {
}
