package com.depotiq.repositories;

import com.depotiq.models.ImportAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportAuditLogRepository extends JpaRepository<ImportAuditLog, Long> {
    List<ImportAuditLog> findAllByOrderByCreatedAtDesc();
}
