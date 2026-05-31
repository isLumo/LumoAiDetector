package com.lumskyy.lumoaidetector.dataset;

public final class DatasetSnapshot {
    private final double[][] x;
    private final int[] y;
    private final int legitRows;
    private final int cheaterRows;
    private final int skippedRows;

    public DatasetSnapshot(double[][] x, int[] y, int legitRows, int cheaterRows, int skippedRows) {
        this.x = x;
        this.y = y;
        this.legitRows = legitRows;
        this.cheaterRows = cheaterRows;
        this.skippedRows = skippedRows;
    }

    public double[][] x() {
        return x;
    }

    public int[] y() {
        return y;
    }

    public int rows() {
        return y.length;
    }

    public int legitRows() {
        return legitRows;
    }

    public int cheaterRows() {
        return cheaterRows;
    }

    public int skippedRows() {
        return skippedRows;
    }
}
