package com.campus.xianyu.ai;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {
    Optional<AiChatSession> findFirstByUserIdOrderByIdAsc(Long userId);

    List<AiChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
