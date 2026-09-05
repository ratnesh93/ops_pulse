package com.moveinsync.opspulse.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.opspulse.api.dto.MonitoringDashboardDto;
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

    @Transactional
    public MonitoringDashboardDto simulateFeed(String scenario) {
        LiveFeedEvent event = createScenarioEvent(scenario.toUpperCase(Locale.ROOT));
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

    private LiveFeedEvent createScenarioEvent(String scenario) {
        LiveFeedEvent event = new LiveFeedEvent();
        event.setSource("SIMULATOR");
        event.setCreatedAt(Instant.now());

        switch (scenario) {
            case "NEGATIVE_DRIVER_ABSENT" -> {
                event.setSentiment("NEGATIVE");
                event.setEventType("DRIVER_ABSENCE");
                event.setTitle("4 drivers absent — Cedar Ridge Office");
                event.setDetail("Login shift 08:00: 4 assigned drivers marked no-show. 3 routes at risk of delay.");
                event.setOffice("Cedar Ridge Office");
                event.setShiftId("08:00");
                event.setMetricValue(BigDecimal.valueOf(4));
            }
            case "NEGATIVE_DELAY_SPIKE" -> {
                event.setSentiment("NEGATIVE");
                event.setEventType("ETA_SLIP");
                event.setTitle("ETA slip +18 min on Rohan Travel routes");
                event.setDetail("Morning pickup window showing sustained delays across Denver Office nodal routes.");
                event.setOffice("Denver Office");
                event.setShiftId("08:00");
                event.setMetricValue(BigDecimal.valueOf(18));
            }
            case "POSITIVE_OTA_RECOVERY" -> {
                event.setSentiment("POSITIVE");
                event.setEventType("OTA_RECOVERY");
                event.setTitle("Priya Travel morning OTA recovered to 96%");
                event.setDetail("Peer vendor outperforming SLA for third consecutive morning shift.");
                event.setOffice("Denver Office");
                event.setShiftId("08:00");
                event.setMetricValue(BigDecimal.valueOf(96));
            }
            case "NEUTRAL_OCCUPANCY" -> {
                event.setSentiment("NEUTRAL");
                event.setEventType("OCCUPANCY_NORM");
                event.setTitle("Clearwater Campus 15:30 shift at 82% occupancy");
                event.setDetail("Within expected range. No capacity intervention needed.");
                event.setOffice("Clearwater Campus");
                event.setShiftId("15:30");
                event.setMetricValue(BigDecimal.valueOf(82));
            }
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario
                    + ". Use NEGATIVE_DRIVER_ABSENT, NEGATIVE_DELAY_SPIKE, POSITIVE_OTA_RECOVERY, NEUTRAL_OCCUPANCY");
        }
        return event;
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
            payload.recommendedAction = "Dispatch backup drivers and notify vendor ops lead within 15 minutes.";
            payload.actionType = event.getEventType().contains("DRIVER") ? "REASSIGN_DRIVERS" : "ESCALATE_VENDOR";
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

    public List<String> listScenarios() {
        return List.of(
                "NEGATIVE_DRIVER_ABSENT",
                "NEGATIVE_DELAY_SPIKE",
                "POSITIVE_OTA_RECOVERY",
                "NEUTRAL_OCCUPANCY");
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
