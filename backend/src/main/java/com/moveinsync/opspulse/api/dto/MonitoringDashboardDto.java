package com.moveinsync.opspulse.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class MonitoringDashboardDto {

    private boolean openAiConfigured;
    private int pendingCount;
    private List<FeedEventDto> feedEvents;
    private List<ActionItemDto> actionItems;

    public boolean isOpenAiConfigured() {
        return openAiConfigured;
    }

    public void setOpenAiConfigured(boolean openAiConfigured) {
        this.openAiConfigured = openAiConfigured;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public List<FeedEventDto> getFeedEvents() {
        return feedEvents;
    }

    public void setFeedEvents(List<FeedEventDto> feedEvents) {
        this.feedEvents = feedEvents;
    }

    public List<ActionItemDto> getActionItems() {
        return actionItems;
    }

    public void setActionItems(List<ActionItemDto> actionItems) {
        this.actionItems = actionItems;
    }

    public static class FeedEventDto {
        private Long id;
        private String sentiment;
        private String eventType;
        private String title;
        private String detail;
        private String office;
        private String shiftId;
        private BigDecimal metricValue;
        private String source;
        private Instant createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getSentiment() {
            return sentiment;
        }

        public void setSentiment(String sentiment) {
            this.sentiment = sentiment;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public String getOffice() {
            return office;
        }

        public void setOffice(String office) {
            this.office = office;
        }

        public String getShiftId() {
            return shiftId;
        }

        public void setShiftId(String shiftId) {
            this.shiftId = shiftId;
        }

        public BigDecimal getMetricValue() {
            return metricValue;
        }

        public void setMetricValue(BigDecimal metricValue) {
            this.metricValue = metricValue;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class ActionItemDto {
        private Long id;
        private Long eventId;
        private String severity;
        private String title;
        private String aiInsight;
        private String recommendedAction;
        private String actionType;
        private String status;
        private String openaiModel;
        private Instant createdAt;
        private Instant confirmedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getEventId() {
            return eventId;
        }

        public void setEventId(Long eventId) {
            this.eventId = eventId;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAiInsight() {
            return aiInsight;
        }

        public void setAiInsight(String aiInsight) {
            this.aiInsight = aiInsight;
        }

        public String getRecommendedAction() {
            return recommendedAction;
        }

        public void setRecommendedAction(String recommendedAction) {
            this.recommendedAction = recommendedAction;
        }

        public String getActionType() {
            return actionType;
        }

        public void setActionType(String actionType) {
            this.actionType = actionType;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getOpenaiModel() {
            return openaiModel;
        }

        public void setOpenaiModel(String openaiModel) {
            this.openaiModel = openaiModel;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getConfirmedAt() {
            return confirmedAt;
        }

        public void setConfirmedAt(Instant confirmedAt) {
            this.confirmedAt = confirmedAt;
        }
    }
}
