package com.campus.xianyu.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiChatRequest(
        @NotBlank(message = "消息不能为空")
        @Size(max = 2000, message = "消息不能超过2000字")
        String message,
        String mode,
        List<AiChatHistoryItem> history
) {
}
