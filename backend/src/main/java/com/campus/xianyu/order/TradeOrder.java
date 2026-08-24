package com.campus.xianyu.order;

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
@Table(name = "trade_order")
public class TradeOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 32)
    private String orderNo;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false, length = 20)
    private String status = "PENDING_CHAT";

    @Column(name = "meet_time")
    private LocalDateTime meetTime;

    @Column(name = "meet_location", length = 200)
    private String meetLocation;

    @Column(length = 255)
    private String remark;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "buyer_confirmed", nullable = false)
    private Boolean buyerConfirmed = false;

    @Column(name = "seller_confirmed", nullable = false)
    private Boolean sellerConfirmed = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
