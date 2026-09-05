package com.moveinsync.opspulse.chat;

import com.moveinsync.opspulse.agent.AgentOrchestrator;
import com.moveinsync.opspulse.api.dto.BriefResponse;
import com.moveinsync.opspulse.api.BriefController;
import com.moveinsync.opspulse.benchmark.BenchmarkingService;
import com.moveinsync.opspulse.benchmark.VendorBenchmark;
import com.moveinsync.opspulse.narration.NarrationClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final BenchmarkingService benchmarkingService;
    private final NarrationClient narrationClient;
    private final BriefController briefController;
    private final AgentOrchestrator agentOrchestrator;

    public ChatService(
            BenchmarkingService benchmarkingService,
            NarrationClient narrationClient,
            BriefController briefController,
            AgentOrchestrator agentOrchestrator) {
        this.benchmarkingService = benchmarkingService;
        this.narrationClient = narrationClient;
        this.briefController = briefController;
        this.agentOrchestrator = agentOrchestrator;
    }

    public String reply(String message) {
        if (message == null || message.isBlank()) {
            return helpText();
        }

        String q = message.toLowerCase(Locale.ROOT).trim();
        VendorBenchmark vendor = benchmarkingService.benchmarkFocusVendor();
        BriefResponse brief = briefController.getBrief();

        if (containsAny(q, "help", "what can", "how do")) {
            return helpText();
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
        if (containsAny(q, "cost", "spend", "billing", "exposure", "rupee", "crore")) {
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
        if (containsAny(q, "finding", "issue", "problem", "vendor", "rohan")) {
            if (brief.getFindings().isEmpty()) {
                return "No active findings. Try running the agent.";
            }
            return brief.getFindings().get(0).getNarration();
        }
        if (containsAny(q, "memo", "leadership", "executive", "summary")) {
            String memo = agentOrchestrator.getLeadershipMemo();
            if (memo == null || memo.isBlank()) {
                return "Leadership memo not generated yet. Run the agent first.";
            }
            return memo.length() > 600 ? memo.substring(0, 600) + "…" : memo;
        }
        if (containsAny(q, "vendor", "vendors", "list vendor", "all vendor")) {
            return benchmarkingService.listAllVendorMetrics().stream()
                    .limit(10)
                    .map(v -> String.format(
                            "%s: OTA %.1f%% (SLA %.0f%%), %,d trips, %s%s",
                            v.getDisplayName(),
                            v.getOtaPct(),
                            v.getSlaOtaPct(),
                            v.getTripCount(),
                            formatCost(v.getTotalCost()),
                            v.isSlaBreach() ? " — BREACH" : ""))
                    .collect(Collectors.joining("\n"))
                    + "\n\nSee full scorecard in the Vendors tab.";
        }
        if (containsAny(q, "office", "campus", "location")) {
            String office = vendor.getTopAffectedOffice();
            return office != null
                    ? "Top affected office for delays: " + office + "."
                    : "No office concentration data available.";
        }

        return "I can answer questions about OTA, SLA breach, delays, cost, pending actions, and the leadership memo. "
                + "Try: \"What's Rohan's OTA?\" or \"Why should we escalate?\"";
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

    private String helpText() {
        return "Ask me about transport ops in plain language:\n"
                + "• \"What's the OTA for Rohan Travel?\"\n"
                + "• \"Why are trips delayed?\"\n"
                + "• \"What's the cost exposure?\"\n"
                + "• \"Any pending actions?\"\n"
                + "• \"Show leadership memo\"\n"
                + "Use the mic button for voice input (Sarvam speech-to-text).";
    }
}
