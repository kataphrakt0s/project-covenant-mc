package dev.covenant.client;

import dev.covenant.rpg.skills.CombatParticles;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** One-shot native particle emitters; their children use the existing four-tick Reaver sprites. */
final class ReaverParticle {
    private ReaverParticle() {}

    static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CombatParticles.BATTLE_CRY_PULSE.get(), sprites -> (options, level, x, y, z, radius, unused, ignored, random) -> {
            double r = Math.clamp(radius, .1, 32);
            // A single shout snapshot, not a sustained aura boundary. Radius follows live server tuning.
            for (int i = 0; i < 32; i++) {
                double angle = i * Math.PI / 16;
                level.addParticle(CombatParticles.COMBO_CRIMSON.get(), x + Math.cos(angle) * r * .8, y, z + Math.sin(angle) * r * .8,
                        Math.cos(angle) * r / 30, .015, Math.sin(angle) * r / 30);
            }
            return null;
        });
        event.registerSpriteSet(CombatParticles.BATTLE_CRY_RECEIVED.get(), sprites -> (options, level, x, y, z, a, b, c, random) -> {
            for (int i = 0; i < 6; i++) {
                double angle = i * Math.PI / 3;
                level.addParticle(i % 2 == 0 ? CombatParticles.COMBO_STEEL.get() : CombatParticles.COMBO_CRIMSON.get(),
                        x + Math.cos(angle) * .28, y, z + Math.sin(angle) * .28, 0, .045, 0);
            }
            return null;
        });
        event.registerSpriteSet(CombatParticles.STORM_DISSIPATE.get(), sprites -> (options, level, x, y, z, radius, unused, ignored, random) -> {
            double r = Math.clamp(radius, .1, 8) * .65;
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6;
                double height = (i % 3 - 1) * r * .5;
                level.addParticle(CombatParticles.COMBO_CRIMSON.get(), x + Math.cos(angle) * r, y + height, z + Math.sin(angle) * r,
                        -Math.cos(angle) * .05, -height * .04, -Math.sin(angle) * .05);
            }
            return null;
        });
    }
}
