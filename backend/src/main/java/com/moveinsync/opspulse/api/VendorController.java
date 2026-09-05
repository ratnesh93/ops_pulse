package com.moveinsync.opspulse.api;

import com.moveinsync.opspulse.api.dto.VendorSummaryDto;
import com.moveinsync.opspulse.benchmark.BenchmarkingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VendorController {

    private final BenchmarkingService benchmarkingService;

    public VendorController(BenchmarkingService benchmarkingService) {
        this.benchmarkingService = benchmarkingService;
    }

    @GetMapping("/vendors")
    public Map<String, Object> listVendors() {
        List<VendorSummaryDto> vendors = benchmarkingService.listAllVendorMetrics();
        long breachCount = vendors.stream().filter(VendorSummaryDto::isSlaBreach).count();
        return Map.of(
                "slaOtaPct", vendors.isEmpty() ? 90 : vendors.get(0).getSlaOtaPct(),
                "vendorCount", vendors.size(),
                "breachCount", breachCount,
                "vendors", vendors);
    }
}
