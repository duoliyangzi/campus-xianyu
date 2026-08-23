package com.campus.xianyu.aiaudit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_audit_log")
public class AiAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "content_snap", nullable = false, columnDefinition = "TEXT")
    private String contentSnap;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel = "NONE";

    @Column(nullable = false, length = 20)
    private String suggestion = "REVIEW";

    @Column(length = 500)
    private String reason;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
