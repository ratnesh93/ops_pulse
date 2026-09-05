package com.moveinsync.opspulse.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.Map;

public final class DataParsing {

    private static final DateTimeFormatter TRIP_DATE = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM d, yyyy")
            .toFormatter(Locale.ENGLISH);

    private DataParsing() {
    }

    public static long normalizeTripId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("trip_id is blank");
        }
        return Long.parseLong(raw.replace(",", "").trim());
    }

    public static long parseEpoch(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        return Long.parseLong(raw.replace(",", "").trim().split("\\.")[0]);
    }

    public static int parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        return Integer.parseInt(raw.replace(",", "").trim().split("\\.")[0]);
    }

    public static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(raw.replace(",", "").trim());
    }

    public static LocalDate parseTripDate(String raw) {
        return LocalDate.parse(raw.trim(), TRIP_DATE);
    }

    public static boolean isOnTime(long plannedEpoch, long actualEpoch) {
        if (plannedEpoch <= 0 || actualEpoch <= 0) {
            return false;
        }
        return (actualEpoch - plannedEpoch) <= 300;
    }

    public static String mapMode(String productType) {
        if (productType == null) {
            return "CAB";
        }
        return switch (productType) {
            case "BUS" -> "SHUTTLE";
            case "SPOT_2.0" -> "SPOT";
            default -> "CAB";
        };
    }

    public static String get(Map<String, String> row, String key) {
        String value = row.get(key);
        return value == null ? "" : value.trim();
    }
}
