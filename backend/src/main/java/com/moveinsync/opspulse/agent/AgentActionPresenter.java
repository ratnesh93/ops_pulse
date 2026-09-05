package com.moveinsync.opspulse.agent;

import com.moveinsync.opspulse.domain.AgentAction;
import com.moveinsync.opspulse.domain.Finding;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class AgentActionPresenter {

    public void enrich(AgentAction action, Finding finding, Map<String, Object> payload, AgentActionView dto) {
        Map<String, Object> safePayload = payload != null ? payload : Collections.emptyMap();
        dto.setTitle(resolveTitle(action, finding, safePayload));
        dto.setSeverity(finding != null && finding.getSeverity() != null ? finding.getSeverity() : "MEDIUM");
        dto.setAiInsight(resolveInsight(action, finding));
        dto.setRecommendedAction(resolveRecommendedAction(action, safePayload));
    }

    private String resolveTitle(AgentAction action, Finding finding, Map<String, Object> payload) {
        String type = action.getActionType();

        if ("ESCALATE_VENDOR".equals(type)) {
            String vendor = stringVal(payload.get("vendorDisplayName"), "focus vendor");
            return "Vendor SLA breach — " + vendor;
        }
        if ("ESCALATE_SAFETY".equals(type)) {
            return "Safety escalation — Sev-1 threshold breached";
        }
        if ("ADD_CAPACITY".equals(type)) {
            String office = stringVal(payload.get("office"), "office");
            String shift = stringVal(payload.get("shiftId"), "shift");
            return "Capacity shortfall — " + office + " · " + shift;
        }
        if (finding != null && finding.getType() != null) {
            return finding.getType().replace('_', ' ');
        }
        return type != null ? type.replace('_', ' ') : "Agent action";
    }

    private String resolveInsight(AgentAction action, Finding finding) {
        if (finding != null && finding.getNarration() != null && !finding.getNarration().isBlank()) {
            return finding.getNarration();
        }
        return action.getDraftedMessage();
    }

    private String resolveRecommendedAction(AgentAction action, Map<String, Object> payload) {
        String type = action.getActionType();

        if ("ESCALATE_VENDOR".equals(type)) {
            String vendor = stringVal(payload.get("vendorDisplayName"), "vendor");
            return "Escalate to " + vendor + " leadership; request corrective action plan within 5 business days "
                    + "with weekly SLA checkpoints.";
        }
        if ("ESCALATE_SAFETY".equals(type)) {
            return "Route to transport safety desk for 24h review; pull vendor compliance records and "
                    + "open incident tracker for top Sev-1 vendors.";
        }
        if ("ADD_CAPACITY".equals(type)) {
            int extra = numberVal(payload.get("extraVehicles"), 1);
            String office = stringVal(payload.get("office"), "site");
            String shift = stringVal(payload.get("shiftId"), "shift");
            int gap = numberVal(payload.get("overbookedSeats"), 0);
            return String.format(
                    "Deploy +%d backup vehicles for %s %s (%d-seat gap); draft vendor extra-trip request and "
                            + "notify site transport lead.",
                    extra, office, shift, gap);
        }
        if (action.getDraftedMessage() != null && !action.getDraftedMessage().isBlank()) {
            return action.getDraftedMessage();
        }
        return "Review finding and confirm next step with the transport desk.";
    }

    public interface AgentActionView {
        void setTitle(String title);
        void setSeverity(String severity);
        void setAiInsight(String aiInsight);
        void setRecommendedAction(String recommendedAction);
    }

    private String stringVal(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private int numberVal(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }
}
