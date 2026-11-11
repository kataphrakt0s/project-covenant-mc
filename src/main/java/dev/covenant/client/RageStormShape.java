package dev.covenant.client;

/** Dependency-free hollow spherical bands; every vertex stays inside the gameplay radius. */
final class RageStormShape {
    @FunctionalInterface interface Quad {
        void accept(float x1, float y1, float z1, float x2, float y2, float z2,
                    float x3, float y3, float z3, float x4, float y4, float z4, boolean steel);
    }

    private RageStormShape() {}

    static void emit(float radius, float phase, Quad out) {
        if (!Float.isFinite(radius) || !Float.isFinite(phase) || radius <= 0 || radius > 8) { return; }
        double width = Math.min(.025, radius * .025);
        for (int band = -2; band <= 2; band++) {
            double y = Math.sin(band * .6) * radius;
            double low = y - width, high = y + width;
            double lowRing = Math.sqrt(radius * radius - low * low);
            double highRing = Math.sqrt(radius * radius - high * high);
            for (int part = 0; part < 24; part++) {
                if (part % 6 >= 4) { continue; } // Four curved strokes per band; wide gaps leave enemies visible.
                double a = phase + band * .7 + part * Math.PI / 12;
                double b = a + Math.PI / 12;
                out.accept((float) (Math.cos(a) * lowRing), (float) low, (float) (Math.sin(a) * lowRing),
                        (float) (Math.cos(b) * lowRing), (float) low, (float) (Math.sin(b) * lowRing),
                        (float) (Math.cos(b) * highRing), (float) high, (float) (Math.sin(b) * highRing),
                        (float) (Math.cos(a) * highRing), (float) high, (float) (Math.sin(a) * highRing), band == 0);
            }
        }
    }
}
