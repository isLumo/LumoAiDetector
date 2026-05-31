package com.lumskyy.lumoaidetector.detector;

public final class RotationSample {
    public final double dx;
    public final double dy;
    public final double dt;
    public final double v;
    public final double a;
    public final double j;
    public final double err;
    public final double derr;

    public RotationSample(double dx, double dy, double dt, double v, double a, double j, double err, double derr) {
        this.dx = dx;
        this.dy = dy;
        this.dt = dt;
        this.v = v;
        this.a = a;
        this.j = j;
        this.err = err;
        this.derr = derr;
    }

    public void write(double[] output, int offset) {
        output[offset] = dx;
        output[offset + 1] = dy;
        output[offset + 2] = dt;
        output[offset + 3] = v;
        output[offset + 4] = a;
        output[offset + 5] = j;
        output[offset + 6] = err;
        output[offset + 7] = derr;
    }
}
