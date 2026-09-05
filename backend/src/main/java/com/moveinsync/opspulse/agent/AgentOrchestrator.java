package com.moveinsync.opspulse.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.opspulse.benchmark.BenchmarkingService;
import com.moveinsync.opspulse.benchmark.CapacityInsight;
import com.moveinsync.opspulse.benchmark.OperationalInsightsService;
import com.moveinsync.opspulse.benchmark.SafetyInsight;
import com.moveinsync.opspulse.benchmark.VendorBenchmark;
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

    private final TripRepository tripRepository;
    private final BenchmarkingService benchmarkingService;
    private final OperationalInsightsService operationalInsightsService;
    private final NarrationClient narrationClient;
    private final FindingRepository findingRepository;
    private final AgentActionRepository agentActionRepository;
    private final AgentAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    private volatile String leadershipMemo;

    public AgentOrchestrator(
            TripRepository tripRepository,
            BenchmarkingService benchmarkingService,
            OperationalInsightsService operationalInsightsService,
            NarrationClient narrationClient,
            FindingRepository findingRepository,
            AgentActionRepository agentActionRepository,
            AgentAuditLogRepository auditLogRepository,
            ObjectMapper objectMapper) {
        this.tripRepository = tripRepository;
        this.benchmarkingService = benchmarkingService;
        this.operationalInsightsService = operationalInsightsService;
        this.narrationClient = narrationClient;
        this.findingRepository = findingRepository;
        this.agentActionRepository = agentActionRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID runCycle() {
        UUID runId = UUID.randomUUID();
        int findingCount = 0;
        int actionCount = 0;

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
            Finding finding = upsertFinding("VENDOR_SLA_BREACH", "HIGH", benchmark, null, null);
            draftEscalation(runId, finding, benchmark);
            findingCount++;
            actionCount++;
        }

        SafetyInsight safety = operationalInsightsService.analyzeSafety();
        audit(runId, "SENSE", String.format(
                "Scanned %,d July safety alerts — %d Sev-1, %d panic",
                safety.getJulyAlertCount(),
                safety.getSev1Count(),
                safety.getPanicCount()));

        if (safety.requiresEscalation()) {
            Finding safetyFinding = upsertFinding("SAFETY_ESCALATION", "HIGH", null, safety, null);
            draftSafetyEscalation(runId, safetyFinding, safety);
            findingCount++;
            actionCount++;
        }

        CapacityInsight capacity = operationalInsightsService.analyzeCapacity();
        if (capacity != null && capacity.hasOverbooking()) {
            audit(runId, "REASON", String.format(
                    "%s %s overbooked by %,d seats across %,d trips",
                    capacity.getOffice(),
                    capacity.getShiftId(),
                    capacity.getOverbookedSeats(),
                    capacity.getOverbookedTrips()));

            Finding capacityFinding = upsertFinding("CAPACITY_SHORTFALL", "MEDIUM", null, null, capacity);
            draftAddCapacity(runId, capacityFinding, capacity);
            findingCount++;
            actionCount++;
        }

        leadershipMemo = narrationClient.generateLeadershipMemo(benchmark);
        createMemoAction(runId);

        audit(runId, "COMPLETE", String.format(
                "Agent cycle complete — %d findings, %d actions pending confirm",
                findingCount,
                actionCount));
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

    private Finding upsertFinding(
            String type,
            String severity,
            VendorBenchmark benchmark,
            SafetyInsight safety,
            CapacityInsight capacity) {
        Finding finding = findingRepository
                .findFirstByTypeAndStatusOrderByCreatedAtDesc(type, "ACTIVE")
                .orElse(new Finding());

        finding.setType(type);
        finding.setSeverity(severity);
        finding.setStatus("ACTIVE");
        finding.setCreatedAt(Instant.now());

        if (benchmark != null) {
            Map<String, Object> metric = new HashMap<>();
            metric.put("vendorId", benchmark.getVendorId());
            metric.put("otaPct", benchmark.getOtaPct());
            metric.put("tripCount", benchmark.getTripCount());
            metric.put("totalCost", benchmark.getTotalCost());
            finding.setMetricJson(toJson(metric));

            Map<String, Object> benchmarkData = new HashMap<>();
            benchmarkData.put("slaOtaPct", benchmark.getSlaOtaPct());
            benchmarkData.put("priorMonthOtaPct", benchmark.getPriorMonthOtaPct());
            benchmarkData.put("peerOtaPct", benchmark.getPeerOtaPct());
            benchmarkData.put("peerVendorName", benchmark.getPeerVendorName());
            benchmarkData.put("delayAttribution", benchmark.getDelayAttribution());
            finding.setBenchmarkJson(toJson(benchmarkData));
            finding.setNarration(narrationClient.narrateVendorBreach(benchmark));
        } else if (safety != null) {
            finding.setMetricJson(toJson(Map.of(
                    "julyAlertCount", safety.getJulyAlertCount(),
                    "sev1Count", safety.getSev1Count(),
                    "panicCount", safety.getPanicCount(),
                    "openHighSeverityCount", safety.getOpenHighSeverityCount())));
            finding.setBenchmarkJson(toJson(Map.of("topEventTypes", safety.getTopEventTypes())));
            finding.setNarration(narrationClient.narrateSafetyEscalation(safety));
        } else if (capacity != null) {
            finding.setMetricJson(toJson(Map.of(
                    "office", capacity.getOffice(),
                    "shiftId", capacity.getShiftId(),
                    "overbookedSeats", capacity.getOverbookedSeats(),
                    "overbookedTrips", capacity.getOverbookedTrips(),
                    "totalRiders", capacity.getTotalRiders(),
                    "totalSeats", capacity.getTotalSeats())));
            finding.setBenchmarkJson(toJson(Map.of(
                    "recommendedExtraVehicles", capacity.recommendedExtraVehicles())));
            finding.setNarration(narrationClient.narrateCapacityShortfall(capacity));
        }

        return findingRepository.save(finding);
    }

    private void draftEscalation(UUID runId, Finding finding, VendorBenchmark benchmark) {
        AgentAction action = getOrCreatePendingAction("ESCALATE_VENDOR");
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
        if (action.getCreatedAt() == null) {
            action.setCreatedAt(Instant.now());
        }
        agentActionRepository.save(action);
        audit(runId, "ACT", "ESCALATE_VENDOR drafted, pending confirm");
    }

    private void draftSafetyEscalation(UUID runId, Finding finding, SafetyInsight safety) {
        AgentAction action = getOrCreatePendingAction("ESCALATE_SAFETY");
        action.setFindingId(finding.getId());
        action.setActionType("ESCALATE_SAFETY");
        action.setPayloadJson(toJson(Map.of(
                "sev1Count", safety.getSev1Count(),
                "panicCount", safety.getPanicCount(),
                "julyAlertCount", safety.getJulyAlertCount(),
                "topEventTypes", safety.getTopEventTypes())));
        action.setDraftedMessage(String.format(
                "Draft safety escalation: %d Sev-1 alerts (%d panic) in July. Route to transport safety desk for 24h review.",
                safety.getSev1Count(),
                safety.getPanicCount()));
        action.setStatus("PENDING");
        if (action.getCreatedAt() == null) {
            action.setCreatedAt(Instant.now());
        }
        agentActionRepository.save(action);
        audit(runId, "ACT", "ESCALATE_SAFETY drafted, pending confirm");
    }

    private void draftAddCapacity(UUID runId, Finding finding, CapacityInsight capacity) {
        int extra = capacity.recommendedExtraVehicles();
        AgentAction action = getOrCreatePendingAction("ADD_CAPACITY");
        action.setFindingId(finding.getId());
        action.setActionType("ADD_CAPACITY");
        Map<String, Object> payload = new HashMap<>();
        payload.put("office", capacity.getOffice());
        payload.put("shiftId", capacity.getShiftId());
        payload.put("overbookedSeats", capacity.getOverbookedSeats());
        payload.put("extraVehicles", extra);
        action.setPayloadJson(toJson(payload));
        action.setDraftedMessage(String.format(
                "ADD_CAPACITY: Deploy +%d backup vehicles for %s %s shift (%d-seat gap, %d trips). Draft vendor extra-trip request.",
                extra,
                capacity.getOffice(),
                capacity.getShiftId(),
                capacity.getOverbookedSeats(),
                capacity.getOverbookedTrips()));
        action.setStatus("PENDING");
        if (action.getCreatedAt() == null) {
            action.setCreatedAt(Instant.now());
        }
        agentActionRepository.save(action);
        audit(runId, "ACT", "ADD_CAPACITY drafted, pending confirm");
    }

    private AgentAction getOrCreatePendingAction(String actionType) {
        List<AgentAction> pending = agentActionRepository.findByActionTypeAndStatus(actionType, "PENDING");
        if (pending.isEmpty()) {
            AgentAction action = new AgentAction();
            action.setCreatedAt(Instant.now());
            return action;
        }
        return pending.get(0);
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
