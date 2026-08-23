package com.depotiq.repositories;

import com.depotiq.models.OperationalActivityLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationalActivityLogRepository extends JpaRepository<OperationalActivityLog, Long> {
    List<OperationalActivityLog> findTop100ByOrderByCreatedAtDesc();
}
