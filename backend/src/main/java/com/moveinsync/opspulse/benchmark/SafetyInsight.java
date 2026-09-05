package com.moveinsync.opspulse.benchmark;

import java.util.LinkedHashMap;
import java.util.Map;

public class SafetyInsight {

    private long julyAlertCount;
    private long sev1Count;
    private long panicCount;
    private long openHighSeverityCount;
    private Map<String, Long> topEventTypes = new LinkedHashMap<>();

    public long getJulyAlertCount() {
        return julyAlertCount;
    }

    public void setJulyAlertCount(long julyAlertCount) {
        this.julyAlertCount = julyAlertCount;
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

    public long getOpenHighSeverityCount() {
        return openHighSeverityCount;
    }

    public void setOpenHighSeverityCount(long openHighSeverityCount) {
        this.openHighSeverityCount = openHighSeverityCount;
    }

    public Map<String, Long> getTopEventTypes() {
        return topEventTypes;
    }

    public void setTopEventTypes(Map<String, Long> topEventTypes) {
        this.topEventTypes = topEventTypes;
    }

    public boolean requiresEscalation() {
        return sev1Count >= 10 || panicCount >= 5;
    }
}
