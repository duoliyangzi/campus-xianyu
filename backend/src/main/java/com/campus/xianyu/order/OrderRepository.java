package com.campus.xianyu.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<TradeOrder, Long> {
    List<TradeOrder> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(Long buyerId, Long sellerId);

    boolean existsByBuyerIdAndProductIdAndStatusIn(Long buyerId, Long productId, Iterable<String> statuses);
}
