package com.campus.xianyu.wanted;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record WantedRequest(
        @NotBlank(message = "求购物品名称不能为空")
        @Size(max = 100, message = "求购物品名称不能超过100字")
        String itemName,

        @NotNull(message = "预算不能为空")
        @DecimalMin(value = "0.01", message = "预算必须大于0")
        BigDecimal budget,

        @NotBlank(message = "期望成色不能为空")
        String expectCondition,

        @Size(max = 500, message = "求购描述不能超过500字")
        String description,

        Long campusId
) {
}
