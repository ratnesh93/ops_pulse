package com.moveinsync.opspulse.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class FacilitiesSummaryDto {

    private double fleetOtaPct;
    private BigDecimal totalCost;
    private BigDecimal totalKm;
    private BigDecimal costPerKm;
    private long safetyIncidentCount;
    private long sev1Count;
    private long panicCount;
    private int vendorCount;
    private int vendorsBelowSla;
    private String highestCostPerKmVendor;
    private BigDecimal highestCostPerKm;
    private String lowestOtaVendor;
    private double lowestOtaPct;
    private BigDecimal aiMonthlyCostInr;
    private long aiMonthlyRequestCount;
    private String aiCostMonth;
    private List<AiProviderMonthlyCost> aiCostByProvider;

    public double getFleetOtaPct() {
        return fleetOtaPct;
    }

    public void setFleetOtaPct(double fleetOtaPct) {
        this.fleetOtaPct = fleetOtaPct;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getTotalKm() {
        return totalKm;
    }

    public void setTotalKm(BigDecimal totalKm) {
        this.totalKm = totalKm;
    }

    public BigDecimal getCostPerKm() {
        return costPerKm;
    }

    public void setCostPerKm(BigDecimal costPerKm) {
        this.costPerKm = costPerKm;
    }

    public long getSafetyIncidentCount() {
        return safetyIncidentCount;
    }

    public void setSafetyIncidentCount(long safetyIncidentCount) {
        this.safetyIncidentCount = safetyIncidentCount;
    }

    public long getSev1Count() {
        return sev1Count;
    }

    public void setSev1Count(long sev1Count) {
        this.sev1Count = sev1Count;
    }

    public long getPanicCount() {
        return panicCount;
    }

    public void setPanicCount(long panicCount) {
        this.panicCount = panicCount;
    }

    public int getVendorCount() {
        return vendorCount;
    }

    public void setVendorCount(int vendorCount) {
        this.vendorCount = vendorCount;
    }

    public int getVendorsBelowSla() {
        return vendorsBelowSla;
    }

    public void setVendorsBelowSla(int vendorsBelowSla) {
        this.vendorsBelowSla = vendorsBelowSla;
    }

    public String getHighestCostPerKmVendor() {
        return highestCostPerKmVendor;
    }

    public void setHighestCostPerKmVendor(String highestCostPerKmVendor) {
        this.highestCostPerKmVendor = highestCostPerKmVendor;
    }

    public BigDecimal getHighestCostPerKm() {
        return highestCostPerKm;
    }

    public void setHighestCostPerKm(BigDecimal highestCostPerKm) {
        this.highestCostPerKm = highestCostPerKm;
    }

    public String getLowestOtaVendor() {
        return lowestOtaVendor;
    }

    public void setLowestOtaVendor(String lowestOtaVendor) {
        this.lowestOtaVendor = lowestOtaVendor;
    }

    public double getLowestOtaPct() {
        return lowestOtaPct;
    }

    public void setLowestOtaPct(double lowestOtaPct) {
        this.lowestOtaPct = lowestOtaPct;
    }

    public BigDecimal getAiMonthlyCostInr() {
        return aiMonthlyCostInr;
    }

    public void setAiMonthlyCostInr(BigDecimal aiMonthlyCostInr) {
        this.aiMonthlyCostInr = aiMonthlyCostInr;
    }

    public long getAiMonthlyRequestCount() {
        return aiMonthlyRequestCount;
    }

    public void setAiMonthlyRequestCount(long aiMonthlyRequestCount) {
        this.aiMonthlyRequestCount = aiMonthlyRequestCount;
    }

    public String getAiCostMonth() {
        return aiCostMonth;
    }

    public void setAiCostMonth(String aiCostMonth) {
        this.aiCostMonth = aiCostMonth;
    }

    public List<AiProviderMonthlyCost> getAiCostByProvider() {
        return aiCostByProvider;
    }

    public void setAiCostByProvider(List<AiProviderMonthlyCost> aiCostByProvider) {
        this.aiCostByProvider = aiCostByProvider;
    }

    public static class AiProviderMonthlyCost {
        private String provider;
        private String providerLabel;
        private long requestCount;
        private long inputTokens;
        private long outputTokens;
        private BigDecimal costInr;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getProviderLabel() {
            return providerLabel;
        }

        public void setProviderLabel(String providerLabel) {
            this.providerLabel = providerLabel;
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
}
