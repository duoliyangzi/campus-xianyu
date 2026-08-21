package com.campus.xianyu.product;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
}