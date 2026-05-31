package com.lumskyy.lumoaidetector.ml;

import smile.classification.RandomForest;

public final class ModelBundle {
    private final RandomForest model;
    private final ModelInfo info;

    public ModelBundle(RandomForest model, ModelInfo info) {
        this.model = model;
        this.info = info;
    }

    public RandomForest model() {
        return model;
    }

    public ModelInfo info() {
        return info;
    }
}
