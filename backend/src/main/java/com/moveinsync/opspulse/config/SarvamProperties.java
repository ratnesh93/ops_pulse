package com.moveinsync.opspulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opspulse.sarvam")
public class SarvamProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.sarvam.ai";
    private String sttModel = "saaras:v3";
    private String languageCode = "en-IN";

    /** INR per minute of audio (primary STT pricing). Set to 0 to use per-token rates below. */
    private double sttCostPerMinuteInr = 0.30;
    private double sttInputCostPer1kTokensInr = 0.0;
    private double sttOutputCostPer1kTokensInr = 0.0;
    private int sttTokensPerAudioSecond = 25;
    private int sttBytesPerSecondEstimate = 16_000;

    /** Future LLM pricing (INR per 1K tokens). */
    private double llmInputCostPer1kTokensInr = 0.50;
    private double llmOutputCostPer1kTokensInr = 1.50;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && !apiKey.equals("your-sarvam-api-key-here");
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSttModel() {
        return sttModel;
    }

    public void setSttModel(String sttModel) {
        this.sttModel = sttModel;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public double getSttCostPerMinuteInr() {
        return sttCostPerMinuteInr;
    }

    public void setSttCostPerMinuteInr(double sttCostPerMinuteInr) {
        this.sttCostPerMinuteInr = sttCostPerMinuteInr;
    }

    public double getSttInputCostPer1kTokensInr() {
        return sttInputCostPer1kTokensInr;
    }

    public void setSttInputCostPer1kTokensInr(double sttInputCostPer1kTokensInr) {
        this.sttInputCostPer1kTokensInr = sttInputCostPer1kTokensInr;
    }

    public double getSttOutputCostPer1kTokensInr() {
        return sttOutputCostPer1kTokensInr;
    }

    public void setSttOutputCostPer1kTokensInr(double sttOutputCostPer1kTokensInr) {
        this.sttOutputCostPer1kTokensInr = sttOutputCostPer1kTokensInr;
    }

    public int getSttTokensPerAudioSecond() {
        return sttTokensPerAudioSecond;
    }

    public void setSttTokensPerAudioSecond(int sttTokensPerAudioSecond) {
        this.sttTokensPerAudioSecond = sttTokensPerAudioSecond;
    }

    public int getSttBytesPerSecondEstimate() {
        return sttBytesPerSecondEstimate;
    }

    public void setSttBytesPerSecondEstimate(int sttBytesPerSecondEstimate) {
        this.sttBytesPerSecondEstimate = sttBytesPerSecondEstimate;
    }

    public double getLlmInputCostPer1kTokensInr() {
        return llmInputCostPer1kTokensInr;
    }

    public void setLlmInputCostPer1kTokensInr(double llmInputCostPer1kTokensInr) {
        this.llmInputCostPer1kTokensInr = llmInputCostPer1kTokensInr;
    }

    public double getLlmOutputCostPer1kTokensInr() {
        return llmOutputCostPer1kTokensInr;
    }

    public void setLlmOutputCostPer1kTokensInr(double llmOutputCostPer1kTokensInr) {
        this.llmOutputCostPer1kTokensInr = llmOutputCostPer1kTokensInr;
    }
}
