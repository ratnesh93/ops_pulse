package com.moveinsync.opspulse.repository;

import com.moveinsync.opspulse.domain.LiveFeedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveFeedEventRepository extends JpaRepository<LiveFeedEvent, Long> {

    List<LiveFeedEvent> findTop30ByOrderByCreatedAtDesc();
}
