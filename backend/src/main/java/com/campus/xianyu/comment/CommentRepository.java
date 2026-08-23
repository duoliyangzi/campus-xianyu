package com.campus.xianyu.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);
}
