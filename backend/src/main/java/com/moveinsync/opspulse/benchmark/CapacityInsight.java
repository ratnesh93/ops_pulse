package com.moveinsync.opspulse.benchmark;

public class CapacityInsight {

    private String office;
    private String shiftId;
    private long totalRiders;
    private long totalSeats;
    private long overbookedSeats;
    private long overbookedTrips;

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

    public long getTotalRiders() {
        return totalRiders;
    }

    public void setTotalRiders(long totalRiders) {
        this.totalRiders = totalRiders;
    }

    public long getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(long totalSeats) {
        this.totalSeats = totalSeats;
    }

    public long getOverbookedSeats() {
        return overbookedSeats;
    }

    public void setOverbookedSeats(long overbookedSeats) {
        this.overbookedSeats = overbookedSeats;
    }

    public long getOverbookedTrips() {
        return overbookedTrips;
    }

    public void setOverbookedTrips(long overbookedTrips) {
        this.overbookedTrips = overbookedTrips;
    }

    public boolean hasOverbooking() {
        return overbookedSeats > 0;
    }

    public int recommendedExtraVehicles() {
        if (overbookedSeats <= 0) {
            return 0;
        }
        return (int) Math.min(4, Math.max(2, overbookedSeats / 50 + 1));
    }
}
