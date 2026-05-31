package com.lumskyy.lumoaidetector.detector;

import java.util.ArrayDeque;

public final class PredictionTracker {
    private final ArrayDeque<Boolean> results = new ArrayDeque<Boolean>();

    public void add(boolean cheater, int maxSize) {
        results.addLast(Boolean.valueOf(cheater));
        while (results.size() > maxSize) {
            results.removeFirst();
        }
    }

    public int windows() {
        return results.size();
    }

    public int cheatWindows() {
        int count = 0;
        for (Boolean value : results) {
            if (value.booleanValue()) {
                count++;
            }
        }
        return count;
    }

    public double percent() {
        if (results.isEmpty()) {
            return 0.0D;
        }
        return cheatWindows() * 100.0D / results.size();
    }

    public void clear() {
        results.clear();
    }
}
