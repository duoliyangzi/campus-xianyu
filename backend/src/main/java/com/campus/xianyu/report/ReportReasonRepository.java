package com.campus.xianyu.report;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportReasonRepository extends JpaRepository<ReportReason, Long> {
    List<ReportReason> findByStatusOrderBySortOrderAscIdAsc(String status);
}
