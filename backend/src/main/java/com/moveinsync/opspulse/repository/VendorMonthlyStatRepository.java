package com.moveinsync.opspulse.repository;

import com.moveinsync.opspulse.domain.VendorMonthlyStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorMonthlyStatRepository extends JpaRepository<VendorMonthlyStat, VendorMonthlyStat.VendorMonthKey> {

    Optional<VendorMonthlyStat> findByVendorIdAndMonthYear(String vendorId, String monthYear);
}
