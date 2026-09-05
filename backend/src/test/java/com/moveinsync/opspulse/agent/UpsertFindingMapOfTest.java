package com.moveinsync.opspulse.agent;

import com.moveinsync.opspulse.benchmark.VendorBenchmark;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpsertFindingMapOfTest {

    @Test
    void benchmarkJsonMapOfThrowsWhenPriorMonthOtaPctIsNull() {
        VendorBenchmark benchmark = vendorWithoutPriorMonthStats();

        assertThrows(NullPointerException.class, () -> Map.of(
                "slaOtaPct", benchmark.getSlaOtaPct(),
                "priorMonthOtaPct", benchmark.getPriorMonthOtaPct(),
                "peerOtaPct", benchmark.getPeerOtaPct(),
                "peerVendorName", benchmark.getPeerVendorName(),
                "delayAttribution", benchmark.getDelayAttribution()));
    }

    @Test
    void hashMapAllowsNullPriorMonthOtaPct() {
        VendorBenchmark benchmark = vendorWithoutPriorMonthStats();

        assertDoesNotThrow(() -> {
            Map<String, Object> benchmarkData = new LinkedHashMap<>();
            benchmarkData.put("slaOtaPct", benchmark.getSlaOtaPct());
            benchmarkData.put("priorMonthOtaPct", benchmark.getPriorMonthOtaPct());
            benchmarkData.put("peerOtaPct", benchmark.getPeerOtaPct());
            benchmarkData.put("peerVendorName", benchmark.getPeerVendorName());
            benchmarkData.put("delayAttribution", benchmark.getDelayAttribution());
            assertNull(benchmarkData.get("priorMonthOtaPct"));
        });
    }

    private static VendorBenchmark vendorWithoutPriorMonthStats() {
        VendorBenchmark benchmark = new VendorBenchmark();
        benchmark.setVendorId("Rohan Mikhailov Travel");
        benchmark.setVendorDisplayName("Rohan Travel (Vendor B)");
        benchmark.setOtaPct(0.0);
        benchmark.setSlaOtaPct(90.0);
        benchmark.setTripCount(0);
        benchmark.setTotalCost(BigDecimal.ZERO);
        benchmark.setPeerOtaPct(85.0);
        benchmark.setPeerVendorName("Priya Travel");
        benchmark.setDelayAttribution(new LinkedHashMap<>());
        return benchmark;
    }
}
