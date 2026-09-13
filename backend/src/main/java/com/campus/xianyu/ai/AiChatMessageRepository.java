package com.campus.xianyu.ai;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    List<AiChatMessage> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);

    List<AiChatMessage> findTop80BySessionIdOrderByCreatedAtDescIdDesc(Long sessionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AiChatMessage m where m.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") Long sessionId);
}
