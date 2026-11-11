package dev.covenant.rpg.skills;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Bounded cosmetic packets at committed movement boundaries; no predicted destinations or saved state. */
final class MovementVisuals {
    private MovementVisuals() {}

    static void disengageDeparture(ServerPlayer player) {
        endpoint(player, CombatParticles.DISENGAGE_DEPARTURE.get(), player.position());
    }

    static void disengageArrival(ServerPlayer player, double departureYaw) {
        player.level().sendParticles(CombatParticles.DISENGAGE_ARRIVAL.get(), player.getX(), player.getY(), player.getZ(),
                0, departureYaw, 0, 0, 1);
    }

    /** One packet per actual collision-resolved segment, including slides; zero travel is silent. */
    static void disengageStep(ServerLevel level, Vec3 start, Vec3 travelled) {
        if (travelled.lengthSqr() < 1e-8) { return; }
        level.sendParticles(CombatParticles.DISENGAGE_TRAIL.get(), start.x, start.y, start.z,
                0, travelled.x, travelled.y, travelled.z, 1);
    }

    static void teleport(ServerPlayer player, Vec3 departure) {
        endpoint(player, CombatParticles.TELEPORT_DEPARTURE.get(), departure);
        endpoint(player, CombatParticles.TELEPORT_ARRIVAL.get(), player.position());
    }

    private static void endpoint(ServerPlayer player, SimpleParticleType type, Vec3 point) {
        // Native count-zero auxiliary data carries orientation, with normal distance/particle limiting.
        player.level().sendParticles(type, point.x, point.y, point.z, 0,
                Math.toRadians(player.getYRot()), 0, 0, 1);
    }
}
