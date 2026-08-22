package com.campus.xianyu.wanted;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WantedRepository extends JpaRepository<Wanted, Long>, JpaSpecificationExecutor<Wanted> {
    List<Wanted> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
}
