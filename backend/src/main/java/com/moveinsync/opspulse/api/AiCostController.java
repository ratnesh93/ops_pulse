package com.moveinsync.opspulse.api;

import com.moveinsync.opspulse.ai.AiCostService;
import com.moveinsync.opspulse.api.dto.AiCostSummaryDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiCostController {

    private final AiCostService aiCostService;

    public AiCostController(AiCostService aiCostService) {
        this.aiCostService = aiCostService;
    }

    @GetMapping("/costs")
    public AiCostSummaryDto getCosts() {
        return aiCostService.getSummary();
    }
}
