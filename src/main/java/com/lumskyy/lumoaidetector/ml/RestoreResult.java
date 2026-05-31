package com.lumskyy.lumoaidetector.ml;

public final class RestoreResult {
    private final String backupName;
    private final String modelName;

    public RestoreResult(String backupName, String modelName) {
        this.backupName = backupName;
        this.modelName = modelName;
    }

    public String backupName() {
        return backupName;
    }

    public String modelName() {
        return modelName;
    }
}
