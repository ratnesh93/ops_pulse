package com.moveinsync.opspulse.repository;

import com.moveinsync.opspulse.domain.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, String> {
}
