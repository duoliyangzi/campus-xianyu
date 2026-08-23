package com.campus.xianyu.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConversationCreateRequest(
        @NotNull(message = "对方用户ID不能为空") Long peerUserId,
        Long productId,
        Long wantedId
) {
}
