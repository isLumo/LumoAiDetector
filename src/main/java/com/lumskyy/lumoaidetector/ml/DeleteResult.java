package com.lumskyy.lumoaidetector.ml;

public final class DeleteResult {
    private final String modelName;
    private final String backupName;

    public DeleteResult(String modelName, String backupName) {
        this.modelName = modelName;
        this.backupName = backupName;
    }

    public String modelName() {
        return modelName;
    }

    public String backupName() {
        return backupName;
    }
}
