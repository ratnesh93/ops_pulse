package com.moveinsync.opspulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opspulse")
public class OpsPulseProperties {

    private String dataPath = "/data/moveinsync";
    private String tripFile = "Ride_data _trip-July_2026.csv";
    private String priorTripFile = "Ride_data _trip-June_2026.csv";
    private String billFile = "bill_data.csv";
    private String alertsFile = "alerts_data.csv";
    private String analysisVendor = "Rohan Mikhailov Travel";
    private String peerVendor = "Priya Mikhailov Travel";
    private double slaOtaPct = 90.0;
    private boolean skipDataLoad = false;
    private String vendorDisplayAlias = "Rohan Travel (Vendor B)";
    private String adminIngestSecret = "";

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getTripFile() {
        return tripFile;
    }

    public void setTripFile(String tripFile) {
        this.tripFile = tripFile;
    }

    public String getPriorTripFile() {
        return priorTripFile;
    }

    public void setPriorTripFile(String priorTripFile) {
        this.priorTripFile = priorTripFile;
    }

    public String getBillFile() {
        return billFile;
    }

    public void setBillFile(String billFile) {
        this.billFile = billFile;
    }

    public String getAlertsFile() {
        return alertsFile;
    }

    public void setAlertsFile(String alertsFile) {
        this.alertsFile = alertsFile;
    }

    public String getAnalysisVendor() {
        return analysisVendor;
    }

    public void setAnalysisVendor(String analysisVendor) {
        this.analysisVendor = analysisVendor;
    }

    public String getPeerVendor() {
        return peerVendor;
    }

    public void setPeerVendor(String peerVendor) {
        this.peerVendor = peerVendor;
    }

    public double getSlaOtaPct() {
        return slaOtaPct;
    }

    public void setSlaOtaPct(double slaOtaPct) {
        this.slaOtaPct = slaOtaPct;
    }

    public boolean isSkipDataLoad() {
        return skipDataLoad;
    }

    public void setSkipDataLoad(boolean skipDataLoad) {
        this.skipDataLoad = skipDataLoad;
    }

    public String getVendorDisplayAlias() {
        return vendorDisplayAlias;
    }

    public void setVendorDisplayAlias(String vendorDisplayAlias) {
        this.vendorDisplayAlias = vendorDisplayAlias;
    }

    public String getAdminIngestSecret() {
        return adminIngestSecret;
    }

    public void setAdminIngestSecret(String adminIngestSecret) {
        this.adminIngestSecret = adminIngestSecret;
    }
}
