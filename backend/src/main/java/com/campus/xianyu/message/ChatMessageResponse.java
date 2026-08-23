package com.campus.xianyu.message;

import com.campus.xianyu.user.PublicUserResponse;
import java.time.format.DateTimeFormatter;

public record ChatMessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String content,
        boolean read,
        String createdAt,
        PublicUserResponse sender
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ChatMessageResponse from(ChatMessage message, PublicUserResponse sender) {
        return new ChatMessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getContent(),
                Boolean.TRUE.equals(message.getIsRead()),
                message.getCreatedAt() == null ? null : message.getCreatedAt().format(FORMATTER),
                sender
        );
    }
}
