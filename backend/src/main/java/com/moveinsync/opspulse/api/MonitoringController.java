package com.moveinsync.opspulse.api;

import com.moveinsync.opspulse.api.dto.MonitoringDashboardDto;
import com.moveinsync.opspulse.api.dto.MonitoringScenarioDto;
import com.moveinsync.opspulse.domain.LiveActionItem;
import com.moveinsync.opspulse.monitoring.MonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping
    public MonitoringDashboardDto dashboard() {
        return monitoringService.getDashboard();
    }

    @GetMapping("/scenarios")
    public List<MonitoringScenarioDto> scenarios() {
        return monitoringService.listScenarioOptions();
    }

    @PostMapping("/simulate/{scenario}")
    public MonitoringDashboardDto simulate(
            @PathVariable String scenario,
            @RequestParam(defaultValue = "rohan") String vendor) {
        return monitoringService.simulateFeed(scenario, vendor);
    }

    @PostMapping("/actions/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable Long id) {
        LiveActionItem item = monitoringService.confirmAction(id);
        return Map.of("id", item.getId(), "status", item.getStatus(), "confirmedAt", item.getConfirmedAt());
    }

    @PostMapping("/actions/{id}/dismiss")
    public Map<String, Object> dismiss(@PathVariable Long id) {
        LiveActionItem item = monitoringService.dismissAction(id);
        return Map.of("id", item.getId(), "status", item.getStatus());
    }
}
