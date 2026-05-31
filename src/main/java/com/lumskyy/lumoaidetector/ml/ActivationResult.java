package com.lumskyy.lumoaidetector.ml;

public final class ActivationResult {
    public enum Status {
        ACTIVATED,
        NOT_FOUND,
        ALREADY_ACTIVE,
        CONFLICT,
        ERROR
    }

    private final Status status;
    private final String modelName;
    private final String activeName;
    private final String error;

    private ActivationResult(Status status, String modelName, String activeName, String error) {
        this.status = status;
        this.modelName = modelName;
        this.activeName = activeName;
        this.error = error;
    }

    public static ActivationResult activated(String modelName) {
        return new ActivationResult(Status.ACTIVATED, modelName, modelName, "");
    }

    public static ActivationResult notFound(String modelName) {
        return new ActivationResult(Status.NOT_FOUND, modelName, "", "");
    }

    public static ActivationResult alreadyActive(String modelName) {
        return new ActivationResult(Status.ALREADY_ACTIVE, modelName, modelName, "");
    }

    public static ActivationResult conflict(String modelName, String activeName) {
        return new ActivationResult(Status.CONFLICT, modelName, activeName, "");
    }

    public static ActivationResult error(String modelName, String error) {
        return new ActivationResult(Status.ERROR, modelName, "", error);
    }

    public Status status() {
        return status;
    }

    public String modelName() {
        return modelName;
    }

    public String activeName() {
        return activeName;
    }

    public String error() {
        return error;
    }
}
