package com.moveinsync.opspulse.api;

import com.moveinsync.opspulse.api.dto.FacilitiesSummaryDto;
import com.moveinsync.opspulse.benchmark.BenchmarkingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facilities")
public class FacilitiesController {

    private final BenchmarkingService benchmarkingService;

    public FacilitiesController(BenchmarkingService benchmarkingService) {
        this.benchmarkingService = benchmarkingService;
    }

    @GetMapping("/summary")
    public FacilitiesSummaryDto summary() {
        return benchmarkingService.buildFacilitiesSummary();
    }
}
