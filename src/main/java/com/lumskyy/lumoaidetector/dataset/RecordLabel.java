package com.lumskyy.lumoaidetector.dataset;

public enum RecordLabel {
    LEGIT(0, "legit"),
    CHEATER(1, "cheater");

    private final int classValue;
    private final String id;

    RecordLabel(int classValue, String id) {
        this.classValue = classValue;
        this.id = id;
    }

    public int classValue() {
        return classValue;
    }

    public String id() {
        return id;
    }

    public static RecordLabel parse(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase();
        if (normalized.equals("legit") || normalized.equals("0") || normalized.equals("clean")) {
            return LEGIT;
        }
        if (normalized.equals("cheater") || normalized.equals("cheat") || normalized.equals("1")) {
            return CHEATER;
        }
        return null;
    }
}
