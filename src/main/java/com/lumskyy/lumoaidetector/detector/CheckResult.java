package com.lumskyy.lumoaidetector.detector;

public final class CheckResult {
    private final boolean available;
    private final double percent;
    private final int windows;
    private final double confidence;
    private final String model;

    public CheckResult(boolean available, double percent, int windows, double confidence, String model) {
        this.available = available;
        this.percent = percent;
        this.windows = windows;
        this.confidence = confidence;
        this.model = model;
    }

    public boolean available() {
        return available;
    }

    public double percent() {
        return percent;
    }

    public int windows() {
        return windows;
    }

    public double confidence() {
        return confidence;
    }

    public String model() {
        return model;
    }
}
