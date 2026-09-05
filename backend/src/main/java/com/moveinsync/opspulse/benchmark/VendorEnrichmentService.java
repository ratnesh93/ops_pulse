package com.moveinsync.opspulse.benchmark;

import com.moveinsync.opspulse.config.OpsPulseProperties;
import com.moveinsync.opspulse.repository.TripRepository;
import com.moveinsync.opspulse.util.DataParsing;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class VendorEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(VendorEnrichmentService.class);
    private static final int TRIP_LOOKUP_BATCH = 2000;

    private final OpsPulseProperties properties;
    private final TripRepository tripRepository;
    private volatile Map<String, VendorBillMetric> cachedBillMetrics;
    private volatile Map<String, VendorSafetyMetric> cachedSafetyMetrics;

    public VendorEnrichmentService(OpsPulseProperties properties, TripRepository tripRepository) {
        this.properties = properties;
        this.tripRepository = tripRepository;
    }

    public Map<String, VendorBillMetric> billMetricsByVendor() {
        if (cachedBillMetrics != null) {
            return cachedBillMetrics;
        }
        cachedBillMetrics = loadBillMetrics();
        return cachedBillMetrics;
    }

    public Map<String, VendorSafetyMetric> safetyMetricsByVendor() {
        if (cachedSafetyMetrics != null) {
            return cachedSafetyMetrics;
        }
        cachedSafetyMetrics = loadSafetyMetrics();
        return cachedSafetyMetrics;
    }

    private Map<String, VendorBillMetric> loadBillMetrics() {
        Path billFile = Path.of(properties.getDataPath()).resolve(properties.getBillFile());
        Map<String, VendorBillMetric> metrics = new HashMap<>();
        if (!Files.isRegularFile(billFile)) {
            log.warn("Bill file not found: {}", billFile);
            return metrics;
        }

        try (Reader reader = Files.newBufferedReader(billFile);
             CSVReader csv = new CSVReaderBuilder(reader).withSkipLines(0).build()) {
            String[] headers = csv.readNext();
            Map<String, Integer> idx = headerIndex(headers);
            String[] row;
            while ((row = csv.readNext()) != null) {
                String vendor = get(row, idx, "vendor");
                if (vendor.isBlank()) {
                    continue;
                }
                BigDecimal cost = DataParsing.parseDecimal(get(row, idx, "trip_cost"));
                BigDecimal km = DataParsing.parseDecimal(get(row, idx, "total_trip_km"));
                VendorBillMetric metric = metrics.computeIfAbsent(vendor, v -> new VendorBillMetric());
                metric.totalCost = metric.totalCost.add(cost);
                metric.totalKm = metric.totalKm.add(km);
            }
        } catch (Exception e) {
            log.error("Failed to load vendor bill metrics", e);
            return metrics;
        }

        for (VendorBillMetric metric : metrics.values()) {
            if (metric.totalKm.compareTo(BigDecimal.ZERO) > 0) {
                metric.costPerKm = metric.totalCost.divide(metric.totalKm, 2, RoundingMode.HALF_UP);
            }
        }
        log.info("Loaded bill metrics for {} vendors", metrics.size());
        return metrics;
    }

    private Map<String, VendorSafetyMetric> loadSafetyMetrics() {
        Path alertsFile = Path.of(properties.getDataPath()).resolve(properties.getAlertsFile());
        Map<String, VendorSafetyMetric> metrics = new HashMap<>();
        if (!Files.isRegularFile(alertsFile)) {
            log.warn("Alerts file not found: {}", alertsFile);
            return metrics;
        }

        Map<Long, VendorSafetyMetric> pendingByTrip = new HashMap<>();
        Set<Long> tripIds = new HashSet<>();

        try (Reader reader = Files.newBufferedReader(alertsFile);
             CSVReader csv = new CSVReaderBuilder(reader).withSkipLines(0).build()) {
            String[] headers = csv.readNext();
            Map<String, Integer> idx = headerIndex(headers);
            String[] row;
            while ((row = csv.readNext()) != null) {
                String startTime = get(row, idx, "start_time");
                if (!isJuly2026(startTime)) {
                    continue;
                }

                long tripId;
                try {
                    tripId = DataParsing.normalizeTripId(get(row, idx, "trip_id"));
                } catch (Exception e) {
                    continue;
                }
                if (tripId <= 0) {
                    continue;
                }

                String severity = normalizeSeverity(get(row, idx, "severity"));
                String eventType = get(row, idx, "event_type");

                VendorSafetyMetric tripMetric = pendingByTrip.computeIfAbsent(tripId, id -> new VendorSafetyMetric());
                tripMetric.incidentCount++;
                if ("Sev-1".equals(severity)) {
                    tripMetric.sev1Count++;
                }
                if (eventType.contains("PANIC")) {
                    tripMetric.panicCount++;
                }
                tripIds.add(tripId);
            }
        } catch (Exception e) {
            log.error("Failed to load vendor safety metrics", e);
            return metrics;
        }

        Map<Long, String> tripVendors = lookupTripVendors(tripIds);
        for (Map.Entry<Long, VendorSafetyMetric> entry : pendingByTrip.entrySet()) {
            String vendorId = tripVendors.get(entry.getKey());
            if (vendorId == null || vendorId.isBlank()) {
                continue;
            }
            VendorSafetyMetric vendorMetric = metrics.computeIfAbsent(vendorId, v -> new VendorSafetyMetric());
            VendorSafetyMetric tripMetric = entry.getValue();
            vendorMetric.incidentCount += tripMetric.incidentCount;
            vendorMetric.sev1Count += tripMetric.sev1Count;
            vendorMetric.panicCount += tripMetric.panicCount;
        }

        log.info("Loaded safety metrics for {} vendors from {} July alert trips", metrics.size(), tripIds.size());
        return metrics;
    }

    private Map<Long, String> lookupTripVendors(Set<Long> tripIds) {
        Map<Long, String> vendors = new HashMap<>();
        List<Long> ids = new ArrayList<>(tripIds);
        for (int i = 0; i < ids.size(); i += TRIP_LOOKUP_BATCH) {
            List<Long> batch = ids.subList(i, Math.min(i + TRIP_LOOKUP_BATCH, ids.size()));
            for (Object[] row : tripRepository.findVendorIdsByTripIds(batch)) {
                vendors.put(((Number) row[0]).longValue(), (String) row[1]);
            }
        }
        return vendors;
    }

    private boolean isJuly2026(String startTime) {
        return startTime != null && startTime.contains("Jul") && startTime.contains("2026");
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank() || "False".equalsIgnoreCase(severity)) {
            return "UNKNOWN";
        }
        return severity.trim();
    }

    private Map<String, Integer> headerIndex(String[] headers) {
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            idx.put(headers[i].trim(), i);
        }
        return idx;
    }

    private String get(String[] row, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null || i >= row.length || row[i] == null) {
            return "";
        }
        return row[i].trim();
    }

    public static class VendorBillMetric {
        private BigDecimal totalCost = BigDecimal.ZERO;
        private BigDecimal totalKm = BigDecimal.ZERO;
        private BigDecimal costPerKm;

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public BigDecimal getTotalKm() {
            return totalKm;
        }

        public BigDecimal getCostPerKm() {
            return costPerKm;
        }
    }

    public static class VendorSafetyMetric {
        private long incidentCount;
        private long sev1Count;
        private long panicCount;

        public long getIncidentCount() {
            return incidentCount;
        }

        public long getSev1Count() {
            return sev1Count;
        }

        public long getPanicCount() {
            return panicCount;
        }
    }
}
