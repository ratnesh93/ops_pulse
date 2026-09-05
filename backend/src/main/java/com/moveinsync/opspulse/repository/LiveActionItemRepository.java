package com.moveinsync.opspulse.repository;

import com.moveinsync.opspulse.domain.LiveActionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveActionItemRepository extends JpaRepository<LiveActionItem, Long> {

    List<LiveActionItem> findByStatusOrderByCreatedAtDesc(String status);

    List<LiveActionItem> findTop20ByOrderByCreatedAtDesc();
}
