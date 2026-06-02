package com.lumskyy.lumoaidetector.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DatasetCsv {
    public static final int WINDOW_SIZE = 15;
    public static final int VALUES_PER_TICK = 8;
    public static final int FEATURE_COUNT = WINDOW_SIZE * VALUES_PER_TICK;
    public static final int COLUMN_COUNT = FEATURE_COUNT + 1;
    public static final int MAX_ROWS = 500000;
    private static final String[] NAMES = new String[]{"dx", "dy", "dt", "v", "a", "j", "err", "derr"};
    private static final ThreadLocal<DecimalFormat> DOUBLE_FMT =
            ThreadLocal.withInitial(new java.util.function.Supplier<DecimalFormat>() {
                @Override
                public DecimalFormat get() {
                    DecimalFormat fmt = new DecimalFormat("0.00000000",
                            DecimalFormatSymbols.getInstance(Locale.US));
                    return fmt;
                }
            });

    private DatasetCsv() {
    }

    public static String header() {
        StringBuilder builder = new StringBuilder();
        for (int tick = 1; tick <= WINDOW_SIZE; tick++) {
            for (int i = 0; i < NAMES.length; i++) {
                if (builder.length() > 0) {
                    builder.append(',');
                }
                builder.append(NAMES[i]).append('_').append(tick);
            }
        }
        builder.append(",class");
        return builder.toString();
    }

    public static String row(double[] features, RecordLabel label) {
        if (features.length != FEATURE_COUNT) {
            throw new IllegalArgumentException("Expected " + FEATURE_COUNT + " features, got " + features.length);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < features.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(DOUBLE_FMT.get().format(features[i]));
        }
        builder.append(',').append(label.classValue());
        return builder.toString();
    }

    public static DatasetSnapshot read(File file) throws IOException {
        return read(file, 0);
    }

    public static DatasetSnapshot read(File file, int maxRows) throws IOException {
        if (!file.exists()) {
            return new DatasetSnapshot(new double[0][0], new int[0], 0, 0, 0);
        }
        List<double[]> features = new ArrayList<double[]>();
        List<Integer> labels = new ArrayList<Integer>();
        int legit = 0;
        int cheater = 0;
        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("dx_1,")) {
                    continue;
                }
                if (maxRows > 0 && features.size() >= maxRows) {
                    skipped++;
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length != COLUMN_COUNT) {
                    skipped++;
                    continue;
                }
                double[] row = new double[FEATURE_COUNT];
                boolean valid = true;
                for (int i = 0; i < FEATURE_COUNT; i++) {
                    try {
                        row[i] = Double.parseDouble(parts[i]);
                    } catch (NumberFormatException exception) {
                        valid = false;
                        break;
                    }
                    if (Double.isNaN(row[i]) || Double.isInfinite(row[i])) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) {
                    skipped++;
                    continue;
                }
                int label;
                try {
                    label = Integer.parseInt(parts[FEATURE_COUNT].trim());
                } catch (NumberFormatException exception) {
                    skipped++;
                    continue;
                }
                if (label != 0 && label != 1) {
                    skipped++;
                    continue;
                }
                if (label == 0) {
                    legit++;
                } else {
                    cheater++;
                }
                features.add(row);
                labels.add(Integer.valueOf(label));
            }
        }
        double[][] x = new double[features.size()][FEATURE_COUNT];
        int[] y = new int[features.size()];
        for (int i = 0; i < features.size(); i++) {
            x[i] = features.get(i);
            y[i] = labels.get(i).intValue();
        }
        return new DatasetSnapshot(x, y, legit, cheater, skipped);
    }
}
