package com.lumskyy.lumoaidetector.ml;

public final class PredictionResult {
    private final boolean available;
    private final int label;
    private final double confidencePercent;
    private final String modelName;

    public PredictionResult(boolean available, int label, double confidencePercent, String modelName) {
        this.available = available;
        this.label = label;
        this.confidencePercent = confidencePercent;
        this.modelName = modelName;
    }

    public static PredictionResult unavailable() {
        return new PredictionResult(false, 0, 0.0D, "none");
    }

    public boolean available() {
        return available;
    }

    public int label() {
        return label;
    }

    public boolean cheater() {
        return label == 1;
    }

    public double confidencePercent() {
        return confidencePercent;
    }

    public String modelName() {
        return modelName;
    }
}
