package dev.covenant.rpg.skills;

import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** Shared native collision step for the authoritative server and owning client's prediction. */
public final class DashMovement {
    private static final double EPSILON = 1e-8;
    /** Actual displacement and surface-adjusted next-tick velocity, shared by server and prediction. */
    public record Result(Vec3 travelled, Vec3 nextStep) {}
    private DashMovement() {}

    public static Result move(Player player, Vec3 step) { return move(player, step, null); }

    /** Visit each actual segment for dash hits; false stops immediately after a lifecycle/control callback. */
    public static Result move(Player player, Vec3 step, BiPredicate<AABB, Vec3> segment) {
        return move(player, step, segment, true);
    }

    /** Arcing hops clip vertical motion without converting it into additional horizontal speed. */
    public static Result move(Player player, Vec3 step, BiPredicate<AABB, Vec3> segment, boolean flattenPitch) {
        Vec3 start = player.position();
        player.resetFallDistance();
        double time = 1;
        Vec3 velocity = step;
        // At most three blocked axes plus one final free segment. Never bounce, climb, or add energy.
        for (int pass = 0; pass < 4 && time > EPSILON && velocity.lengthSqr() > EPSILON; pass++) {
            var body = player.getBoundingBox();
            Vec3 requested = velocity.scale(time);
            Vec3 clipped = Entity.collideBoundingBox(player, requested, body, player.level(), List.of());
            double x = contact(requested.x, clipped.x), y = contact(requested.y, clipped.y), z = contact(requested.z, clipped.z);
            double fraction = Math.min(1, Math.min(x, Math.min(y, z)));
            Vec3 before = player.position();
            player.move(MoverType.SELF, requested.scale(Math.clamp(fraction, 0, 1)));
            player.resetFallDistance();
            Vec3 travelled = player.position().subtract(before);
            if (segment != null && !segment.test(body, travelled)) { break; }
            if (fraction >= 1) { break; }
            boolean blockX = x <= fraction + EPSILON, blockY = y <= fraction + EPSILON,
                    blockZ = z <= fraction + EPSILON;
            // Floors/ceilings flatten pitch while preserving the captured horizontal heading.
            // Do this before stripping wall normals, so even simultaneous contacts cannot turn
            // blocked forward speed into a full-speed strafe. Wall slides retain only the
            // original unblocked components; never renormalize after losing a horizontal axis.
            if (blockY && flattenPitch) { velocity = new Vec3(velocity.x, 0, velocity.z).normalize().scale(velocity.length()); }
            velocity = new Vec3(blockX ? 0 : velocity.x, blockY ? 0 : velocity.y, blockZ ? 0 : velocity.z);
            time *= 1 - fraction;
        }
        player.resetFallDistance();
        return new Result(player.position().subtract(start), velocity);
    }

    private static double contact(double requested, double clipped) {
        return Math.abs(requested) > EPSILON && Math.abs(requested - clipped) > EPSILON
                ? Math.clamp(clipped / requested, 0, 1) : Double.POSITIVE_INFINITY;
    }
}
