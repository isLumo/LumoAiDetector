package com.lumskyy.lumoaidetector.ml;

public final class MetricsMath {
    private final double accuracy;
    private final double precision;
    private final double recall;
    private final double f1;
    private final double falsePositiveRate;

    private MetricsMath(double accuracy, double precision, double recall, double f1, double falsePositiveRate) {
        this.accuracy = accuracy;
        this.precision = precision;
        this.recall = recall;
        this.f1 = f1;
        this.falsePositiveRate = falsePositiveRate;
    }

    public static MetricsMath of(int tp, int tn, int fp, int fn) {
        double total = Math.max(1, tp + tn + fp + fn);
        double accuracy = (tp + tn) * 100.0D / total;
        double precision = tp + fp == 0 ? 0.0D : tp * 100.0D / (tp + fp);
        double recall = tp + fn == 0 ? 0.0D : tp * 100.0D / (tp + fn);
        double f1 = precision + recall == 0 ? 0.0D : 2.0D * precision * recall / (precision + recall);
        double falsePositiveRate = fp + tn == 0 ? 0.0D : fp * 100.0D / (fp + tn);
        return new MetricsMath(accuracy, precision, recall, f1, falsePositiveRate);
    }

    public double accuracy() {
        return accuracy;
    }

    public double precision() {
        return precision;
    }

    public double recall() {
        return recall;
    }

    public double f1() {
        return f1;
    }

    public double falsePositiveRate() {
        return falsePositiveRate;
    }
}
