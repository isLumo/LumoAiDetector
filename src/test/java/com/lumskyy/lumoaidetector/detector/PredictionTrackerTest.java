package com.lumskyy.lumoaidetector.detector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PredictionTrackerTest {

    @Test
    public void emptyTrackerIsZero() {
        PredictionTracker tracker = new PredictionTracker();
        assertEquals(0, tracker.windows());
        assertEquals(0, tracker.cheatWindows());
        assertEquals(0.0D, tracker.percent(), 1e-9);
    }

    @Test
    public void percentReflectsCheatRatio() {
        PredictionTracker tracker = new PredictionTracker();
        tracker.add(true, 10);
        tracker.add(false, 10);
        tracker.add(true, 10);
        tracker.add(true, 10);
        assertEquals(4, tracker.windows());
        assertEquals(3, tracker.cheatWindows());
        assertEquals(75.0D, tracker.percent(), 1e-9);
    }

    @Test
    public void respectsMaxSizeSlidingWindow() {
        PredictionTracker tracker = new PredictionTracker();
        for (int i = 0; i < 20; i++) {
            tracker.add(true, 5);
        }
        assertEquals(5, tracker.windows());
        assertEquals(100.0D, tracker.percent(), 1e-9);
    }

    @Test
    public void clearResetsState() {
        PredictionTracker tracker = new PredictionTracker();
        tracker.add(true, 10);
        tracker.clear();
        assertEquals(0, tracker.windows());
        assertTrue(tracker.percent() == 0.0D);
    }
}
