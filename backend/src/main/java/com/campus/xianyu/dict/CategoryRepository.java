package com.campus.xianyu.dict;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByStatusOrderBySortOrderAscIdAsc(String status);

    boolean existsByParentId(Long parentId);

    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM Category c WHERE c.status = :status")
    int findMaxSortOrderByStatus(@Param("status") String status);

    void deleteByStatus(String status);
}