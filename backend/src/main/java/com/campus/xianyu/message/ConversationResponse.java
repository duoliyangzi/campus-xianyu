package com.campus.xianyu.message;

import com.campus.xianyu.user.PublicUserResponse;
import java.time.format.DateTimeFormatter;

public record ConversationResponse(
        Long id,
        Long peerUserId,
        PublicUserResponse peerUser,
        Long productId,
        Long wantedId,
        String lastMessage,
        String lastMsgAt,
        String createdAt,
        int unreadCount
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ConversationResponse from(
            Conversation conversation,
            Long currentUserId,
            PublicUserResponse peerUser,
            String lastMessage,
            int unreadCount
    ) {
        Long peerUserId = conversation.getUserAId().equals(currentUserId)
                ? conversation.getUserBId()
                : conversation.getUserAId();
        return new ConversationResponse(
                conversation.getId(),
                peerUserId,
                peerUser,
                conversation.getProductId(),
                conversation.getWantedId(),
                lastMessage,
                conversation.getLastMsgAt() == null ? null : conversation.getLastMsgAt().format(FORMATTER),
                conversation.getCreatedAt() == null ? null : conversation.getCreatedAt().format(FORMATTER),
                unreadCount
        );
    }
}
