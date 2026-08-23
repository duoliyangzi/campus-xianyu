package com.campus.xianyu.message;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByUserAIdAndUserBIdAndProductId(Long userAId, Long userBId, Long productId);

    Optional<Conversation> findByUserAIdAndUserBIdAndWantedId(Long userAId, Long userBId, Long wantedId);

    List<Conversation> findByUserAIdAndUserBId(Long userAId, Long userBId);

    List<Conversation> findByUserAIdOrUserBIdOrderByLastMsgAtDescCreatedAtDesc(Long userAId, Long userBId);
}
