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
}
