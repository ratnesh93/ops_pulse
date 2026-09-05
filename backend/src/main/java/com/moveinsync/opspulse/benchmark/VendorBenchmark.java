package com.moveinsync.opspulse.benchmark;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class VendorBenchmark {

    private String vendorId;
    private String vendorDisplayName;
    private double otaPct;
    private double slaOtaPct;
    private Double priorMonthOtaPct;
    private Double peerOtaPct;
    private String peerVendorName;
    private BigDecimal totalCost;
    private long tripCount;
    private String topAffectedOffice;
    private Map<String, Double> delayAttribution = new LinkedHashMap<>();

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorDisplayName() {
        return vendorDisplayName;
    }

    public void setVendorDisplayName(String vendorDisplayName) {
        this.vendorDisplayName = vendorDisplayName;
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

    public Double getPeerOtaPct() {
        return peerOtaPct;
    }

    public void setPeerOtaPct(Double peerOtaPct) {
        this.peerOtaPct = peerOtaPct;
    }

    public String getPeerVendorName() {
        return peerVendorName;
    }

    public void setPeerVendorName(String peerVendorName) {
        this.peerVendorName = peerVendorName;
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

    public String getTopAffectedOffice() {
        return topAffectedOffice;
    }

    public void setTopAffectedOffice(String topAffectedOffice) {
        this.topAffectedOffice = topAffectedOffice;
    }

    public Map<String, Double> getDelayAttribution() {
        return delayAttribution;
    }

    public void setDelayAttribution(Map<String, Double> delayAttribution) {
        this.delayAttribution = delayAttribution;
    }

    public boolean isSlaBreach() {
        return otaPct < slaOtaPct;
    }
}
