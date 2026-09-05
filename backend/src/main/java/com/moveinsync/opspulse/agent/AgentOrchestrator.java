package com.moveinsync.opspulse.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.opspulse.benchmark.BenchmarkingService;
import com.moveinsync.opspulse.benchmark.VendorBenchmark;
import com.moveinsync.opspulse.config.OpsPulseProperties;
import com.moveinsync.opspulse.domain.AgentAction;
import com.moveinsync.opspulse.domain.AgentAuditLog;
import com.moveinsync.opspulse.domain.Finding;
import com.moveinsync.opspulse.narration.NarrationClient;
import com.moveinsync.opspulse.repository.AgentActionRepository;
import com.moveinsync.opspulse.repository.AgentAuditLogRepository;
import com.moveinsync.opspulse.repository.FindingRepository;
import com.moveinsync.opspulse.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AgentOrchestrator {

    private final OpsPulseProperties properties;
    private final TripRepository tripRepository;
    private final BenchmarkingService benchmarkingService;
    private final NarrationClient narrationClient;
    private final FindingRepository findingRepository;
    private final AgentActionRepository agentActionRepository;
    private final AgentAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    private volatile String leadershipMemo;

    public AgentOrchestrator(
            OpsPulseProperties properties,
            TripRepository tripRepository,
            BenchmarkingService benchmarkingService,
            NarrationClient narrationClient,
            FindingRepository findingRepository,
            AgentActionRepository agentActionRepository,
            AgentAuditLogRepository auditLogRepository,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.tripRepository = tripRepository;
        this.benchmarkingService = benchmarkingService;
        this.narrationClient = narrationClient;
        this.findingRepository = findingRepository;
        this.agentActionRepository = agentActionRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID runCycle() {
        UUID runId = UUID.randomUUID();
        long tripCount = tripRepository.count();
        audit(runId, "SENSE", String.format("Scanned %,d July trips", tripCount));

        VendorBenchmark benchmark = benchmarkingService.benchmarkFocusVendor();
        audit(runId, "REASON", String.format(
                "%s OTA %.1f%% (SLA %.0f%%, was %.1f%%, peer %.1f%%)",
                benchmark.getVendorDisplayName(),
                benchmark.getOtaPct(),
                benchmark.getSlaOtaPct(),
                benchmark.getPriorMonthOtaPct() != null ? benchmark.getPriorMonthOtaPct() : 0,
                benchmark.getPeerOtaPct() != null ? benchmark.getPeerOtaPct() : 0));

        String attributionSummary = benchmark.getDelayAttribution().entrySet().stream()
                .limit(2)
                .map(e -> e.getKey() + " " + String.format("%.0f%%", e.getValue()))
                .reduce((a, b) -> a + " + " + b)
                .orElse("no delays");
        audit(runId, "REASON", "Delay attribution: " + attributionSummary);

        if (benchmark.isSlaBreach()) {
            Finding finding = createOrRefreshFinding(benchmark);
            draftEscalation(runId, finding, benchmark);
        }

        leadershipMemo = narrationClient.generateLeadershipMemo(benchmark);
        createMemoAction(runId);

        audit(runId, "COMPLETE", "Agent cycle complete — 1 finding, escalation pending confirm");
        return runId;
    }

    public String getLeadershipMemo() {
        return leadershipMemo != null ? leadershipMemo : "";
    }

    @Transactional
    public AgentAction confirmAction(Long actionId) {
        AgentAction action = agentActionRepository.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Action not found: " + actionId));

        if (!"PENDING".equals(action.getStatus())) {
            throw new IllegalStateException("Action is not pending: " + action.getStatus());
        }

        action.setStatus("CONFIRMED");
        action.setConfirmedAt(Instant.now());
        agentActionRepository.save(action);

        UUID runId = UUID.randomUUID();
        audit(runId, "ACT", String.format("CONFIRMED %s (action #%d)", action.getActionType(), actionId));
        return action;
    }

    private Finding createOrRefreshFinding(VendorBenchmark benchmark) {
        List<Finding> existing = findingRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
        Finding finding = existing.isEmpty() ? new Finding() : existing.get(0);

        finding.setType("VENDOR_SLA_BREACH");
        finding.setSeverity("HIGH");
        finding.setMetricJson(toJson(Map.of(
                "vendorId", benchmark.getVendorId(),
                "otaPct", benchmark.getOtaPct(),
                "tripCount", benchmark.getTripCount(),
                "totalCost", benchmark.getTotalCost())));
        finding.setBenchmarkJson(toJson(Map.of(
                "slaOtaPct", benchmark.getSlaOtaPct(),
                "priorMonthOtaPct", benchmark.getPriorMonthOtaPct(),
                "peerOtaPct", benchmark.getPeerOtaPct(),
                "peerVendorName", benchmark.getPeerVendorName(),
                "delayAttribution", benchmark.getDelayAttribution())));
        finding.setNarration(narrationClient.narrateVendorBreach(benchmark));
        finding.setStatus("ACTIVE");
        finding.setCreatedAt(Instant.now());
        return findingRepository.save(finding);
    }

    private void draftEscalation(UUID runId, Finding finding, VendorBenchmark benchmark) {
        List<AgentAction> pending = agentActionRepository
                .findByActionTypeAndStatus("ESCALATE_VENDOR", "PENDING");

        AgentAction action;
        if (pending.isEmpty()) {
            action = new AgentAction();
            action.setCreatedAt(Instant.now());
        } else {
            action = pending.get(0);
        }

        action.setFindingId(finding.getId());
        action.setActionType("ESCALATE_VENDOR");
        action.setPayloadJson(toJson(Map.of(
                "vendorId", benchmark.getVendorId(),
                "vendorDisplayName", benchmark.getVendorDisplayName(),
                "otaPct", benchmark.getOtaPct(),
                "slaOtaPct", benchmark.getSlaOtaPct())));
        action.setDraftedMessage(String.format(
                "Draft escalation to %s: OTA %.1f%% vs SLA %.0f%%. Request corrective action plan within 5 business days.",
                benchmark.getVendorDisplayName(),
                benchmark.getOtaPct(),
                benchmark.getSlaOtaPct()));
        action.setStatus("PENDING");
        agentActionRepository.save(action);

        audit(runId, "ACT", "ESCALATE_VENDOR drafted, pending confirm");
    }

    private void createMemoAction(UUID runId) {
        List<AgentAction> existing = agentActionRepository
                .findByActionTypeAndStatus("LEADERSHIP_MEMO", "AUTO");

        AgentAction memo = existing.isEmpty() ? new AgentAction() : existing.get(0);
        memo.setActionType("LEADERSHIP_MEMO");
        memo.setDraftedMessage(leadershipMemo);
        memo.setStatus("AUTO");
        memo.setCreatedAt(memo.getCreatedAt() != null ? memo.getCreatedAt() : Instant.now());
        agentActionRepository.save(memo);

        audit(runId, "ACT", "LEADERSHIP_MEMO auto-generated");
    }

    private void audit(UUID runId, String stage, String message) {
        AgentAuditLog log = new AgentAuditLog();
        log.setRunId(runId);
        log.setStage(stage);
        log.setMessage(message);
        log.setTimestamp(Instant.now());
        auditLogRepository.save(log);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
