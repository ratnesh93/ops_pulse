package com.moveinsync.opspulse.repository;

import com.moveinsync.opspulse.domain.DelayRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DelayRecordRepository extends JpaRepository<DelayRecord, Long> {
}
