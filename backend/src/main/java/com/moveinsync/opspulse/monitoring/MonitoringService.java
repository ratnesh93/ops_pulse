package com.moveinsync.opspulse.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.opspulse.api.dto.MonitoringDashboardDto;
import com.moveinsync.opspulse.api.dto.MonitoringScenarioDto;
import com.moveinsync.opspulse.benchmark.BenchmarkingService;
import com.moveinsync.opspulse.benchmark.VendorBenchmark;
import com.moveinsync.opspulse.domain.LiveActionItem;
import com.moveinsync.opspulse.domain.LiveFeedEvent;
import com.moveinsync.opspulse.openai.ChatCompletionResult;
import com.moveinsync.opspulse.openai.OpenAiClient;
import com.moveinsync.opspulse.repository.LiveActionItemRepository;
import com.moveinsync.opspulse.repository.LiveFeedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final LiveFeedEventRepository eventRepository;
    private final LiveActionItemRepository actionRepository;
    private final BenchmarkingService benchmarkingService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public MonitoringService(
            LiveFeedEventRepository eventRepository,
            LiveActionItemRepository actionRepository,
            BenchmarkingService benchmarkingService,
            OpenAiClient openAiClient,
            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.actionRepository = actionRepository;
        this.benchmarkingService = benchmarkingService;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
    }

    public MonitoringDashboardDto getDashboard() {
        MonitoringDashboardDto dto = new MonitoringDashboardDto();
        dto.setOpenAiConfigured(openAiClient.isConfigured());
        dto.setFeedEvents(eventRepository.findTop30ByOrderByCreatedAtDesc().stream()
                .map(this::toEventDto)
                .toList());
        dto.setActionItems(actionRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::toActionDto)
                .toList());
        dto.setPendingCount(actionRepository.findByStatusOrderByCreatedAtDesc("PENDING").size());
        return dto;
    }

    public List<LiveFeedEvent> recentEtaEvents(int limit) {
        return eventRepository.findTop30ByOrderByCreatedAtDesc().stream()
                .filter(this::isEtaRelated)
                .limit(limit)
                .toList();
    }

    public String buildRecentEtaSummaryForChat() {
        List<LiveFeedEvent> events = recentEtaEvents(5);
        if (events.isEmpty()) {
            return "(none — inject ETA missed on Live Monitoring tab)";
        }
        return events.stream()
                .map(e -> e.getTitle() + " | " + e.getDetail())
                .collect(Collectors.joining("\n"));
    }

    private boolean isEtaRelated(LiveFeedEvent event) {
        if (event.getEventType() != null) {
            String type = event.getEventType().toUpperCase(Locale.ROOT);
            if (type.contains("ETA")) {
                return true;
            }
        }
        String title = event.getTitle() != null ? event.getTitle().toLowerCase(Locale.ROOT) : "";
        return title.contains("eta");
    }

    @Transactional
    public MonitoringDashboardDto simulateFeed(String scenario, String vendor) {
        String normalizedScenario = scenario.toUpperCase(Locale.ROOT);
        String vendorKey = vendor == null || vendor.isBlank() ? "rohan" : vendor.toLowerCase(Locale.ROOT);
        LiveFeedEvent event = createScenarioEvent(normalizedScenario, vendorKey);
        event = eventRepository.save(event);
        generateActionItem(event);
        return getDashboard();
    }

    @Transactional
    public LiveActionItem confirmAction(Long id) {
        LiveActionItem item = actionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Action not found: " + id));
        if (!"PENDING".equals(item.getStatus())) {
            throw new IllegalStateException("Action is not pending: " + item.getStatus());
        }
        item.setStatus("CONFIRMED");
        item.setConfirmedAt(Instant.now());
        return actionRepository.save(item);
    }

    @Transactional
    public LiveActionItem dismissAction(Long id) {
        LiveActionItem item = actionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Action not found: " + id));
        item.setStatus("DISMISSED");
        return actionRepository.save(item);
    }

    private LiveFeedEvent createScenarioEvent(String scenario, String vendorKey) {
        VendorContext vendor = resolveVendor(vendorKey);
        LiveFeedEvent event = new LiveFeedEvent();
        event.setSource("SIMULATOR");
        event.setCreatedAt(Instant.now());

        switch (scenario) {
            case "NEGATIVE_DRIVER_ABSENT" -> {
                event.setSentiment("NEGATIVE");
                event.setEventType("DRIVER_ABSENCE");
                event.setTitle("4 drivers absent — " + vendor.office());
                event.setDetail(vendor.name() + " login shift 08:00: 4 assigned drivers marked no-show. 3 routes at risk of delay.");
                event.setOffice(vendor.office());
                event.setShiftId("08:00");
                event.setMetricValue(BigDecimal.valueOf(4));
            }
            case "NEGATIVE_DELAY_SPIKE" -> {
                event.setSentiment("NEGATIVE");
                event.setEventType("ETA_SLIP");
                event.setTitle("ETA slip +18 min on " + vendor.name() + " routes");
                event.setDetail("Morning pickup window showing sustained delays across " + vendor.office() + " nodal routes.");
                event.setOffice(vendor.office());
                event.setShiftId("08:00");
                event.setMetricValue(BigDecimal.valueOf(18));
            }
            case "NEGATIVE_ROAD_BLOCK" -> {
                event.setSentiment("NEGATIVE");
                event.setEventType("ROAD_BLOCK");
                event.setTitle("Road block on ORR — " + vendor.office() + " routes stalled");
                event.setDetail("Traffic closure near Silk Board junction. " + vendor.name()
                        + " has 8 active shuttles affected; average ETA slip +25 min. Reroute required.");
                event.setOffice(vendor.office());
                event.setShiftId("08:00");
                event.setMetricValue(BigDecimal.valueOf(25));
            }
            case "NEGATIVE_ETA_MISSED" -> {
                event.setSentiment("NEGATIVE");
                event.setEventType("ETA_MISSED");
                event.setTitle("ETA missed — " + vendor.name() + " · +22 min");
                event.setDetail("Trip DEN-4821 (" + vendor.name() + "): scheduled 08:15 pickup still pending. "
                        + "14 employees waiting at nodal point. SLA breach imminent.");
                event.setOffice(vendor.office());
                event.setShiftId("08:00");
                event.setMetricValue(BigDecimal.valueOf(22));
            }
            case "NEGATIVE_VEHICLE_BREAKDOWN" -> {
                event.setSentiment("NEGATIVE");
                event.setEventType("VEHICLE_BREAKDOWN");
                event.setTitle("Vehicle breakdown — " + vendor.name() + " shuttle");
                event.setDetail("Mechanical failure on Route C-12 (" + vendor.office() + "). "
                        + "42 passengers stranded. Backup vehicle ETA 35 min.");
                event.setOffice(vendor.office());
                event.setShiftId("08:00");
                event.setMetricValue(BigDecimal.valueOf(35));
            }
            case "POSITIVE_OTA_RECOVERY" -> {
                event.setSentiment("POSITIVE");
                event.setEventType("OTA_RECOVERY");
                event.setTitle(vendor.name() + " morning OTA recovered to 96%");
                event.setDetail(vendor.name() + " outperforming SLA for third consecutive morning shift at "
                        + vendor.office() + ".");
                event.setOffice(vendor.office());
                event.setShiftId("08:00");
                event.setMetricValue(BigDecimal.valueOf(96));
            }
            case "POSITIVE_ROUTE_CLEARED" -> {
                event.setSentiment("POSITIVE");
                event.setEventType("ROUTE_CLEARED");
                event.setTitle("Road block cleared — " + vendor.name() + " routes recovering");
                event.setDetail("ORR corridor reopened. " + vendor.name()
                        + " average delay dropping from +25 min to +8 min across morning shift.");
                event.setOffice(vendor.office());
                event.setShiftId("08:00");
                event.setMetricValue(BigDecimal.valueOf(8));
            }
            case "NEUTRAL_OCCUPANCY" -> {
                event.setSentiment("NEUTRAL");
                event.setEventType("OCCUPANCY_NORM");
                event.setTitle("Clearwater Campus 15:30 shift at 82% occupancy");
                event.setDetail("Within expected range for " + vendor.name() + " routes. No capacity intervention needed.");
                event.setOffice("Clearwater Campus");
                event.setShiftId("15:30");
                event.setMetricValue(BigDecimal.valueOf(82));
            }
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario
                    + ". Use GET /api/monitoring/scenarios for available demo signals.");
        }
        return event;
    }

    private VendorContext resolveVendor(String vendorKey) {
        if ("priya".equals(vendorKey) || "peer".equals(vendorKey)) {
            return new VendorContext("Priya Travel", "Denver Office");
        }
        return new VendorContext("Rohan Travel (Vendor B)", "Denver Office");
    }

    private void generateActionItem(LiveFeedEvent event) {
        InsightPayload insight = openAiClient.isConfigured()
                ? generateOpenAiInsight(event)
                : generateFallbackInsight(event);

        LiveActionItem item = new LiveActionItem();
        item.setEventId(event.getId());
        item.setSeverity(insight.severity);
        item.setTitle(insight.title);
        item.setAiInsight(insight.insight);
        item.setRecommendedAction(insight.recommendedAction);
        item.setActionType(insight.actionType);
        item.setStatus("PENDING");
        item.setOpenaiModel(insight.model);
        item.setCreatedAt(Instant.now());
        actionRepository.save(item);
    }

    private InsightPayload generateOpenAiInsight(LiveFeedEvent event) {
        VendorBenchmark vendor = benchmarkingService.benchmarkFocusVendor();
        String recentFeed = eventRepository.findTop30ByOrderByCreatedAtDesc().stream()
                .limit(5)
                .map(e -> e.getSentiment() + " | " + e.getTitle())
                .collect(Collectors.joining("\n"));

        String system = """
                You are Ops Pulse, an enterprise mobility operations copilot.
                Given a live feed event and July benchmark context, produce ONE actionable insight.
                Respond ONLY with valid JSON:
                {
                  "severity": "HIGH|MEDIUM|LOW",
                  "title": "short headline",
                  "insight": "2-3 sentence analysis tying live signal to static benchmarks",
                  "recommendedAction": "specific action for transport manager",
                  "actionType": "REASSIGN_DRIVERS|ADD_CAPACITY|ESCALATE_VENDOR|MONITOR|ACKNOWLEDGE"
                }
                """;

        String user = String.format("""
                LIVE EVENT:
                - Sentiment: %s
                - Type: %s
                - Title: %s
                - Detail: %s
                - Office: %s
                - Shift: %s
                - Metric: %s

                JULY BENCHMARK CONTEXT:
                - Focus vendor %s OTA: %.1f%% (SLA %.0f%%)
                - Peer %s OTA: %.1f%%
                - Top delayed office: %s

                RECENT LIVE FEED:
                %s
                """,
                event.getSentiment(),
                event.getEventType(),
                event.getTitle(),
                event.getDetail(),
                event.getOffice(),
                event.getShiftId(),
                event.getMetricValue(),
                vendor.getVendorDisplayName(),
                vendor.getOtaPct(),
                vendor.getSlaOtaPct(),
                vendor.getPeerVendorName(),
                vendor.getPeerOtaPct() != null ? vendor.getPeerOtaPct() : 0,
                vendor.getTopAffectedOffice() != null ? vendor.getTopAffectedOffice() : "N/A",
                recentFeed.isBlank() ? "(none)" : recentFeed);

        try {
            ChatCompletionResult result = openAiClient.chat(system, user);
            JsonNode json = objectMapper.readTree(result.getContent());
            InsightPayload payload = new InsightPayload();
            payload.severity = json.path("severity").asText("MEDIUM");
            payload.title = json.path("title").asText(event.getTitle());
            payload.insight = json.path("insight").asText(event.getDetail());
            payload.recommendedAction = json.path("recommendedAction").asText("Review and confirm next step.");
            payload.actionType = json.path("actionType").asText("MONITOR");
            payload.model = result.getModel();
            return payload;
        } catch (Exception e) {
            log.warn("OpenAI insight failed, using fallback: {}", e.getMessage());
            return generateFallbackInsight(event);
        }
    }

    private InsightPayload generateFallbackInsight(LiveFeedEvent event) {
        InsightPayload payload = new InsightPayload();
        payload.model = "template-fallback";
        VendorBenchmark vendor = benchmarkingService.benchmarkFocusVendor();

        if ("NEGATIVE".equals(event.getSentiment())) {
            payload.severity = "HIGH";
            payload.title = "Live alert: " + event.getTitle();
            payload.insight = String.format(
                    "%s at %s %s shift. With %s already at %.1f%% OTA vs %.0f%% SLA, this live signal increases delay risk.",
                    event.getDetail(),
                    event.getOffice(),
                    event.getShiftId(),
                    vendor.getVendorDisplayName(),
                    vendor.getOtaPct(),
                    vendor.getSlaOtaPct());
            if ("ROAD_BLOCK".equals(event.getEventType())) {
                payload.recommendedAction = "Activate alternate routes via nodal bypass; notify " + vendor.getVendorDisplayName()
                        + " ops and dispatch 2 backup shuttles within 10 minutes.";
                payload.actionType = "ADD_CAPACITY";
            } else if ("ETA_MISSED".equals(event.getEventType())) {
                payload.recommendedAction = "Call vendor ops lead; reassign nearest available shuttle and send employee ETA SMS.";
                payload.actionType = "ESCALATE_VENDOR";
            } else if (event.getEventType().contains("DRIVER")) {
                payload.recommendedAction = "Dispatch backup drivers and notify vendor ops lead within 15 minutes.";
                payload.actionType = "REASSIGN_DRIVERS";
            } else if ("VEHICLE_BREAKDOWN".equals(event.getEventType())) {
                payload.recommendedAction = "Deploy backup vehicle immediately; transfer passengers and log SLA impact.";
                payload.actionType = "ADD_CAPACITY";
            } else {
                payload.recommendedAction = "Dispatch backup drivers and notify vendor ops lead within 15 minutes.";
                payload.actionType = "ESCALATE_VENDOR";
            }
        } else if ("POSITIVE".equals(event.getSentiment())) {
            payload.severity = "LOW";
            payload.title = "Positive signal: " + event.getTitle();
            payload.insight = event.getDetail() + " Consider sharing as best practice with underperforming vendors.";
            payload.recommendedAction = "Acknowledge and log for weekly vendor review.";
            payload.actionType = "ACKNOWLEDGE";
        } else {
            payload.severity = "LOW";
            payload.title = event.getTitle();
            payload.insight = event.getDetail() + " No immediate intervention required.";
            payload.recommendedAction = "Continue monitoring — no action needed.";
            payload.actionType = "MONITOR";
        }
        return payload;
    }

    private MonitoringDashboardDto.FeedEventDto toEventDto(LiveFeedEvent event) {
        MonitoringDashboardDto.FeedEventDto dto = new MonitoringDashboardDto.FeedEventDto();
        dto.setId(event.getId());
        dto.setSentiment(event.getSentiment());
        dto.setEventType(event.getEventType());
        dto.setTitle(event.getTitle());
        dto.setDetail(event.getDetail());
        dto.setOffice(event.getOffice());
        dto.setShiftId(event.getShiftId());
        dto.setMetricValue(event.getMetricValue());
        dto.setSource(event.getSource());
        dto.setCreatedAt(event.getCreatedAt());
        return dto;
    }

    private MonitoringDashboardDto.ActionItemDto toActionDto(LiveActionItem item) {
        MonitoringDashboardDto.ActionItemDto dto = new MonitoringDashboardDto.ActionItemDto();
        dto.setId(item.getId());
        dto.setEventId(item.getEventId());
        dto.setSeverity(item.getSeverity());
        dto.setTitle(item.getTitle());
        dto.setAiInsight(item.getAiInsight());
        dto.setRecommendedAction(item.getRecommendedAction());
        dto.setActionType(item.getActionType());
        dto.setStatus(item.getStatus());
        dto.setOpenaiModel(item.getOpenaiModel());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setConfirmedAt(item.getConfirmedAt());
        return dto;
    }

    public List<MonitoringScenarioDto> listScenarioOptions() {
        return List.of(
                new MonitoringScenarioDto("NEGATIVE_ROAD_BLOCK", "Road block on route",
                        "NEGATIVE", "ORR corridor closure — reroute shuttles"),
                new MonitoringScenarioDto("NEGATIVE_ETA_MISSED", "ETA missed for vendor",
                        "NEGATIVE", "Specific trip past pickup window"),
                new MonitoringScenarioDto("NEGATIVE_DRIVER_ABSENT", "Drivers absent",
                        "NEGATIVE", "No-shows at login shift"),
                new MonitoringScenarioDto("NEGATIVE_DELAY_SPIKE", "ETA slip spike",
                        "NEGATIVE", "Sustained delay across routes"),
                new MonitoringScenarioDto("NEGATIVE_VEHICLE_BREAKDOWN", "Vehicle breakdown",
                        "NEGATIVE", "Shuttle stranded — backup needed"),
                new MonitoringScenarioDto("POSITIVE_OTA_RECOVERY", "OTA recovery",
                        "POSITIVE", "Vendor back above SLA"),
                new MonitoringScenarioDto("POSITIVE_ROUTE_CLEARED", "Road block cleared",
                        "POSITIVE", "Corridor reopened — delays easing"),
                new MonitoringScenarioDto("NEUTRAL_OCCUPANCY", "Occupancy normal",
                        "NEUTRAL", "Within expected capacity range"));
    }

    private record VendorContext(String name, String office) {
    }

    private static class InsightPayload {
        String severity;
        String title;
        String insight;
        String recommendedAction;
        String actionType;
        String model;
    }
}
