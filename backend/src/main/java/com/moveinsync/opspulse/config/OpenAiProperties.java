package com.moveinsync.opspulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opspulse.openai")
public class OpenAiProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private double inputCostPer1kTokensUsd = 0.00015;
    private double outputCostPer1kTokensUsd = 0.00060;
    private double usdToInr = 84.0;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && !apiKey.equals("your-openai-api-key-here");
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

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getInputCostPer1kTokensUsd() {
        return inputCostPer1kTokensUsd;
    }

    public void setInputCostPer1kTokensUsd(double inputCostPer1kTokensUsd) {
        this.inputCostPer1kTokensUsd = inputCostPer1kTokensUsd;
    }

    public double getOutputCostPer1kTokensUsd() {
        return outputCostPer1kTokensUsd;
    }

    public void setOutputCostPer1kTokensUsd(double outputCostPer1kTokensUsd) {
        this.outputCostPer1kTokensUsd = outputCostPer1kTokensUsd;
    }

    public double getUsdToInr() {
        return usdToInr;
    }

    public void setUsdToInr(double usdToInr) {
        this.usdToInr = usdToInr;
    }
}
