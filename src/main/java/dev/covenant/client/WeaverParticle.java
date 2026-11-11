package dev.covenant.client;

import dev.covenant.rpg.skills.CombatParticles;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Elemental one-shot choreography using the existing energy/shard family and native limiting. */
final class WeaverParticle {
    private WeaverParticle() {}

    static void register(RegisterParticleProvidersEvent event) {
        discharge(event, CombatParticles.DISCHARGE_FIRE.get(), 0);
        discharge(event, CombatParticles.DISCHARGE_ICE.get(), 1);
        discharge(event, CombatParticles.DISCHARGE_LIGHTNING.get(), 2);
        event.registerSpriteSet(CombatParticles.THREEFOLD_CONSUME.get(), sprites -> (options, level, x, y, z, fire, ice, lightning, random) -> {
            double[] consumed = { fire, ice, lightning };
            for (int e = 0; e < 3; e++) {
                double strength = Math.clamp(consumed[e], 0, 1);
                if (!(strength > 0)) { continue; }
                int count = 2 + (int) Math.ceil(6 * strength);
                for (int i = 0; i < count; i++) {
                    double angle = e * Math.PI * 2 / 3 + (i / (double) count - .5) * .8;
                    double dx = Math.cos(angle), dz = Math.sin(angle);
                    level.addParticle(energy(e), x + dx * .65, y + (i % 2) * .1, z + dz * .65,
                            -dx * .1, 0, -dz * .1);
                }
            }
            return null;
        });
        event.registerSpriteSet(CombatParticles.THREEFOLD_EXPIRE.get(), sprites -> (options, level, x, y, z, mask, unused, ignored, random) -> {
            int elements = (int) mask & 7;
            for (int e = 0; e < 3; e++) {
                if ((elements & (1 << e)) == 0) { continue; }
                for (int i = 0; i < 3; i++) {
                    double angle = e * Math.PI * 2 / 3 + i * .15;
                    level.addParticle(energy(e), x + Math.cos(angle) * .25, y, z + Math.sin(angle) * .25,
                            Math.cos(angle) * .025, -.02, Math.sin(angle) * .025);
                }
            }
            return null;
        });
    }

    private static SimpleParticleType energy(int element) {
        return switch (element) {
            case 0 -> CombatParticles.FIRE_ENERGY.get();
            case 1 -> CombatParticles.ICE_ENERGY.get();
            default -> CombatParticles.ELECTRIC_ENERGY.get();
        };
    }

    private static void discharge(RegisterParticleProvidersEvent event, SimpleParticleType type, int element) {
        event.registerSpriteSet(type, sprites -> (options, level, x, y, z, radius, fill, full, random) -> {
            double range = Math.clamp(radius, .1, 16), strength = Math.clamp(fill, 0, 1);
            if (!(strength > 0)) { return null; }
            int count = 6 + (int) Math.ceil(18 * strength) + (full > .5 ? 4 : 0);
            for (int i = 0; i < count; i++) {
                double angle = i * Math.PI * 2 / count + element * .31;
                double dx = Math.cos(angle), dz = Math.sin(angle);
                double height = (i % 3 - 1) * range * .12;
                // Open radial spokes; no projectile-like branches that could imply additional targets.
                level.addParticle(energy(element), x + dx * range * .55, y + height, z + dz * range * .55,
                        dx * range * .08, height * .04, dz * range * .08);
                if (element == 1 && i % 4 == 0) {
                    level.addParticle(CombatParticles.ICE_SHARD.get(), x + dx * range * .7, y + height, z + dz * range * .7,
                            dx, .15, dz);
                }
            }
            return null;
        });
    }
}
