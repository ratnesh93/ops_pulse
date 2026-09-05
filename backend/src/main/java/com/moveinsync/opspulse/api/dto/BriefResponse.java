package com.moveinsync.opspulse.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class BriefResponse {

    private KpiBar kpis;
    private List<FindingDto> findings;
    private List<ActionDto> pendingActions;

    public KpiBar getKpis() {
        return kpis;
    }

    public void setKpis(KpiBar kpis) {
        this.kpis = kpis;
    }

    public List<FindingDto> getFindings() {
        return findings;
    }

    public void setFindings(List<FindingDto> findings) {
        this.findings = findings;
    }

    public List<ActionDto> getPendingActions() {
        return pendingActions;
    }

    public void setPendingActions(List<ActionDto> pendingActions) {
        this.pendingActions = pendingActions;
    }

    public static class KpiBar {
        private double otaPct;
        private double slaOtaPct;
        private Double priorMonthOtaPct;
        private double otaDeltaVsPriorMonth;
        private BigDecimal totalCost;
        private long tripCount;
        private String vendorDisplayName;

        public double getOtaPct() {
            return otaPct;
        }

        public void setOtaPct(double otaPct) {
            this.otaPct = otaPct;
        }

        public double getSlaOtaPct() {
            return slaOtaPct;
        }

        public void setSlaOtaPct(double slaOtaPct) {
            this.slaOtaPct = slaOtaPct;
        }

        public Double getPriorMonthOtaPct() {
            return priorMonthOtaPct;
        }

        public void setPriorMonthOtaPct(Double priorMonthOtaPct) {
            this.priorMonthOtaPct = priorMonthOtaPct;
        }

        public double getOtaDeltaVsPriorMonth() {
            return otaDeltaVsPriorMonth;
        }

        public void setOtaDeltaVsPriorMonth(double otaDeltaVsPriorMonth) {
            this.otaDeltaVsPriorMonth = otaDeltaVsPriorMonth;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(BigDecimal totalCost) {
            this.totalCost = totalCost;
        }

        public long getTripCount() {
            return tripCount;
        }

        public void setTripCount(long tripCount) {
            this.tripCount = tripCount;
        }

        public String getVendorDisplayName() {
            return vendorDisplayName;
        }

        public void setVendorDisplayName(String vendorDisplayName) {
            this.vendorDisplayName = vendorDisplayName;
        }
    }

    public static class FindingDto {
        private Long id;
        private String type;
        private String severity;
        private String narration;
        private Map<String, Object> metrics;
        private Map<String, Object> benchmarks;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getNarration() {
            return narration;
        }

        public void setNarration(String narration) {
            this.narration = narration;
        }

        public Map<String, Object> getMetrics() {
            return metrics;
        }

        public void setMetrics(Map<String, Object> metrics) {
            this.metrics = metrics;
        }

        public Map<String, Object> getBenchmarks() {
            return benchmarks;
        }

        public void setBenchmarks(Map<String, Object> benchmarks) {
            this.benchmarks = benchmarks;
        }
    }

    public static class ActionDto {
        private Long id;
        private Long findingId;
        private String actionType;
        private String draftedMessage;
        private String status;
        private Map<String, Object> payload;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getFindingId() {
            return findingId;
        }

        public void setFindingId(Long findingId) {
            this.findingId = findingId;
        }

        public String getActionType() {
            return actionType;
        }

        public void setActionType(String actionType) {
            this.actionType = actionType;
        }

        public String getDraftedMessage() {
            return draftedMessage;
        }

        public void setDraftedMessage(String draftedMessage) {
            this.draftedMessage = draftedMessage;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public void setPayload(Map<String, Object> payload) {
            this.payload = payload;
        }
    }
}
