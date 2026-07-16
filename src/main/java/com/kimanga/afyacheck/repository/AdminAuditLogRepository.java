package com.kimanga.afyacheck.repository;

import com.kimanga.afyacheck.model.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
}
