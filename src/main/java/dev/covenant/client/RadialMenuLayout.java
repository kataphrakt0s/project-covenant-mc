package dev.covenant.client;

/** Shared wheel geometry in GUI coordinates. Slot zero is at the top, then clockwise. */
final class RadialMenuLayout {
    static final int SLOT_COUNT = 10;
    static final double SLOT_ANGLE = Math.TAU / SLOT_COUNT;
    static final float INNER_RADIUS_RATIO = 0.43f;

    private RadialMenuLayout() {}

    static int radius(int width, int height) {
        return Math.max(1, Math.min(108, Math.min(width / 2 - 12, height / 2 - 32)));
    }

    static double angle(int slot) {
        return -Math.PI / 2 + slot * SLOT_ANGLE;
    }

    static int hoveredSlot(double x, double y, double radius) {
        double distanceSquared = x * x + y * y;
        double innerRadius = radius * INNER_RADIUS_RATIO;
        if (distanceSquared < innerRadius * innerRadius || distanceSquared > radius * radius) {
            return -1;
        }
        double angle = Math.atan2(y, x) + Math.PI / 2 + SLOT_ANGLE / 2;
        return (int) Math.floor((angle + Math.TAU) % Math.TAU / SLOT_ANGLE) % SLOT_COUNT;
    }
}
