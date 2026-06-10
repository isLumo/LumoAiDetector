package com.lumskyy.lumoaidetector.ml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MetricsMathTest {

    @Test
    public void perfectClassifier() {
        MetricsMath m = MetricsMath.of(10, 10, 0, 0);
        assertEquals(100.0D, m.accuracy(), 1e-9);
        assertEquals(100.0D, m.precision(), 1e-9);
        assertEquals(100.0D, m.recall(), 1e-9);
        assertEquals(100.0D, m.f1(), 1e-9);
        assertEquals(0.0D, m.falsePositiveRate(), 1e-9);
    }

    @Test
    public void mixedConfusionMatrix() {
        // tp=8, tn=80, fp=2, fn=10
        MetricsMath m = MetricsMath.of(8, 80, 2, 10);
        assertEquals((8 + 80) * 100.0D / 100.0D, m.accuracy(), 1e-9);
        assertEquals(8 * 100.0D / (8 + 2), m.precision(), 1e-9);
        assertEquals(8 * 100.0D / (8 + 10), m.recall(), 1e-9);
        assertEquals(2 * 100.0D / (2 + 80), m.falsePositiveRate(), 1e-9);
    }

    @Test
    public void noPositivesPredicted() {
        MetricsMath m = MetricsMath.of(0, 50, 0, 10);
        assertEquals(0.0D, m.precision(), 1e-9);
        assertEquals(0.0D, m.recall(), 1e-9);
        assertEquals(0.0D, m.f1(), 1e-9);
    }

    @Test
    public void emptyDoesNotDivideByZero() {
        MetricsMath m = MetricsMath.of(0, 0, 0, 0);
        assertEquals(0.0D, m.accuracy(), 1e-9);
        assertTrue(Double.isFinite(m.f1()));
    }

    @Test
    public void f1IsHarmonicMean() {
        MetricsMath m = MetricsMath.of(5, 5, 5, 5);
        double precision = 50.0D;
        double recall = 50.0D;
        double expected = 2.0D * precision * recall / (precision + recall);
        assertEquals(expected, m.f1(), 1e-9);
    }
}
