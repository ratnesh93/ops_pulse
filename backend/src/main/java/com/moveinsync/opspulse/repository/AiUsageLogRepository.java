package com.moveinsync.opspulse.repository;

import com.moveinsync.opspulse.domain.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    List<AiUsageLog> findTop20ByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(a.cost), 0) FROM AiUsageLog a")
    BigDecimal sumTotalCost();

    @Query("SELECT COALESCE(SUM(a.inputTokens), 0) FROM AiUsageLog a")
    long sumInputTokens();

    @Query("SELECT COALESCE(SUM(a.outputTokens), 0) FROM AiUsageLog a")
    long sumOutputTokens();

    @Query("SELECT COALESCE(SUM(a.cost), 0) FROM AiUsageLog a WHERE a.createdAt >= :since")
    BigDecimal sumCostSince(@Param("since") Instant since);

    @Query("SELECT COUNT(a) FROM AiUsageLog a WHERE a.createdAt >= :since")
    long countSince(@Param("since") Instant since);

    @Query("""
            SELECT a.operationType,
                   COUNT(a),
                   COALESCE(SUM(a.inputTokens), 0),
                   COALESCE(SUM(a.outputTokens), 0),
                   COALESCE(SUM(a.cost), 0)
            FROM AiUsageLog a
            GROUP BY a.operationType
            ORDER BY a.operationType
            """)
    List<Object[]> aggregateByOperationType();

    @Query("""
            SELECT a.provider,
                   COUNT(a),
                   COALESCE(SUM(a.inputTokens), 0),
                   COALESCE(SUM(a.outputTokens), 0),
                   COALESCE(SUM(a.cost), 0)
            FROM AiUsageLog a
            GROUP BY a.provider
            ORDER BY a.provider
            """)
    List<Object[]> aggregateByProvider();

    @Query("""
            SELECT a.provider,
                   COUNT(a),
                   COALESCE(SUM(a.inputTokens), 0),
                   COALESCE(SUM(a.outputTokens), 0),
                   COALESCE(SUM(a.cost), 0)
            FROM AiUsageLog a
            WHERE a.createdAt >= :since
            GROUP BY a.provider
            ORDER BY SUM(a.cost) DESC
            """)
    List<Object[]> aggregateByProviderSince(@Param("since") Instant since);
}
