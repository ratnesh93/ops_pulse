package com.moveinsync.opspulse.repository;

import com.moveinsync.opspulse.domain.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    List<Finding> findByStatusOrderByCreatedAtDesc(String status);

    Optional<Finding> findFirstByTypeAndStatusOrderByCreatedAtDesc(String type, String status);
}
