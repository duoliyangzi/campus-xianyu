package com.campus.xianyu.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotNull(message = "商品ID不能为空") Long productId,
        @NotBlank(message = "留言内容不能为空")
        @Size(max = 500, message = "留言内容不能超过500字") String content
) {
}
