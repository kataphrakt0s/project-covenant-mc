package dev.covenant.rpg.skills;

import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Instant server blink. Sweep terrain along aim; floor/ceiling contact clips pitch, walls stop travel. */
public final class Teleport {
    private static final double EPSILON = 1e-7;
    private Teleport() {}

    /** Read-only preflight, bounded to loaded chunks. Null means no paid movement is possible. */
    static Vec3 destination(ServerPlayer player, double range) {
        var level = player.level(); var box = player.getBoundingBox();
        var delta = player.getLookAngle().scale(range);
        if (!clear(player, box)) { return null; }
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
        var move = Vec3.ZERO;
        // One pitched segment and, on floor/ceiling contact, one horizontal remainder. The same
        // continuous sweep protects both segments from thin walls and diagonal corner tunneling.
        for (int pass = 0; pass < 2 && delta.lengthSqr() > EPSILON * EPSILON; pass++) {
            var contact = new Contact(1, false);
            for (var shape : level.getBlockCollisions(player, box.expandTowards(delta))) {
                for (var obstacle : shape.toAabbs()) {
                    var hit = contact(box, obstacle, delta);
                    if (hit.fraction < contact.fraction - EPSILON) { contact = hit; }
                    else if (Math.abs(hit.fraction - contact.fraction) <= EPSILON && !hit.vertical) {
                        contact = new Contact(contact.fraction, false); // A simultaneous wall always stops travel.
                    }
                }
            }
            double distance = delta.length();
            var segment = delta.scale(Math.max(0, contact.fraction - (bounded || contact.fraction < 1 ? .000001 / distance : 0)));
            move = move.add(segment); box = box.move(segment);
            if (!contact.vertical || contact.fraction >= 1) { break; }
            delta = new Vec3(delta.x, 0, delta.z).scale(1 - contact.fraction);
        }
        var targetBox = box;
        if (move.lengthSqr() < .01 || !border.isWithinBounds(targetBox) || targetBox.minY < level.getMinY()
                || targetBox.maxY > level.getMaxY() || !clear(player, targetBox)) { return null; }
        return player.position().add(move);
    }
    private static boolean clear(ServerPlayer player, AABB box) {
        return !player.level().getBlockCollisions(player, box).iterator().hasNext();
    }
    private static double bound(double current, double movement, double min, double max, double lower, double upper) {
        if (movement > EPSILON) { return Math.min(current, (upper - max) / movement); }
        if (movement < -EPSILON) { return Math.min(current, (lower - min) / movement); }
        return current;
    }
    /** Continuous AABB collision, preserving a straight path even for diagonal aim and thin voxel shapes. */
    private record Contact(double fraction, boolean vertical) {}
    private static Contact contact(AABB box, AABB obstacle, Vec3 movement) {
        double enter = Double.NEGATIVE_INFINITY, exit = Double.POSITIVE_INFINITY;
        boolean vertical = false;
        for (var axis : net.minecraft.core.Direction.Axis.values()) {
            double speed = movement.get(axis);
            if (Math.abs(speed) < EPSILON) {
                if (box.max(axis) <= obstacle.min(axis) + EPSILON || box.min(axis) >= obstacle.max(axis) - EPSILON) { return new Contact(1, false); }
            } else {
                double a = (obstacle.min(axis) - box.max(axis)) / speed;
                double b = (obstacle.max(axis) - box.min(axis)) / speed;
                double near = Math.min(a, b);
                if (near > enter + EPSILON) { vertical = axis == net.minecraft.core.Direction.Axis.Y; }
                else if (Math.abs(near - enter) <= EPSILON && axis != net.minecraft.core.Direction.Axis.Y) { vertical = false; }
                enter = Math.max(enter, near); exit = Math.min(exit, Math.max(a, b));
            }
        }
        return enter <= exit && exit > EPSILON && enter >= -EPSILON && enter <= 1
                ? new Contact(Math.max(0, enter), vertical) : new Contact(1, false);
    }
    static void execute(ServerPlayer player, Vec3 destination) {
        var old = player.position();
        SkillSounds.play(player.level(), old, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, .35f, 1.3f);
        player.connection.teleport(new PositionMoveRotation(destination, Vec3.ZERO, player.getYRot(), player.getXRot()), Set.of());
        player.setDeltaMovement(Vec3.ZERO); player.resetFallDistance();
        if (old.distanceToSqr(destination) >= 4) {
            SkillSounds.play(player.level(), destination, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, .35f, 1.5f);
        }
        for (var point : new Vec3[] { old, destination }) {
            player.level().sendParticles(ParticleTypes.PORTAL, point.x, point.y + 1, point.z, 24, .3, .7, .3, .05);
        }
    }
}
