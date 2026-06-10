package com.lumskyy.lumoaidetector.dataset;

public final class DatasetCounts {
    private final int legitRows;
    private final int cheaterRows;
    private final int skippedRows;

    public DatasetCounts(int legitRows, int cheaterRows, int skippedRows) {
        this.legitRows = legitRows;
        this.cheaterRows = cheaterRows;
        this.skippedRows = skippedRows;
    }

    public int rows() {
        return legitRows + cheaterRows;
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
