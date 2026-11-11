package dev.covenant.client;

/** Tests the production centerline's endpoints, direction, degeneracy and bounded ornament. */
public final class LightningPathTest {
    public static void main(String[] args) {
        var start = new LightningPath.Point(10, -20, 30);
        check(LightningPath.points(start, start).equals(java.util.List.of(start, start)), "Zero-length beam stays degenerate");
        for (var end : java.util.List.of(new LightningPath.Point(42, -20, 30), new LightningPath.Point(-22, -20, 30),
                new LightningPath.Point(10, 12, 30), new LightningPath.Point(10, -52, 30),
                new LightningPath.Point(10, -20, 62), new LightningPath.Point(10, -20, -2),
                new LightningPath.Point(10.000001, 11, 30), new LightningPath.Point(10.001, -20, 30))) {
            verify(start, end);
        }
        var random = new java.util.Random(731);
        for (int i = 0; i < 500; i++) {
            double x = random.nextDouble() * 2 - 1, y = random.nextDouble() * 2 - 1, z = random.nextDouble() * 2 - 1;
            double scale = (random.nextDouble() * 31.9 + .001) / Math.sqrt(x*x+y*y+z*z);
            verify(start, new LightningPath.Point(start.x()+x*scale, start.y()+y*scale, start.z()+z*scale));
        }
        reject(start, new LightningPath.Point(Double.NaN, 0, 0));
        reject(start, new LightningPath.Point(1000, 0, 0));
        System.out.println("Lightning path checks passed: exact endpoints, bounded 3D deviations, no invented branches, vertical/zero-length aim and finite budget.");
    }

    private static void verify(LightningPath.Point a, LightningPath.Point b) {
        var points = LightningPath.points(a,b);
        check(points.size() >= 2 && points.size() <= 65, "Finite segment budget");
        check(points.getFirst().equals(a) && points.getLast().equals(b), "Exact authoritative endpoints");
        double dx=b.x()-a.x(), dy=b.y()-a.y(), dz=b.z()-a.z(), lengthSquared=dx*dx+dy*dy+dz*dz;
        double previous=-1;
        for (var p : points) {
            double x=p.x()-a.x(),y=p.y()-a.y(),z=p.z()-a.z();
            double t=(x*dx+y*dy+z*dz)/lengthSquared;
            check(t>=-1e-8 && t<=1+1e-8 && t>previous, "Path advances between endpoints");
            previous=t;
            double offset=Math.sqrt(Math.pow(x-t*dx,2)+Math.pow(y-t*dy,2)+Math.pow(z-t*dz,2));
            check(Double.isFinite(offset) && offset<.079, "Ornament stays inside narrow beam tube");
        }
    }
    private static void reject(LightningPath.Point a, LightningPath.Point b) {
        try { LightningPath.points(a,b); throw new AssertionError("Invalid beam accepted"); }
        catch (IllegalArgumentException expected) { }
    }
    private static void check(boolean valid, String message) { if (!valid) { throw new AssertionError(message); } }
}
