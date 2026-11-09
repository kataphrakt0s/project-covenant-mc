package dev.covenant.rpg.skills;

import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Instant server blink. A swept player box follows the aim exactly; no wall sliding or endpoint tunneling. */
public final class Teleport {
    private static final double EPSILON = 1e-7;
    private Teleport() {}

    /** Read-only preflight, bounded to loaded chunks. Null means no paid movement is possible. */
    static Vec3 destination(ServerPlayer player, double range) {
        var level = player.level(); var box = player.getBoundingBox();
        var delta = player.getLookAngle().scale(range);
        if (!level.noCollision(player, box)) { return null; }
        double fraction = 1;
        var border = level.getWorldBorder();
        if (!border.isWithinBounds(box)) { return null; }
        fraction = bound(fraction, delta.x, box.minX, box.maxX, border.getMinX(), border.getMaxX());
        fraction = bound(fraction, delta.z, box.minZ, box.maxZ, border.getMinZ(), border.getMaxZ());
        fraction = bound(fraction, delta.y, box.minY, box.maxY, level.getMinY(), level.getMaxY());
        boolean bounded = fraction < 1;
        delta = delta.scale(Math.max(0, fraction));
        var swept = box.expandTowards(delta).inflate(EPSILON);
        // Never query collision shapes in an unloaded chunk, or force it to load for a skill.
        for (int x = net.minecraft.util.Mth.floor(swept.minX) >> 4; x <= net.minecraft.util.Mth.floor(swept.maxX) >> 4; x++) {
            for (int z = net.minecraft.util.Mth.floor(swept.minZ) >> 4; z <= net.minecraft.util.Mth.floor(swept.maxZ) >> 4; z++) {
                if (!level.hasChunk(x, z)) { return null; }
            }
        }
        fraction = 1;
        for (var shape : Entity.collectAllColliders(player, level, swept)) {
            for (var obstacle : shape.toAabbs()) { fraction = Math.min(fraction, contact(box, obstacle, delta)); }
        }
        double distance = delta.length();
        if (distance < .1) { return null; }
        var move = delta.scale(Math.max(0, fraction - (bounded || fraction < 1 ? .001 / distance : 0)));
        var targetBox = box.move(move);
        if (move.lengthSqr() < .01 || !border.isWithinBounds(targetBox) || targetBox.minY < level.getMinY()
                || targetBox.maxY > level.getMaxY() || !level.noCollision(player, targetBox)) { return null; }
        return player.position().add(move);
    }
    private static double bound(double current, double movement, double min, double max, double lower, double upper) {
        if (movement > EPSILON) { return Math.min(current, (upper - max) / movement); }
        if (movement < -EPSILON) { return Math.min(current, (lower - min) / movement); }
        return current;
    }
    /** Continuous AABB collision, preserving a straight path even for diagonal aim and thin voxel shapes. */
    private static double contact(AABB box, AABB obstacle, Vec3 movement) {
        double enter = Double.NEGATIVE_INFINITY, exit = Double.POSITIVE_INFINITY;
        for (var axis : net.minecraft.core.Direction.Axis.values()) {
            double speed = movement.get(axis);
            if (Math.abs(speed) < EPSILON) {
                if (box.max(axis) <= obstacle.min(axis) + EPSILON || box.min(axis) >= obstacle.max(axis) - EPSILON) { return 1; }
            } else {
                double a = (obstacle.min(axis) - box.max(axis)) / speed;
                double b = (obstacle.max(axis) - box.min(axis)) / speed;
                enter = Math.max(enter, Math.min(a, b)); exit = Math.min(exit, Math.max(a, b));
            }
        }
        return enter <= exit && exit > EPSILON && enter >= -EPSILON && enter <= 1 ? Math.max(0, enter) : 1;
    }
    static void execute(ServerPlayer player, Vec3 destination) {
        var old = player.position();
        player.connection.teleport(new PositionMoveRotation(destination, Vec3.ZERO, player.getYRot(), player.getXRot()), Set.of());
        player.setDeltaMovement(Vec3.ZERO); player.resetFallDistance();
        for (var point : new Vec3[] { old, destination }) {
            player.level().sendParticles(ParticleTypes.PORTAL, point.x, point.y + 1, point.z, 24, .3, .7, .3, .05);
        }
    }
}
