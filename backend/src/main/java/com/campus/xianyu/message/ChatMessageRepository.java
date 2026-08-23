package com.campus.xianyu.message;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    List<ChatMessage> findByConversationIdInOrderByCreatedAtAsc(List<Long> conversationIds);

    boolean existsByConversationId(Long conversationId);

    long countByConversationIdAndIsReadFalseAndSenderIdNot(Long conversationId, Long senderId);
}
