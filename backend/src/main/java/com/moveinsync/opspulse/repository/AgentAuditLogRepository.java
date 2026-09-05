package com.moveinsync.opspulse.repository;

import com.moveinsync.opspulse.domain.AgentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentAuditLogRepository extends JpaRepository<AgentAuditLog, Long> {

    List<AgentAuditLog> findByRunIdOrderByTimestampAsc(UUID runId);

    List<AgentAuditLog> findAllByOrderByTimestampDesc();
}
