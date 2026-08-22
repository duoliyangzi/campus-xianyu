package com.campus.xianyu.product;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdInOrderByProductIdAscSortOrderAscIdAsc(Collection<Long> productIds);

    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    void deleteByProductId(Long productId);
}
