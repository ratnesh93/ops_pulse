package com.moveinsync.opspulse.repository;

import com.moveinsync.opspulse.domain.AgentAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentActionRepository extends JpaRepository<AgentAction, Long> {

    List<AgentAction> findByStatusOrderByCreatedAtDesc(String status);

    List<AgentAction> findByActionTypeAndStatus(String actionType, String status);

    List<AgentAction> findTop20ByOrderByCreatedAtDesc();
}
