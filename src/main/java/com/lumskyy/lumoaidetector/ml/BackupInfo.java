package com.lumskyy.lumoaidetector.ml;

import java.io.File;

public final class BackupInfo {
    private final String name;
    private final String originalName;
    private final File modelFile;
    private final File metadataFile;
    private final long deletedAt;
    private final long expiresAt;

    public BackupInfo(String name, String originalName, File modelFile, File metadataFile, long deletedAt, long expiresAt) {
        this.name = name;
        this.originalName = originalName;
        this.modelFile = modelFile;
        this.metadataFile = metadataFile;
        this.deletedAt = deletedAt;
        this.expiresAt = expiresAt;
    }

    public String name() {
        return name;
    }

    public String originalName() {
        return originalName;
    }

    public File modelFile() {
        return modelFile;
    }

    public File metadataFile() {
        return metadataFile;
    }

    public long deletedAt() {
        return deletedAt;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public long size() {
        return modelFile.exists() ? modelFile.length() : 0L;
    }
}
