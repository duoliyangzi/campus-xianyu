package com.campus.xianyu.dict;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusRepository extends JpaRepository<Campus, Long> {
    List<Campus> findByStatusOrderBySortOrderAscIdAsc(String status);
}