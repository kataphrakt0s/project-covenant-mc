package dev.covenant.client;

/** Executed by Gradle's radialMenuTest task with no Minecraft or test framework bootstrap. */
public final class RadialMenuLayoutTest {
    public static void main(String[] args) {
        for (int slot = 0; slot < RadialMenuLayout.SLOT_COUNT; slot++) {
            double angle = RadialMenuLayout.angle(slot);
            expect(slot, hit(angle, 75), "slot center " + slot);
            expect(slot, hit(angle - RadialMenuLayout.SLOT_ANGLE * 0.49, 75), "left edge " + slot);
            expect(slot, hit(angle + RadialMenuLayout.SLOT_ANGLE * 0.49, 75), "right edge " + slot);
            expect((slot + 1) % 10, hit(angle + RadialMenuLayout.SLOT_ANGLE * 0.51, 75), "next slot " + slot);
            expect(-1, hit(angle, 40), "dead zone " + slot);
            expect(-1, hit(angle, 101), "outside wheel " + slot);
        }
        expect(-1, RadialMenuLayout.hoveredSlot(0, 0, 100), "center has no selection");
        expect(0, RadialMenuLayout.hoveredSlot(0, -100, 100), "outer boundary");
        expect(108, RadialMenuLayout.radius(960, 540), "large window caps radius");
        for (int[] size : new int[][] {{320, 240}, {427, 240}, {640, 360}, {1920, 1080}, {240, 320}}) {
            int radius = RadialMenuLayout.radius(size[0], size[1]);
            if (radius <= 0 || radius > 108 || radius + 12 > size[0] / 2 || radius + 32 > size[1] / 2) {
                throw new AssertionError("Wheel exceeds viewport " + size[0] + "x" + size[1]);
            }
        }
        System.out.println("Radial menu geometry checks passed.");
    }

    private static int hit(double angle, double distance) {
        return RadialMenuLayout.hoveredSlot(Math.cos(angle) * distance, Math.sin(angle) * distance, 100);
    }

    private static void expect(int expected, int actual, String description) {
        if (expected != actual) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }
}
