package com.campus.xianyu.aiaudit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAuditLogRepository extends JpaRepository<AiAuditLog, Long> {
    List<AiAuditLog> findByProductIdOrderByCreatedAtDesc(Long productId);
}
