package com.lumskyy.lumoaidetector.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formats {
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US).withZone(UTC);
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.US).withZone(UTC);

    private Formats() {
    }

    public static String percent(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    public static String number(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    public static String date(long millis) {
        return DATE_FMT.format(Instant.ofEpochMilli(millis));
    }

    public static String fileDate(long millis) {
        return FILE_DATE_FMT.format(Instant.ofEpochMilli(millis));
    }

    public static String duration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m " + secs + "s";
        }
        if (minutes > 0L) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }

    public static String size(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0D;
        if (kb < 1024.0D) {
            return String.format(Locale.US, "%.1f", kb) + " KB";
        }
        double mb = kb / 1024.0D;
        return String.format(Locale.US, "%.1f", mb) + " MB";
    }
}
