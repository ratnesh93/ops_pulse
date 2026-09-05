package com.moveinsync.opspulse.repository;

import com.moveinsync.opspulse.domain.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    long countByVendorId(String vendorId);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.vendorId = :vendorId AND t.onTime = true")
    long countOnTimeByVendor(@Param("vendorId") String vendorId);

    @Query("SELECT COALESCE(SUM(t.cost), 0) FROM Trip t WHERE t.vendorId = :vendorId")
    BigDecimal sumCostByVendor(@Param("vendorId") String vendorId);

    @Query("SELECT t.office, COUNT(t) FROM Trip t WHERE t.vendorId = :vendorId AND t.onTime = false GROUP BY t.office ORDER BY COUNT(t) DESC")
    List<Object[]> topDelayedOffices(@Param("vendorId") String vendorId);

    @Query("SELECT t.delayReason, COUNT(t) FROM Trip t WHERE t.vendorId = :vendorId AND t.onTime = false AND t.delayReason IS NOT NULL AND t.delayReason <> 'NODELAY' GROUP BY t.delayReason")
    List<Object[]> delayAttribution(@Param("vendorId") String vendorId);

    @Query(value = """
            SELECT vendor_id,
                   COUNT(*) AS trip_count,
                   SUM(CASE WHEN on_time THEN 1 ELSE 0 END) AS on_time_count,
                   COALESCE(SUM(cost), 0) AS total_cost
            FROM trips
            GROUP BY vendor_id
            ORDER BY trip_count DESC
            """, nativeQuery = true)
    List<Object[]> aggregateMetricsByVendor();
}
