package com.moveinsync.opspulse.chat;

import com.moveinsync.opspulse.agent.AgentOrchestrator;
import com.moveinsync.opspulse.api.dto.BriefResponse;
import com.moveinsync.opspulse.api.dto.FacilitiesSummaryDto;
import com.moveinsync.opspulse.api.dto.VendorSummaryDto;
import com.moveinsync.opspulse.api.BriefController;
import com.moveinsync.opspulse.benchmark.BenchmarkingService;
import com.moveinsync.opspulse.benchmark.OperationalInsightsService;
import com.moveinsync.opspulse.benchmark.OfficeSummary;
import com.moveinsync.opspulse.benchmark.SafetyInsight;
import com.moveinsync.opspulse.benchmark.VendorBenchmark;
import com.moveinsync.opspulse.domain.LiveFeedEvent;
import com.moveinsync.opspulse.monitoring.MonitoringService;
import com.moveinsync.opspulse.narration.NarrationClient;
import com.moveinsync.opspulse.openai.OpenAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final BenchmarkingService benchmarkingService;
    private final OperationalInsightsService operationalInsightsService;
    private final NarrationClient narrationClient;
    private final BriefController briefController;
    private final AgentOrchestrator agentOrchestrator;
    private final OpenAiClient openAiClient;
    private final MonitoringService monitoringService;

    public ChatService(
            BenchmarkingService benchmarkingService,
            OperationalInsightsService operationalInsightsService,
            NarrationClient narrationClient,
            BriefController briefController,
            AgentOrchestrator agentOrchestrator,
            OpenAiClient openAiClient,
            MonitoringService monitoringService) {
        this.benchmarkingService = benchmarkingService;
        this.operationalInsightsService = operationalInsightsService;
        this.narrationClient = narrationClient;
        this.briefController = briefController;
        this.agentOrchestrator = agentOrchestrator;
        this.openAiClient = openAiClient;
        this.monitoringService = monitoringService;
    }

    public String reply(String message) {
        if (message == null || message.isBlank()) {
            return helpText();
        }

        String q = message.toLowerCase(Locale.ROOT).trim();

        if (isClearlyOutOfScope(q)) {
            return outOfScopeMessage();
        }

        VendorBenchmark vendor = benchmarkingService.benchmarkFocusVendor();
        BriefResponse brief = briefController.getBrief();

        if (containsAny(q, "help", "what can", "how do")) {
            return helpText();
        }
        if (containsAny(q, "safety", "alert", "alerts", "panic", "sev-1", "sev1", "incident", "security")) {
            return safetyReply(q);
        }
        if (containsAny(q, "ota", "on time", "on-time", "sla", "performance", "breach")) {
            return String.format(
                    "%s OTA is %.1f%% vs SLA %.0f%%. Prior month: %.1f%%. Peer %s: %.1f%%. %s",
                    vendor.getVendorDisplayName(),
                    vendor.getOtaPct(),
                    vendor.getSlaOtaPct(),
                    vendor.getPriorMonthOtaPct() != null ? vendor.getPriorMonthOtaPct() : 0,
                    vendor.getPeerVendorName(),
                    vendor.getPeerOtaPct() != null ? vendor.getPeerOtaPct() : 0,
                    vendor.isSlaBreach() ? "This is an active SLA breach." : "");
        }
        if (containsAny(q, "delay", "attribution", "reason", "why late", "root cause")) {
            if (vendor.getDelayAttribution().isEmpty()) {
                return "No delay attribution data available for this vendor.";
            }
            String breakdown = vendor.getDelayAttribution().entrySet().stream()
                    .map(e -> e.getKey() + " " + String.format("%.0f%%", e.getValue()))
                    .collect(Collectors.joining(", "));
            return "Delay attribution for " + vendor.getVendorDisplayName() + ": " + breakdown + ".";
        }
        if (containsAny(q, "cost", "spend", "billing", "exposure", "rupee", "crore", "cost per km", "cost/km")) {
            if (containsAny(q, "per km", "per-km", "/km", "cost per km")) {
                FacilitiesSummaryDto summary = benchmarkingService.buildFacilitiesSummary();
                return String.format(
                        "Fleet cost is %s across %,d km — %.0f per km. Focus vendor %s cost/km varies by vendor; see Vendors tab.",
                        formatCost(summary.getTotalCost()),
                        summary.getTotalKm() != null ? summary.getTotalKm().longValue() : 0,
                        summary.getCostPerKm() != null ? summary.getCostPerKm().doubleValue() : 0,
                        vendor.getVendorDisplayName());
            }
            return String.format(
                    "Cost exposure for %s is %s across %,d July trips.",
                    vendor.getVendorDisplayName(),
                    formatCost(vendor.getTotalCost()),
                    vendor.getTripCount());
        }
        if (containsAny(q, "action", "escalate", "pending", "confirm")) {
            if (brief.getPendingActions().isEmpty()) {
                return "No pending actions right now. Run the agent if you expect a new escalation.";
            }
            return brief.getPendingActions().stream()
                    .map(a -> a.getActionType() + ": " + a.getDraftedMessage())
                    .collect(Collectors.joining("\n"));
        }
        if (containsAny(q, "finding", "issue", "problem")) {
            if (brief.getFindings().isEmpty()) {
                return "No active findings. Try running the agent.";
            }
            return brief.getFindings().stream()
                    .map(BriefResponse.FindingDto::getNarration)
                    .collect(Collectors.joining("\n\n"));
        }
        if (containsAny(q, "memo", "leadership", "executive", "summary", "aggregate", "facilities", "fleet")) {
            if (containsAny(q, "memo", "leadership", "executive")) {
                String memo = agentOrchestrator.getLeadershipMemo();
                if (memo == null || memo.isBlank()) {
                    return "Leadership memo not generated yet. Run the agent first.";
                }
                return memo.length() > 800 ? memo.substring(0, 800) + "…" : memo;
            }
            return facilitiesSummaryReply();
        }
        if (containsAny(q, "vendor", "vendors", "list vendor", "all vendor", "rohan", "priya", "scorecard")) {
            return vendorScorecardReply(q);
        }
        if (containsAny(q, "office", "campus", "location", "address")) {
            return officeReply(q, vendor);
        }

        if (openAiClient.isConfigured() && (isInScope(q) || !isClearlyOutOfScope(q))) {
            try {
                return openAiReply(message);
            } catch (Exception e) {
                log.warn("OpenAI chat failed, using rule-based fallback: {}", e.getMessage());
            }
        }

        if (isEtaQuestion(q)) {
            return etaMissedReply();
        }

        if (!isInScope(q)) {
            return outOfScopeMessage();
        }

        return inScopeUnmatchedMessage();
    }

    private boolean isEtaQuestion(String q) {
        return containsAny(q, "eta", "missed", "pickup window", "eta slip", "slip spike", "late pickup");
    }

    private String etaMissedReply() {
        List<LiveFeedEvent> liveEvents = monitoringService.recentEtaEvents(5);
        if (!liveEvents.isEmpty()) {
            return "Recent ETA misses/slips by travel vendor:\n"
                    + liveEvents.stream()
                            .map(e -> "• " + e.getTitle() + " — " + e.getDetail())
                            .collect(Collectors.joining("\n"));
        }

        List<String> vendorDelays = benchmarkingService.topVendorsByDelayedTrips(5);
        if (!vendorDelays.isEmpty()) {
            return "No live ETA events in the feed yet. July delayed-trip leaders by vendor:\n"
                    + vendorDelays.stream().map(v -> "• " + v).collect(Collectors.joining("\n"))
                    + "\n\nInject an \"ETA missed\" demo signal on the Live Monitoring tab for real-time events.";
        }

        return "No recent ETA missed events found. Use Live Monitoring → Inject demo signal → ETA missed for vendor.";
    }

    private String officeReply(String q, VendorBenchmark vendor) {
        List<String> registered = benchmarkingService.listRegisteredOffices();
        String matched = matchRegisteredOffice(q, registered);

        if (matched != null) {
            return benchmarkingService.summarizeOffice(matched)
                    .map(this::formatOfficeSummary)
                    .orElse(officeNotRegisteredMessage());
        }

        if (isGenericOfficeQuery(q)) {
            String office = vendor.getTopAffectedOffice();
            return office != null
                    ? "Top affected office for delays: " + office + "."
                    : "No office concentration data available.";
        }

        if (looksLikeSpecificOfficeQuery(q)) {
            return officeNotRegisteredMessage();
        }

        String office = vendor.getTopAffectedOffice();
        return office != null
                ? "Top affected office for delays: " + office + "."
                : "No office concentration data available.";
    }

    private String formatOfficeSummary(OfficeSummary summary) {
        String routeScope;
        if (summary.getLoginRouteCount() > 0 && summary.getLogoutRouteCount() > 0) {
            routeScope = String.format(
                    "%d login (destination) and %d logout (origin) routes",
                    summary.getLoginRouteCount(),
                    summary.getLogoutRouteCount());
        } else if (summary.getLoginRouteCount() > 0) {
            routeScope = summary.getLoginRouteCount() + " login (destination) routes";
        } else if (summary.getLogoutRouteCount() > 0) {
            routeScope = summary.getLogoutRouteCount() + " logout (origin) routes";
        } else {
            routeScope = "no active route endpoints";
        }

        return String.format(
                "%s is a registered office in our route network (%s). "
                        + "July: %,d trips, OTA %.1f%%, %,d delayed trips.",
                summary.getOffice(),
                routeScope,
                summary.getTripCount(),
                summary.getOtaPct(),
                summary.getDelayedCount());
    }

    private String matchRegisteredOffice(String q, List<String> registeredOffices) {
        String normalized = q.toLowerCase(Locale.ROOT);
        String best = null;
        int bestLen = 0;

        for (String office : registeredOffices) {
            String officeLower = office.toLowerCase(Locale.ROOT);
            if (normalized.contains(officeLower) && officeLower.length() > bestLen) {
                best = office;
                bestLen = officeLower.length();
            }
        }

        if (best == null) {
            for (String office : registeredOffices) {
                String key = office.toLowerCase(Locale.ROOT)
                        .replaceAll("\\s+(office|campus|center|commons)$", "");
                if (key.length() >= 4 && normalized.contains(key) && key.length() > bestLen) {
                    best = office;
                    bestLen = key.length();
                }
            }
        }

        return best;
    }

    private boolean isGenericOfficeQuery(String q) {
        return containsAny(q,
                "top affected", "which office", "most delay", "all office", "list office",
                "registered office", "any office", "worst office", "best office", "how many office");
    }

    private boolean looksLikeSpecificOfficeQuery(String q) {
        if (isGenericOfficeQuery(q)) {
            return false;
        }
        if (containsAny(q, "address")) {
            return true;
        }
        if (q.matches(".*\\b[a-z]{3,}\\s+(office|campus|center|commons)\\b.*")) {
            return true;
        }
        return q.matches(".*\\b(about|for|at|in)\\s+\\w{3,}.*")
                && containsAny(q, "office", "campus", "location");
    }

    private String officeNotRegisteredMessage() {
        return "That's out of scope — the office you mentioned is not registered in our route network "
                + "(no matching route origin or destination found).";
    }

    private String safetyReply(String q) {
        SafetyInsight safety = operationalInsightsService.analyzeSafety();
        VendorSummaryDto matched = matchVendor(q);

        if (matched != null) {
            return String.format(
                    "%s — July safety alerts: %,d total (%d Sev-1, %d panic). %s",
                    matched.getDisplayName(),
                    matched.getSafetyIncidentCount(),
                    matched.getSev1Count(),
                    matched.getPanicCount(),
                    matched.getSev1Count() >= 10
                            ? "Recommend vendor safety review and compliance check."
                            : "Monitor trend in Facilities Head aggregate view.");
        }

        String fleetSummary = narrationClient.narrateSafetyEscalation(safety);
        String topVendors = benchmarkingService.listAllVendorMetrics().stream()
                .filter(v -> v.getSafetyIncidentCount() > 0)
                .sorted(Comparator.comparingLong(VendorSummaryDto::getSev1Count).reversed())
                .limit(3)
                .map(v -> String.format(
                        "%s: %,d alerts (%d Sev-1)",
                        v.getDisplayName(),
                        v.getSafetyIncidentCount(),
                        v.getSev1Count()))
                .collect(Collectors.joining("; "));

        return fleetSummary + (topVendors.isBlank() ? "" : "\n\nTop vendors by Sev-1: " + topVendors + ".");
    }

    private String facilitiesSummaryReply() {
        FacilitiesSummaryDto s = benchmarkingService.buildFacilitiesSummary();
        return String.format(
                "Fleet aggregate (July): OTA %.1f%%, cost %s, %.0f/km across %,d km. "
                        + "Safety alerts: %,d (%d Sev-1, %d panic). %d of %d vendors below SLA. "
                        + "Highest cost/km: %s. Lowest OTA: %s (%.1f%%). "
                        + "AI ops spend (%s): %s (%d API calls).",
                s.getFleetOtaPct(),
                formatCost(s.getTotalCost()),
                s.getCostPerKm() != null ? s.getCostPerKm().doubleValue() : 0,
                s.getTotalKm() != null ? s.getTotalKm().longValue() : 0,
                s.getSafetyIncidentCount(),
                s.getSev1Count(),
                s.getPanicCount(),
                s.getVendorsBelowSla(),
                s.getVendorCount(),
                s.getHighestCostPerKmVendor() != null ? s.getHighestCostPerKmVendor() : "N/A",
                s.getLowestOtaVendor() != null ? s.getLowestOtaVendor() : "N/A",
                s.getLowestOtaPct(),
                s.getAiCostMonth() != null ? s.getAiCostMonth() : "this month",
                formatAiCost(s.getAiMonthlyCostInr()),
                s.getAiMonthlyRequestCount());
    }

    private String vendorScorecardReply(String q) {
        VendorSummaryDto matched = matchVendor(q);
        if (matched != null && !containsAny(q, "list", "all", "scorecard", "vendors")) {
            return String.format(
                    "%s: OTA %.1f%% (SLA %.0f%%), %,d trips, cost %s, %.0f/km, %,d safety alerts (%d Sev-1)%s",
                    matched.getDisplayName(),
                    matched.getOtaPct(),
                    matched.getSlaOtaPct(),
                    matched.getTripCount(),
                    formatCost(matched.getTotalCost()),
                    matched.getCostPerKm() != null ? matched.getCostPerKm().doubleValue() : 0,
                    matched.getSafetyIncidentCount(),
                    matched.getSev1Count(),
                    matched.isSlaBreach() ? " — SLA BREACH" : "");
        }

        return benchmarkingService.listAllVendorMetrics().stream()
                .limit(10)
                .map(v -> String.format(
                        "%s: OTA %.1f%%, %,d trips, %s, %,d safety alerts%s",
                        v.getDisplayName(),
                        v.getOtaPct(),
                        v.getTripCount(),
                        formatCost(v.getTotalCost()),
                        v.getSafetyIncidentCount(),
                        v.isSlaBreach() ? " — BREACH" : ""))
                .collect(Collectors.joining("\n"))
                + "\n\nSee full scorecard in the Vendors tab.";
    }

    private VendorSummaryDto matchVendor(String q) {
        for (VendorSummaryDto vendor : benchmarkingService.listAllVendorMetrics()) {
            String id = vendor.getVendorId().toLowerCase(Locale.ROOT);
            String display = vendor.getDisplayName().toLowerCase(Locale.ROOT);
            String firstName = id.split(" ")[0];
            if (q.contains(firstName) || q.contains(display) || q.contains(id)) {
                return vendor;
            }
        }
        return null;
    }

    private String openAiReply(String message) {
        String system = """
                You are Ops Pulse, an enterprise mobility operations copilot for MoveInSync.
                You ONLY answer questions about: vendor OTA/SLA, safety alerts, trip delays, ETA misses/slips,
                live monitoring events, transport costs, fleet aggregates, vendor scorecards, pending actions,
                findings, leadership memo, and route/office coverage.
                Answer using ONLY the DATA CONTEXT below. Be concise (2-5 sentences).
                Use ₹ for costs. Mention specific vendor/travel names and numbers when relevant.
                For ETA missed questions, cite RECENT LIVE ETA EVENTS and vendor delay leaders from context.
                If the question is outside enterprise employee mobility operations, respond with exactly: OUT_OF_SCOPE
                If the answer is not in the context but the topic is in scope, say what data is missing.
                """;
        String user = "DATA CONTEXT:\n" + buildChatContext() + "\n\nUSER QUESTION:\n" + message;
        String reply = openAiClient.chatText(system, user).getContent().trim();
        if (reply.equalsIgnoreCase("OUT_OF_SCOPE") || reply.toUpperCase(Locale.ROOT).startsWith("OUT_OF_SCOPE")) {
            return outOfScopeMessage();
        }
        return reply;
    }

    private String buildChatContext() {
        VendorBenchmark focus = benchmarkingService.benchmarkFocusVendor();
        SafetyInsight safety = operationalInsightsService.analyzeSafety();
        FacilitiesSummaryDto fleet = benchmarkingService.buildFacilitiesSummary();
        BriefResponse brief = briefController.getBrief();

        String topSafetyVendors = benchmarkingService.listAllVendorMetrics().stream()
                .filter(v -> v.getSafetyIncidentCount() > 0)
                .sorted(Comparator.comparingLong(VendorSummaryDto::getSev1Count).reversed())
                .limit(5)
                .map(v -> String.format(
                        "%s: %,d alerts, %d Sev-1, %d panic, OTA %.1f%%, cost/km %.0f",
                        v.getDisplayName(),
                        v.getSafetyIncidentCount(),
                        v.getSev1Count(),
                        v.getPanicCount(),
                        v.getOtaPct(),
                        v.getCostPerKm() != null ? v.getCostPerKm().doubleValue() : 0))
                .collect(Collectors.joining("\n"));

        String findings = brief.getFindings().stream()
                .map(f -> f.getType() + ": " + f.getNarration())
                .collect(Collectors.joining("\n"));

        String actions = brief.getPendingActions().stream()
                .map(a -> a.getActionType() + ": " + a.getDraftedMessage())
                .collect(Collectors.joining("\n"));

        String etaEvents = monitoringService.buildRecentEtaSummaryForChat();
        String delayLeaders = benchmarkingService.topVendorsByDelayedTrips(5).stream()
                .collect(Collectors.joining("\n"));

        return String.format("""
                FOCUS VENDOR: %s OTA %.1f%% vs SLA %.0f%%, prior month %.1f%%, peer %s %.1f%%, cost %s, trips %,d
                FLEET: OTA %.1f%%, total cost %s, %.0f/km, %,d km, %d/%d vendors below SLA
                SAFETY (July alerts): %,d total, %d Sev-1, %d panic, %d open high-severity
                TOP SAFETY EVENT TYPES: %s
                VENDOR SAFETY LEADERS:
                %s
                RECENT LIVE ETA EVENTS:
                %s
                VENDOR DELAY LEADERS (July delayed trips):
                %s
                FINDINGS:
                %s
                PENDING ACTIONS:
                %s
                LEADERSHIP MEMO EXCERPT: %s
                """,
                focus.getVendorDisplayName(),
                focus.getOtaPct(),
                focus.getSlaOtaPct(),
                focus.getPriorMonthOtaPct() != null ? focus.getPriorMonthOtaPct() : 0,
                focus.getPeerVendorName(),
                focus.getPeerOtaPct() != null ? focus.getPeerOtaPct() : 0,
                formatCost(focus.getTotalCost()),
                focus.getTripCount(),
                fleet.getFleetOtaPct(),
                formatCost(fleet.getTotalCost()),
                fleet.getCostPerKm() != null ? fleet.getCostPerKm().doubleValue() : 0,
                fleet.getTotalKm() != null ? fleet.getTotalKm().longValue() : 0,
                fleet.getVendorsBelowSla(),
                fleet.getVendorCount(),
                safety.getJulyAlertCount(),
                safety.getSev1Count(),
                safety.getPanicCount(),
                safety.getOpenHighSeverityCount(),
                safety.getTopEventTypes().entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(", ")),
                topSafetyVendors.isBlank() ? "(none)" : topSafetyVendors,
                etaEvents,
                delayLeaders.isBlank() ? "(none)" : delayLeaders,
                findings.isBlank() ? "(none)" : findings,
                actions.isBlank() ? "(none)" : actions,
                truncate(agentOrchestrator.getLeadershipMemo(), 400));
    }

    private String truncate(String text, int max) {
        if (text == null || text.isBlank()) {
            return "(not generated)";
        }
        return text.length() > max ? text.substring(0, max) + "…" : text;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String formatCost(BigDecimal cost) {
        if (cost == null) {
            return "₹0";
        }
        double value = cost.doubleValue();
        if (value >= 1_000_000) {
            return String.format("₹%.1fM", value / 1_000_000);
        }
        return String.format("₹%,.0f", value);
    }

    private String formatAiCost(BigDecimal cost) {
        if (cost == null) {
            return "₹0";
        }
        double value = cost.doubleValue();
        if (value >= 1) {
            return String.format("₹%.2f", value);
        }
        if (value >= 0.01) {
            return String.format("₹%.4f", value);
        }
        return String.format("₹%.6f", value);
    }

    private String helpText() {
        String openAiNote = openAiClient.isConfigured()
                ? "\nOpenAI is enabled for in-scope ops questions."
                : "";
        return "Ask me about transport ops in plain language:\n"
                + "• \"What's the OTA for Rohan Travel?\"\n"
                + "• \"Recent ETA missed by which travel?\"\n"
                + "• \"How many safety alerts in July?\"\n"
                + "• \"Safety alerts for Priya Travel\"\n"
                + "• \"What's the fleet cost per km?\"\n"
                + "• \"Show facilities aggregate summary\"\n"
                + "• \"Any pending actions?\"\n"
                + "• \"Show leadership memo\"\n"
                + "Use the mic button for voice input (Sarvam speech-to-text)."
                + openAiNote;
    }

    private boolean isInScope(String q) {
        return containsAny(q,
                "ota", "sla", "safety", "alert", "vendor", "travel", "trip", "shuttle", "cab", "fleet",
                "transport", "mobility", "cost", "delay", "action", "finding", "memo", "leadership",
                "facilities", "office", "campus", "route", "driver", "capacity", "monitoring", "escalate",
                "breach", "panic", "sev", "incident", "rohan", "priya", "moveinsync", "ops pulse", "opspulse",
                "july", "june", "km", "billing", "spend", "help", "what can", "scorecard", "aggregate",
                "summary", "performance", "on-time", "on time", "confirm", "pending", "issue", "problem",
                "security", "exposure", "rupee", "crore", "nodal", "pickup", "shuttle", "employee commute",
                "eta", "missed", "recent", "live", "slip", "late");
    }

    private boolean isClearlyOutOfScope(String q) {
        boolean offTopic = containsAny(q,
                "recipe", "pizza", "cook ", "movie", "song", "lyrics", "poem", "write code", "python code",
                "javascript", "debug this", "capital of", "who won", "cricket", "football", "basketball",
                "ipl", "world cup", "weather forecast", "temperature today", "stock price", "bitcoin",
                "crypto", "dating", "relationship advice", "medical advice", "diagnose", "homework", "essay",
                "translate to", "tell me a joke", "play a game", "celebrity", "netflix", "amazon prime");
        return offTopic && !isInScope(q);
    }

    private String outOfScopeMessage() {
        return "That's out of scope for me — I'm Ops Pulse, your enterprise mobility ops assistant. "
                + "I can help with OTA, safety alerts, vendor scorecards, costs, fleet summary, actions, and the leadership memo. "
                + "Try: \"How many safety alerts in July?\" or \"What's the fleet OTA?\"";
    }

    private String inScopeUnmatchedMessage() {
        if (openAiClient.isConfigured()) {
            return "I couldn't answer that from the available ops data. Try rephrasing, or ask about OTA, "
                    + "ETA misses, safety alerts, vendor scorecards, costs, or pending actions.";
        }
        return "I understand this is an ops question. Configure OPENAI_API_KEY for natural-language answers, "
                + "or try: \"Recent ETA missed by which travel?\", \"What's the fleet OTA?\", "
                + "\"Safety alerts for Priya Travel\".";
    }
}
