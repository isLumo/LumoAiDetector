package com.lumskyy.lumoaidetector.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Formats {
    private static final DecimalFormat ONE = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat TWO = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final SimpleDateFormat FILE_DATE = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);

    private Formats() {
    }

    public static String percent(double value) {
        return ONE.format(value);
    }

    public static String number(double value) {
        return TWO.format(value);
    }

    public static String date(long millis) {
        return DATE.format(new Date(millis));
    }

    public static String fileDate(long millis) {
        return FILE_DATE.format(new Date(millis));
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
            return ONE.format(kb) + " KB";
        }
        double mb = kb / 1024.0D;
        return ONE.format(mb) + " MB";
    }
}
