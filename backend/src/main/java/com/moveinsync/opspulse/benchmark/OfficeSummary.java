package com.moveinsync.opspulse.benchmark;

public class OfficeSummary {

    private String office;
    private long tripCount;
    private long delayedCount;
    private double otaPct;
    private long loginRouteCount;
    private long logoutRouteCount;

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }

    public long getTripCount() {
        return tripCount;
    }

    public void setTripCount(long tripCount) {
        this.tripCount = tripCount;
    }

    public long getDelayedCount() {
        return delayedCount;
    }

    public void setDelayedCount(long delayedCount) {
        this.delayedCount = delayedCount;
    }

    public double getOtaPct() {
        return otaPct;
    }

    public void setOtaPct(double otaPct) {
        this.otaPct = otaPct;
    }

    public long getLoginRouteCount() {
        return loginRouteCount;
    }

    public void setLoginRouteCount(long loginRouteCount) {
        this.loginRouteCount = loginRouteCount;
    }

    public long getLogoutRouteCount() {
        return logoutRouteCount;
    }

    public void setLogoutRouteCount(long logoutRouteCount) {
        this.logoutRouteCount = logoutRouteCount;
    }
}
