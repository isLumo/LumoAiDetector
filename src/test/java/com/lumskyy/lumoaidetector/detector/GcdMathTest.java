package com.lumskyy.lumoaidetector.detector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public class GcdMathTest {

    @Test
    public void gcdBasic() {
        assertEquals(6L, GcdMath.gcd(12L, 18L));
        assertEquals(1L, GcdMath.gcd(7L, 13L));
        assertEquals(5L, GcdMath.gcd(5L, 0L));
    }

    @Test
    public void combinedGcdIgnoresNonPositive() {
        long gcd = GcdMath.combinedGcd(Arrays.asList(0L, -3L, 12L, 8L));
        assertEquals(4L, gcd);
    }

    @Test
    public void combinedGcdEmptyIsZero() {
        assertEquals(0L, GcdMath.combinedGcd(Arrays.<Long>asList()));
    }

    @Test
    public void multiplierUsesGcdGrid() {
        // gcd=2000, scale=100000 -> 0.02
        double m = GcdMath.multiplier(2000L, 100000.0D, 0.01D, 0.0001D);
        assertEquals(0.02D, m, 1e-9);
    }

    @Test
    public void multiplierFallsBackWhenGcdMissing() {
        double m = GcdMath.multiplier(0L, 100000.0D, 0.01D, 0.0001D);
        assertEquals(0.01D, m, 1e-9);
    }

    @Test
    public void multiplierFallsBackBelowMinimum() {
        // gcd=1, scale=100000 -> 0.00001 which is below the 0.0001 floor
        double m = GcdMath.multiplier(1L, 100000.0D, 0.01D, 0.0001D);
        assertEquals(0.01D, m, 1e-9);
    }

    @Test
    public void multiplierNeverNaN() {
        double m = GcdMath.multiplier(2000L, 0.0D, 0.01D, 0.0001D);
        assertTrue(Double.isFinite(m));
    }
}
