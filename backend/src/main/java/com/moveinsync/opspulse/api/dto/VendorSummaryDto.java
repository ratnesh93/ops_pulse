package com.moveinsync.opspulse.api.dto;

import java.math.BigDecimal;

public class VendorSummaryDto {

    private String vendorId;
    private String displayName;
    private double otaPct;
    private double slaOtaPct;
    private Double priorMonthOtaPct;
    private BigDecimal totalCost;
    private long tripCount;
    private boolean slaBreach;
    private boolean focusVendor;
    private int otaRank;
    private int vendorCount;
    private Double peerGapPct;
    private BigDecimal costPerOnTimeTrip;
    private long onTimeTripCount;
    private BigDecimal costPerKm;
    private long safetyIncidentCount;
    private long sev1Count;
    private long panicCount;

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

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

    public boolean isSlaBreach() {
        return slaBreach;
    }

    public void setSlaBreach(boolean slaBreach) {
        this.slaBreach = slaBreach;
    }

    public boolean isFocusVendor() {
        return focusVendor;
    }

    public void setFocusVendor(boolean focusVendor) {
        this.focusVendor = focusVendor;
    }

    public int getOtaRank() {
        return otaRank;
    }

    public void setOtaRank(int otaRank) {
        this.otaRank = otaRank;
    }

    public int getVendorCount() {
        return vendorCount;
    }

    public void setVendorCount(int vendorCount) {
        this.vendorCount = vendorCount;
    }

    public Double getPeerGapPct() {
        return peerGapPct;
    }

    public void setPeerGapPct(Double peerGapPct) {
        this.peerGapPct = peerGapPct;
    }

    public BigDecimal getCostPerOnTimeTrip() {
        return costPerOnTimeTrip;
    }

    public void setCostPerOnTimeTrip(BigDecimal costPerOnTimeTrip) {
        this.costPerOnTimeTrip = costPerOnTimeTrip;
    }

    public long getOnTimeTripCount() {
        return onTimeTripCount;
    }

    public void setOnTimeTripCount(long onTimeTripCount) {
        this.onTimeTripCount = onTimeTripCount;
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
}
