package com.moveinsync.opspulse.data;

import com.moveinsync.opspulse.config.OpsPulseProperties;
import com.moveinsync.opspulse.domain.AppMetadata;
import com.moveinsync.opspulse.domain.Vendor;
import com.moveinsync.opspulse.domain.VendorMonthlyStat;
import com.moveinsync.opspulse.repository.AppMetadataRepository;
import com.moveinsync.opspulse.repository.TripRepository;
import com.moveinsync.opspulse.repository.VendorMonthlyStatRepository;
import com.moveinsync.opspulse.repository.VendorRepository;
import com.moveinsync.opspulse.util.DataParsing;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class MoveInSyncDatasetAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(MoveInSyncDatasetAdapter.class);
    private static final String DATA_LOADED_KEY = "data_loaded";
    private static final int BATCH_SIZE = 2000;

    private static final String INSERT_TRIP = """
            INSERT INTO trips (id, business_unit, office, mode, vendor_id, route_id, corridor,
                shift_id, trip_date, scheduled_at, actual_at, occupancy, capacity, cost,
                on_time, delay_minutes, delay_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """;

    private static final String INSERT_DELAY = """
            INSERT INTO delay_records (trip_id, reason_code, delay_minutes)
            VALUES (?, ?, ?)
            """;

    private final OpsPulseProperties properties;
    private final TripRepository tripRepository;
    private final VendorRepository vendorRepository;
    private final VendorMonthlyStatRepository vendorMonthlyStatRepository;
    private final AppMetadataRepository appMetadataRepository;
    private final JdbcTemplate jdbcTemplate;

    public MoveInSyncDatasetAdapter(
            OpsPulseProperties properties,
            TripRepository tripRepository,
            VendorRepository vendorRepository,
            VendorMonthlyStatRepository vendorMonthlyStatRepository,
            AppMetadataRepository appMetadataRepository,
            JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.tripRepository = tripRepository;
        this.vendorRepository = vendorRepository;
        this.vendorMonthlyStatRepository = vendorMonthlyStatRepository;
        this.appMetadataRepository = appMetadataRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void load() {
        if (properties.isSkipDataLoad()) {
            log.info("Skipping data load (SKIP_DATA_LOAD=true)");
            return;
        }
        loadForced(false);
    }

    @Override
    @Transactional
    public Map<String, Object> loadForced(boolean force) {
        Path dataDir = Path.of(properties.getDataPath());
        Map<String, Object> fileStatus = describeDataFiles(dataDir);

        if (!Files.isDirectory(dataDir)) {
            return result(false, "DATA_PATH not found: " + dataDir, fileStatus, 0);
        }

        java.util.List<String> missing = missingRequiredFiles(dataDir);
        if (!missing.isEmpty()) {
            return result(false, "Missing required CSV files: " + missing, fileStatus, tripRepository.count());
        }

        if (!force && appMetadataRepository.findById(DATA_LOADED_KEY).isPresent()) {
            long trips = tripRepository.count();
            log.info("Data already loaded ({} trips), skipping ingest", trips);
            return result(true, "Already loaded — use force=true to re-ingest", fileStatus, trips);
        }

        if (force) {
            appMetadataRepository.deleteById(DATA_LOADED_KEY);
            jdbcTemplate.update("DELETE FROM vendor_monthly_stats WHERE month_year = ?", "2026-06");
            log.info("Force re-ingest: cleared data_loaded flag and June vendor stats");
        }

        log.info("Loading bill data...");
        Map<Long, BigDecimal> billCosts = loadBillCosts(dataDir.resolve(properties.getBillFile()));

        log.info("Computing prior-month vendor stats from {}", properties.getPriorTripFile());
        computeMonthlyStats(dataDir.resolve(properties.getPriorTripFile()), "2026-06");

        log.info("Ingesting trips from {}", properties.getTripFile());
        ingestTrips(dataDir.resolve(properties.getTripFile()), billCosts);

        AppMetadata metadata = new AppMetadata();
        metadata.setKey(DATA_LOADED_KEY);
        metadata.setValue("true");
        appMetadataRepository.save(metadata);

        long tripCount = tripRepository.count();
        log.info("Data load complete. Trips: {}", tripCount);
        return result(true, "Ingest complete", fileStatus, tripCount);
    }

    public Map<String, Object> describeDataFiles(Path dataDir) {
        Map<String, Object> files = new HashMap<>();
        if (!Files.isDirectory(dataDir)) {
            files.put("dataPath", dataDir.toString());
            files.put("exists", false);
            return files;
        }
        files.put("dataPath", dataDir.toString());
        files.put("exists", true);
        files.put(properties.getTripFile(), fileInfo(dataDir.resolve(properties.getTripFile())));
        files.put(properties.getPriorTripFile(), fileInfo(dataDir.resolve(properties.getPriorTripFile())));
        files.put(properties.getBillFile(), fileInfo(dataDir.resolve(properties.getBillFile())));
        files.put(properties.getAlertsFile(), fileInfo(dataDir.resolve(properties.getAlertsFile())));
        files.put("dataLoaded", appMetadataRepository.findById(DATA_LOADED_KEY).isPresent());
        files.put("tripCount", tripRepository.count());
        return files;
    }

    private Map<String, Object> fileInfo(Path path) {
        Map<String, Object> info = new HashMap<>();
        info.put("present", Files.isRegularFile(path));
        if (Files.isRegularFile(path)) {
            try {
                info.put("bytes", Files.size(path));
            } catch (Exception ignored) {
                info.put("bytes", -1);
            }
        }
        return info;
    }

    private java.util.List<String> missingRequiredFiles(Path dataDir) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (String name : java.util.List.of(
                properties.getTripFile(),
                properties.getPriorTripFile(),
                properties.getBillFile(),
                properties.getAlertsFile())) {
            if (!Files.isRegularFile(dataDir.resolve(name))) {
                missing.add(name);
            }
        }
        return missing;
    }

    private Map<String, Object> result(boolean success, String message, Map<String, Object> files, long tripCount) {
        Map<String, Object> out = new HashMap<>();
        out.put("success", success);
        out.put("message", message);
        out.put("files", files);
        out.put("tripCount", tripCount);
        return out;
    }

    private Map<Long, BigDecimal> loadBillCosts(Path billFile) {
        Map<Long, BigDecimal> costs = new HashMap<>();
        try (Reader reader = Files.newBufferedReader(billFile);
             CSVReader csv = new CSVReaderBuilder(reader).withSkipLines(0).build()) {
            String[] headers = csv.readNext();
            Map<String, Integer> idx = headerIndex(headers);
            String[] row;
            while ((row = csv.readNext()) != null) {
                try {
                    long tripId = DataParsing.normalizeTripId(row[idx.get("trip_id")]);
                    BigDecimal cost = DataParsing.parseDecimal(row[idx.get("trip_cost")]);
                    costs.merge(tripId, cost, BigDecimal::add);
                } catch (Exception ignored) {
                    // skip malformed rows
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load bill data", e);
        }
        log.info("Loaded {} bill cost entries", costs.size());
        return costs;
    }

    private void computeMonthlyStats(Path tripFile, String monthYear) {
        Map<String, long[]> vendorCounts = new HashMap<>();
        try (Reader reader = Files.newBufferedReader(tripFile);
             CSVReader csv = new CSVReaderBuilder(reader).withSkipLines(0).build()) {
            String[] headers = csv.readNext();
            Map<String, Integer> idx = headerIndex(headers);
            String[] row;
            while ((row = csv.readNext()) != null) {
                try {
                    String vendor = get(row, idx, "vendor_id");
                    long planned = DataParsing.parseEpoch(get(row, idx, "planned_start_epoch"));
                    long actual = DataParsing.parseEpoch(get(row, idx, "actual_start_epoch"));
                    boolean onTime = DataParsing.isOnTime(planned, actual);
                    long[] counts = vendorCounts.computeIfAbsent(vendor, v -> new long[2]);
                    counts[0]++;
                    if (onTime) {
                        counts[1]++;
                    }
                } catch (Exception ignored) {
                    // skip malformed rows
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute monthly stats", e);
        }

        Set<String> vendors = new HashSet<>(vendorCounts.keySet());
        for (Map.Entry<String, long[]> entry : vendorCounts.entrySet()) {
            long total = entry.getValue()[0];
            long onTime = entry.getValue()[1];
            BigDecimal ota = total == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(onTime * 100.0 / total).setScale(1, RoundingMode.HALF_UP);

            VendorMonthlyStat stat = new VendorMonthlyStat();
            stat.setVendorId(entry.getKey());
            stat.setMonthYear(monthYear);
            stat.setOtaPct(ota);
            stat.setTripCount(total);
            vendorMonthlyStatRepository.save(stat);
        }

        saveVendors(vendors);
        log.info("Stored monthly stats for {} vendors ({})", vendors.size(), monthYear);
    }

    private void ingestTrips(Path tripFile, Map<Long, BigDecimal> billCosts) {
        Set<String> vendors = new HashSet<>();
        int total = 0;
        java.util.List<Object[]> tripBatch = new java.util.ArrayList<>();
        java.util.List<Object[]> delayBatch = new java.util.ArrayList<>();

        try (Reader reader = Files.newBufferedReader(tripFile);
             CSVReader csv = new CSVReaderBuilder(reader).withSkipLines(0).build()) {
            String[] headers = csv.readNext();
            Map<String, Integer> idx = headerIndex(headers);
            String[] row;
            while ((row = csv.readNext()) != null) {
                try {
                    long tripId = DataParsing.normalizeTripId(get(row, idx, "trip_id"));
                    long planned = DataParsing.parseEpoch(get(row, idx, "planned_start_epoch"));
                    long actual = DataParsing.parseEpoch(get(row, idx, "actual_start_epoch"));
                    String vendor = get(row, idx, "vendor_id");
                    String office = get(row, idx, "office");
                    String shift = get(row, idx, "shift_type");
                    String direction = get(row, idx, "trip_direction");
                    String nodal = get(row, idx, "trip_nodal");
                    String delayReason = get(row, idx, "delay_reason");
                    int delayMinutes = DataParsing.parseInt(get(row, idx, "delay_minutes"));
                    boolean onTime = DataParsing.isOnTime(planned, actual);

                    vendors.add(vendor);
                    tripBatch.add(new Object[]{
                            tripId,
                            get(row, idx, "business_unit"),
                            office,
                            DataParsing.mapMode(get(row, idx, "product_type")),
                            vendor,
                            office + "-" + shift + "-" + direction,
                            office + (nodal.isBlank() || "NA".equals(nodal) ? "" : "-" + nodal),
                            shift,
                            java.sql.Date.valueOf(DataParsing.parseTripDate(get(row, idx, "trip_date"))),
                            Timestamp.from(Instant.ofEpochSecond(planned)),
                            actual > 0 ? Timestamp.from(Instant.ofEpochSecond(actual)) : null,
                            DataParsing.parseInt(get(row, idx, "actualemployee_cnt")),
                            DataParsing.parseInt(get(row, idx, "actual_cab_capacity")),
                            billCosts.getOrDefault(tripId, BigDecimal.ZERO),
                            onTime,
                            delayMinutes,
                            delayReason
                    });

                    if (delayReason != null && !delayReason.isBlank() && !"NODELAY".equalsIgnoreCase(delayReason)) {
                        delayBatch.add(new Object[]{tripId, delayReason, delayMinutes});
                    }

                    if (tripBatch.size() >= BATCH_SIZE) {
                        flushBatches(tripBatch, delayBatch);
                        total += tripBatch.size();
                        tripBatch.clear();
                        delayBatch.clear();
                        if (total % 20000 == 0) {
                            log.info("Ingested {} trips...", total);
                        }
                    }
                } catch (Exception ignored) {
                    // skip malformed rows
                }
            }
            if (!tripBatch.isEmpty()) {
                flushBatches(tripBatch, delayBatch);
                total += tripBatch.size();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to ingest trips", e);
        }

        saveVendors(vendors);
        log.info("Ingested {} trips total", total);
    }

    private void flushBatches(java.util.List<Object[]> tripBatch, java.util.List<Object[]> delayBatch) {
        jdbcTemplate.batchUpdate(INSERT_TRIP, tripBatch);
        if (!delayBatch.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_DELAY, delayBatch);
        }
    }

    private void saveVendors(Set<String> vendorIds) {
        BigDecimal sla = BigDecimal.valueOf(properties.getSlaOtaPct());
        for (String vendorId : vendorIds) {
            if (vendorRepository.existsById(vendorId)) {
                continue;
            }
            Vendor vendor = new Vendor();
            vendor.setId(vendorId);
            vendor.setName(vendorId);
            vendor.setSlaOtaPct(sla);
            vendorRepository.save(vendor);
        }
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
        if (i == null || i >= row.length) {
            return "";
        }
        return row[i] == null ? "" : row[i].trim();
    }
}
