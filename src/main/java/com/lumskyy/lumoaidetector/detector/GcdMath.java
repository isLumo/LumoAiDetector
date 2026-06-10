package com.lumskyy.lumoaidetector.detector;

/**
 * Pure GCD-grid math extracted from PlayerSampleState so it can be unit tested
 * without a running server. Estimates the smallest rotation step (the "mouse
 * grid") from a history of scaled deltas and derives a normalization multiplier.
 */
public final class GcdMath {
    private GcdMath() {
    }

    public static long gcd(long a, long b) {
        long x = Math.abs(a);
        long y = Math.abs(b);
        while (y != 0L) {
            long temp = x % y;
            x = y;
            y = temp;
        }
        return x;
    }

    public static long combinedGcd(Iterable<Long> values) {
        long gcd = 0L;
        for (Long value : values) {
            long current = value.longValue();
            if (current <= 0L) {
                continue;
            }
            gcd = gcd == 0L ? current : gcd(gcd, current);
        }
        return gcd;
    }

    public static double multiplier(long gcd, double gcdScale, double defaultMultiplier, double gcdMinMultiplier) {
        double multiplier = gcd <= 0L ? defaultMultiplier : gcd / gcdScale;
        if (Double.isNaN(multiplier) || Double.isInfinite(multiplier) || multiplier < gcdMinMultiplier) {
            return defaultMultiplier;
        }
        return multiplier;
    }
}
