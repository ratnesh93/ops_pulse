package com.moveinsync.opspulse.repository;

import com.moveinsync.opspulse.domain.AppMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppMetadataRepository extends JpaRepository<AppMetadata, String> {
}
