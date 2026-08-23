package com.campus.xianyu.product;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<Product> findBySellerIdAndStatusNotOrderByCreatedAtDesc(Long sellerId, String status);

    boolean existsByCategoryId(Long categoryId);
}
