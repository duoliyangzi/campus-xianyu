package com.campus.xianyu.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank(message = "商品标题不能为空")
        @Size(max = 100, message = "商品标题不能超过100字")
        String title,

        @NotNull(message = "商品价格不能为空")
        @DecimalMin(value = "0.01", message = "商品价格必须大于0")
        BigDecimal price,

        @NotNull(message = "商品分类不能为空")
        Long categoryId,

        @NotBlank(message = "新旧程度不能为空")
        String conditionLevel,

        @NotNull(message = "校区不能为空")
        Long campusId,

        @NotBlank(message = "交易方式不能为空")
        String tradeMethod,

        @NotBlank(message = "图文描述不能为空")
        String description,

        String coverUrl,

        List<String> imageUrls
) {
}
