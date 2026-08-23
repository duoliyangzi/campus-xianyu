package com.campus.xianyu.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByStudentNo(String studentNo);

    boolean existsByUsername(String username);

    List<AppUser> findByRoleAndAuthStatusOrderByUpdatedAtDesc(String role, String authStatus);

    @Query("""
            SELECT u FROM AppUser u
            WHERE u.role = 'STUDENT'
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR u.studentNo LIKE CONCAT('%', :keyword, '%'))
            ORDER BY u.createdAt DESC
            """)
    Page<AppUser> searchStudents(@Param("keyword") String keyword, Pageable pageable);
}