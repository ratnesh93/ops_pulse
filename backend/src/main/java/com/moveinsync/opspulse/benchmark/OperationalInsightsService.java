package com.moveinsync.opspulse.benchmark;

import com.moveinsync.opspulse.config.OpsPulseProperties;
import com.moveinsync.opspulse.repository.TripRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OperationalInsightsService {

    private static final Logger log = LoggerFactory.getLogger(OperationalInsightsService.class);

    private final OpsPulseProperties properties;
    private final TripRepository tripRepository;
    private volatile SafetyInsight cachedSafetyInsight;

    public OperationalInsightsService(OpsPulseProperties properties, TripRepository tripRepository) {
        this.properties = properties;
        this.tripRepository = tripRepository;
    }

    public CapacityInsight analyzeCapacity() {
        List<Object[]> rows = tripRepository.topOverbookedOfficeShift();
        if (rows.isEmpty()) {
            return null;
        }

        Object[] row = rows.get(0);
        CapacityInsight insight = new CapacityInsight();
        insight.setOffice((String) row[0]);
        insight.setShiftId((String) row[1]);
        insight.setTotalRiders(((Number) row[2]).longValue());
        insight.setTotalSeats(((Number) row[3]).longValue());
        insight.setOverbookedSeats(((Number) row[4]).longValue());
        insight.setOverbookedTrips(((Number) row[5]).longValue());
        return insight;
    }

    public SafetyInsight analyzeSafety() {
        if (cachedSafetyInsight != null) {
            return cachedSafetyInsight;
        }
        cachedSafetyInsight = loadSafetyInsight();
        return cachedSafetyInsight;
    }

    private SafetyInsight loadSafetyInsight() {
        Path alertsFile = Path.of(properties.getDataPath()).resolve(properties.getAlertsFile());
        if (!Files.isRegularFile(alertsFile)) {
            log.warn("Alerts file not found: {}", alertsFile);
            return emptySafetyInsight();
        }

        Map<String, Long> sev1ByType = new HashMap<>();
        long julyAlerts = 0;
        long sev1 = 0;
        long panic = 0;
        long openHigh = 0;

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
                julyAlerts++;

                String eventType = get(row, idx, "event_type");
                String severity = normalizeSeverity(get(row, idx, "severity"));
                String state = get(row, idx, "state_text");

                if ("Sev-1".equals(severity) || "Sev-2".equals(severity)) {
                    if ("OPEN".equals(state) || "NEW".equals(state)) {
                        openHigh++;
                    }
                }

                if ("Sev-1".equals(severity)) {
                    sev1++;
                    sev1ByType.merge(eventType, 1L, Long::sum);
                }

                if (eventType.contains("PANIC")) {
                    panic++;
                }
            }
        } catch (Exception e) {
            log.error("Failed to load alerts insight", e);
            return emptySafetyInsight();
        }

        SafetyInsight insight = new SafetyInsight();
        insight.setJulyAlertCount(julyAlerts);
        insight.setSev1Count(sev1);
        insight.setPanicCount(panic);
        insight.setOpenHighSeverityCount(openHigh);
        insight.setTopEventTypes(sev1ByType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new)));
        log.info("Safety insight: {} July alerts, {} Sev-1, {} panic", julyAlerts, sev1, panic);
        return insight;
    }

    private SafetyInsight emptySafetyInsight() {
        SafetyInsight insight = new SafetyInsight();
        insight.setTopEventTypes(Map.of());
        return insight;
    }

    private boolean isJuly2026(String startTime) {
        if (startTime == null || startTime.isBlank()) {
            return false;
        }
        return startTime.contains("Jul") && startTime.contains("2026");
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
}
