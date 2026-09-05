package com.moveinsync.opspulse.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AiCostSummaryDto {

    private BigDecimal totalCostInr;
    private long totalInputTokens;
    private long totalOutputTokens;
    private long totalRequests;
    private String currency = "INR";
    private List<OperationBreakdown> byOperation;
    private List<UsageEntry> recentUsage;

    public BigDecimal getTotalCostInr() {
        return totalCostInr;
    }

    public void setTotalCostInr(BigDecimal totalCostInr) {
        this.totalCostInr = totalCostInr;
    }

    public long getTotalInputTokens() {
        return totalInputTokens;
    }

    public void setTotalInputTokens(long totalInputTokens) {
        this.totalInputTokens = totalInputTokens;
    }

    public long getTotalOutputTokens() {
        return totalOutputTokens;
    }

    public void setTotalOutputTokens(long totalOutputTokens) {
        this.totalOutputTokens = totalOutputTokens;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<OperationBreakdown> getByOperation() {
        return byOperation;
    }

    public void setByOperation(List<OperationBreakdown> byOperation) {
        this.byOperation = byOperation;
    }

    public List<UsageEntry> getRecentUsage() {
        return recentUsage;
    }

    public void setRecentUsage(List<UsageEntry> recentUsage) {
        this.recentUsage = recentUsage;
    }

    public static class OperationBreakdown {
        private String operationType;
        private long requestCount;
        private long inputTokens;
        private long outputTokens;
        private BigDecimal costInr;

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public void setRequestCount(long requestCount) {
            this.requestCount = requestCount;
        }

        public long getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(long inputTokens) {
            this.inputTokens = inputTokens;
        }

        public long getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(long outputTokens) {
            this.outputTokens = outputTokens;
        }

        public BigDecimal getCostInr() {
            return costInr;
        }

        public void setCostInr(BigDecimal costInr) {
            this.costInr = costInr;
        }
    }

    public static class UsageEntry {
        private Long id;
        private String operationType;
        private String provider;
        private String model;
        private int inputTokens;
        private int outputTokens;
        private BigDecimal costInr;
        private Integer audioDurationMs;
        private Instant createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(int inputTokens) {
            this.inputTokens = inputTokens;
        }

        public int getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(int outputTokens) {
            this.outputTokens = outputTokens;
        }

        public BigDecimal getCostInr() {
            return costInr;
        }

        public void setCostInr(BigDecimal costInr) {
            this.costInr = costInr;
        }

        public Integer getAudioDurationMs() {
            return audioDurationMs;
        }

        public void setAudioDurationMs(Integer audioDurationMs) {
            this.audioDurationMs = audioDurationMs;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }
}
