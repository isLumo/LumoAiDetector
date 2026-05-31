package com.lumskyy.lumoaidetector.detector;

import com.lumskyy.lumoaidetector.config.PluginSettings;
import com.lumskyy.lumoaidetector.dataset.DatasetCsv;
import java.util.ArrayDeque;

public final class PlayerSampleState {
    public boolean initialized;
    public double lastYaw;
    public double lastPitch;
    public double lastV;
    public double lastA;
    public double lastErr = 180.0D;
    public long lastTime;
    public long lastCombatTime;
    public long lastAlertTime;
    public long lastPunishTime;
    public double lastPercent;
    public double lastConfidence;
    public String lastModel = "none";
    private final ArrayDeque<RotationSample> window = new ArrayDeque<RotationSample>();
    private final ArrayDeque<Long> gcdValues = new ArrayDeque<Long>();
    private final PredictionTracker tracker = new PredictionTracker();

    public void markCombat(long now) {
        lastCombatTime = now;
    }

    public double multiplier(double yawDelta, double pitchDelta, PluginSettings settings) {
        addGcdValue(yawDelta, settings);
        addGcdValue(pitchDelta, settings);
        long gcd = 0L;
        for (Long value : gcdValues) {
            long current = value.longValue();
            if (current <= 0L) {
                continue;
            }
            gcd = gcd == 0L ? current : gcd(gcd, current);
        }
        double multiplier = gcd <= 0L ? settings.defaultMultiplier : gcd / settings.gcdScale;
        if (Double.isNaN(multiplier) || Double.isInfinite(multiplier) || multiplier < settings.gcdMinMultiplier) {
            return settings.defaultMultiplier;
        }
        return multiplier;
    }

    public void addSample(RotationSample sample, int maxSize) {
        window.addLast(sample);
        while (window.size() > maxSize) {
            window.removeFirst();
        }
    }

    public boolean full(int size) {
        return window.size() >= size;
    }

    public double movementSum() {
        double sum = 0.0D;
        for (RotationSample sample : window) {
            sum += Math.abs(sample.dx) + Math.abs(sample.dy);
        }
        return sum;
    }

    public boolean timeStable(double min, double max) {
        for (RotationSample sample : window) {
            if (sample.dt < min || sample.dt > max) {
                return false;
            }
        }
        return true;
    }

    public double[] features() {
        double[] features = new double[DatasetCsv.FEATURE_COUNT];
        int offset = 0;
        for (RotationSample sample : window) {
            sample.write(features, offset);
            offset += DatasetCsv.VALUES_PER_TICK;
        }
        return features;
    }

    public void clearWindow() {
        window.clear();
    }

    public PredictionTracker tracker() {
        return tracker;
    }

    private void addGcdValue(double delta, PluginSettings settings) {
        long scaled = Math.round(Math.abs(delta) * settings.gcdScale);
        if (scaled <= 0L) {
            return;
        }
        gcdValues.addLast(Long.valueOf(scaled));
        while (gcdValues.size() > settings.gcdHistorySize) {
            gcdValues.removeFirst();
        }
    }

    private long gcd(long a, long b) {
        long x = Math.abs(a);
        long y = Math.abs(b);
        while (y != 0L) {
            long temp = x % y;
            x = y;
            y = temp;
        }
        return x;
    }
}
