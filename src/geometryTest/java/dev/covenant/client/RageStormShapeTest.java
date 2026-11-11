package dev.covenant.client;

/** Exercises the production vertex stream, including tiny/large tuning and rotating phases. */
public final class RageStormShapeTest {
    public static void main(String[] args) {
        for (float radius : new float[] {.1f, .25f, 2, 8}) {
            for (float phase : new float[] {0, .37f, 15, 10000}) {
                int[] count = {0}; float[] extent = {0, 0, 0};
                RageStormShape.emit(radius, phase, (x1,y1,z1,x2,y2,z2,x3,y3,z3,x4,y4,z4,steel) -> {
                    count[0]++;
                    float[][] vertices = {{x1,y1,z1},{x2,y2,z2},{x3,y3,z3},{x4,y4,z4}};
                    for (var v : vertices) {
                        double distance = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
                        require(Double.isFinite(distance) && distance <= radius + .00001, "Vertex leaves actual sphere");
                        require(distance >= radius * .999, "Filled center or compressed sphere");
                        for (int axis = 0; axis < 3; axis++) { extent[axis] = Math.max(extent[axis], Math.abs(v[axis])); }
                    }
                    double edge = Math.sqrt((x2-x1)*(x2-x1) + (z2-z1)*(z2-z1));
                    require(edge > 0 && y3 > y2, "Degenerate band quad");
                    require(y3-y2 <= .051 && y3-y2 <= radius * .051, "Band becomes an opaque wall");
                });
                require(count[0] > 0 && count[0] < 100, "Unbounded/empty mesh");
                for (float axis : extent) { require(axis >= radius * .9, "Volume flattens on an axis"); }
            }
        }
        for (float radius : new float[] {0, -1, 9, Float.NaN, Float.POSITIVE_INFINITY}) {
            RageStormShape.emit(radius, 0, (a,b,c,d,e,f,g,h,i,j,k,l,s) -> { throw new AssertionError("Invalid radius emitted geometry"); });
        }
        RageStormShape.emit(2, Float.NaN, (a,b,c,d,e,f,g,h,i,j,k,l,s) -> { throw new AssertionError("Invalid phase emitted geometry"); });
        System.out.println("Rage Storm shape checks passed: spherical bounds, all-axis extent, hollow center, thin bands and finite mesh budget.");
    }
    private static void require(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
}
