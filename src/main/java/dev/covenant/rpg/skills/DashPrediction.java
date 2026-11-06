package dev.covenant.rpg.skills;

import net.minecraft.world.phys.Vec3;

/** Small owner prediction model; physics/rendering stay native and this model never advances combos. */
public final class DashPrediction {
    private long sequence;
    private Vec3 step = Vec3.ZERO, destination = Vec3.ZERO;
    private int remaining;
    private boolean active, acknowledgement;

    /** Ignore stale/duplicate transitions. Distribute any starting position error over the short path. */
    public boolean accept(DashMotion motion, Vec3 current) {
        if (motion.sequence() <= sequence) { return false; }
        sequence = motion.sequence(); active = motion.active();
        destination = motion.position().add(motion.step().scale(motion.ticks()));
        // Incoming knockback returns control immediately; exceptional correction retains that impulse.
        remaining = active ? motion.ticks() : motion.velocity().lengthSqr() > 1e-12 ? 0
                : current.distanceToSqr(destination) > 1e-8 ? 2 : 0;
        step = active ? destination.subtract(current).scale(1.0 / remaining) : Vec3.ZERO;
        acknowledgement = !active;
        return true;
    }
    public boolean ownsTravel() { return active || acknowledgement; }
    public boolean active() { return active; }
    public Vec3 delta(Vec3 current) {
        if (remaining == 0) { return Vec3.ZERO; }
        return active ? step : destination.subtract(current).scale(1.0 / remaining);
    }
    public void advanced(Vec3 nextStep) {
        if (remaining == 0) { return; }
        remaining--;
        if (active) {
            step = nextStep;
            if (step.lengthSqr() < 1e-8) { remaining = 0; }
        }
    }
    public boolean readyToAcknowledge() { return acknowledgement && remaining == 0; }
    public long acknowledge() { acknowledgement = false; return sequence; }

    /** A native teleport supersedes the trajectory. Keep its sequence so stale starts cannot revive it. */
    public void cancel() {
        active = false; acknowledgement = false; remaining = 0;
        step = Vec3.ZERO; destination = Vec3.ZERO;
    }
}
