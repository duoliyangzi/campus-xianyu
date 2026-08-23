package com.campus.xianyu.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record OrderCreateRequest(
        @NotNull(message = "商品ID不能为空") Long productId,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime meetTime,
        @Size(max = 200, message = "约定地点不能超过200字") String meetLocation,
        @Size(max = 255, message = "备注不能超过255字") String remark,
        Long conversationId
) {
}
