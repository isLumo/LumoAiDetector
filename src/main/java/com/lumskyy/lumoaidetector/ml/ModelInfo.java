package com.lumskyy.lumoaidetector.ml;

import java.io.File;

public final class ModelInfo {
    private final String name;
    private final File modelFile;
    private final File metadataFile;
    private final ModelMetadata metadata;

    public ModelInfo(String name, File modelFile, File metadataFile, ModelMetadata metadata) {
        this.name = name;
        this.modelFile = modelFile;
        this.metadataFile = metadataFile;
        this.metadata = metadata;
    }

    public String name() {
        return name;
    }

    public File modelFile() {
        return modelFile;
    }

    public File metadataFile() {
        return metadataFile;
    }

    public ModelMetadata metadata() {
        return metadata;
    }

    public long size() {
        return modelFile.exists() ? modelFile.length() : 0L;
    }
}
