package com.moveinsync.opspulse.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.opspulse.api.dto.BriefResponse;
import com.moveinsync.opspulse.benchmark.BenchmarkingService;
import com.moveinsync.opspulse.benchmark.VendorBenchmark;
import com.moveinsync.opspulse.domain.AgentAction;
import com.moveinsync.opspulse.domain.Finding;
import com.moveinsync.opspulse.repository.AgentActionRepository;
import com.moveinsync.opspulse.repository.FindingRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class BriefController {

    private final BenchmarkingService benchmarkingService;
    private final FindingRepository findingRepository;
    private final AgentActionRepository agentActionRepository;
    private final ObjectMapper objectMapper;

    public BriefController(
            BenchmarkingService benchmarkingService,
            FindingRepository findingRepository,
            AgentActionRepository agentActionRepository,
            ObjectMapper objectMapper) {
        this.benchmarkingService = benchmarkingService;
        this.findingRepository = findingRepository;
        this.agentActionRepository = agentActionRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/brief")
    public BriefResponse getBrief() {
        VendorBenchmark benchmark = benchmarkingService.benchmarkFocusVendor();

        BriefResponse response = new BriefResponse();
        BriefResponse.KpiBar kpis = new BriefResponse.KpiBar();
        kpis.setOtaPct(benchmark.getOtaPct());
        kpis.setSlaOtaPct(benchmark.getSlaOtaPct());
        kpis.setPriorMonthOtaPct(benchmark.getPriorMonthOtaPct());
        if (benchmark.getPriorMonthOtaPct() != null) {
            kpis.setOtaDeltaVsPriorMonth(benchmark.getOtaPct() - benchmark.getPriorMonthOtaPct());
        }
        kpis.setTotalCost(benchmark.getTotalCost());
        kpis.setTripCount(benchmark.getTripCount());
        kpis.setVendorDisplayName(benchmark.getVendorDisplayName());
        response.setKpis(kpis);

        response.setFindings(findingRepository.findByStatusOrderByCreatedAtDesc("ACTIVE")
                .stream()
                .map(this::toFindingDto)
                .collect(Collectors.toList()));

        response.setPendingActions(agentActionRepository.findByStatusOrderByCreatedAtDesc("PENDING")
                .stream()
                .map(this::toActionDto)
                .collect(Collectors.toList()));

        response.setMorningBrief(benchmarkingService.buildMorningBrief(
                response.getFindings().size(),
                response.getPendingActions().size()));

        return response;
    }

    private BriefResponse.FindingDto toFindingDto(Finding finding) {
        BriefResponse.FindingDto dto = new BriefResponse.FindingDto();
        dto.setId(finding.getId());
        dto.setType(finding.getType());
        dto.setSeverity(finding.getSeverity());
        dto.setNarration(finding.getNarration());
        dto.setMetrics(parseJson(finding.getMetricJson()));
        dto.setBenchmarks(parseJson(finding.getBenchmarkJson()));
        return dto;
    }

    private BriefResponse.ActionDto toActionDto(AgentAction action) {
        BriefResponse.ActionDto dto = new BriefResponse.ActionDto();
        dto.setId(action.getId());
        dto.setFindingId(action.getFindingId());
        dto.setActionType(action.getActionType());
        dto.setDraftedMessage(action.getDraftedMessage());
        dto.setStatus(action.getStatus());
        dto.setPayload(parseJson(action.getPayloadJson()));
        return dto;
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
