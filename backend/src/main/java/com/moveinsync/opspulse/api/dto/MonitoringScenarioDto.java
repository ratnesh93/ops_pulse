package com.moveinsync.opspulse.api.dto;

public class MonitoringScenarioDto {

    private String id;
    private String label;
    private String sentiment;
    private String description;

    public MonitoringScenarioDto() {
    }

    public MonitoringScenarioDto(String id, String label, String sentiment, String description) {
        this.id = id;
        this.label = label;
        this.sentiment = sentiment;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
