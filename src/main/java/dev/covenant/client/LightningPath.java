package dev.covenant.client;

import java.util.ArrayList;
import java.util.List;

/** A narrow jagged centerline between one pair of real beam endpoints; never invents branches. */
final class LightningPath {
    record Point(double x, double y, double z) {}

    private LightningPath() {}

    static List<Point> points(Point start, Point end) {
        double dx = end.x - start.x, dy = end.y - start.y, dz = end.z - start.z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(length) || length > 32) { throw new IllegalArgumentException("Invalid cosmetic beam"); }
        if (length < .000001) { return List.of(start, end); }
        double fx = dx / length, fy = dy / length, fz = dz / length;
        // Pick a perpendicular axis that remains well-conditioned for vertical aim.
        double rx = Math.abs(fy) > .9 ? 0 : -fz;
        double ry = Math.abs(fy) > .9 ? fz : 0;
        double rz = Math.abs(fy) > .9 ? -fy : fx;
        double inverse = 1 / Math.sqrt(rx * rx + ry * ry + rz * rz);
        rx *= inverse; ry *= inverse; rz *= inverse;
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;
        int segments = Math.min(64, Math.max(1, (int) Math.ceil(length * 2)));
        var path = new ArrayList<Point>(segments + 1);
        path.add(start);
        for (int i = 1; i < segments; i++) {
            double t = i / (double) segments;
            double envelope = Math.min(1, length * .5) * Math.sin(Math.PI * t);
            double side = (i % 2 == 0 ? .07 : -.07) * envelope;
            double up = (i % 3 - 1) * .035 * envelope;
            path.add(new Point(start.x + dx * t + rx * side + ux * up,
                    start.y + dy * t + ry * side + uy * up,
                    start.z + dz * t + rz * side + uz * up));
        }
        path.add(end);
        return List.copyOf(path);
    }
}
