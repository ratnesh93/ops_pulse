package com.moveinsync.opspulse.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "vendor_monthly_stats")
@IdClass(VendorMonthlyStat.VendorMonthKey.class)
public class VendorMonthlyStat {

    @Id
    private String vendorId;

    @Id
    private String monthYear;

    private BigDecimal otaPct;
    private Long tripCount;

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }

    public BigDecimal getOtaPct() {
        return otaPct;
    }

    public void setOtaPct(BigDecimal otaPct) {
        this.otaPct = otaPct;
    }

    public Long getTripCount() {
        return tripCount;
    }

    public void setTripCount(Long tripCount) {
        this.tripCount = tripCount;
    }

    public static class VendorMonthKey implements Serializable {
        private String vendorId;
        private String monthYear;

        public VendorMonthKey() {
        }

        public VendorMonthKey(String vendorId, String monthYear) {
            this.vendorId = vendorId;
            this.monthYear = monthYear;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            VendorMonthKey that = (VendorMonthKey) o;
            return Objects.equals(vendorId, that.vendorId) && Objects.equals(monthYear, that.monthYear);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vendorId, monthYear);
        }
    }
}
