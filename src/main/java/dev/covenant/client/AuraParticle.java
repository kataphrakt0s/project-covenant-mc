package dev.covenant.client;

import dev.covenant.rpg.skills.Auras;
import dev.covenant.rpg.skills.CombatParticles;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Small, open recipient shapes, sized to the real entity; no persistent field or camera overlay. */
final class AuraParticle {
    private enum Moment { RELEASE, RECEIVED, CONSUMED, EXPIRED, ABSORB }
    private static final Auras.Kind[] KINDS = Auras.Kind.values();
    private AuraParticle() {}

    static void register(RegisterParticleProvidersEvent event) {
        register(event, CombatParticles.AURA_RELEASE.get(), Moment.RELEASE);
        register(event, CombatParticles.AURA_RECEIVED.get(), Moment.RECEIVED);
        register(event, CombatParticles.AURA_CONSUMED.get(), Moment.CONSUMED);
        register(event, CombatParticles.AURA_EXPIRED.get(), Moment.EXPIRED);
        register(event, CombatParticles.AURA_ABSORB.get(), Moment.ABSORB);
    }

    private static SimpleParticleType color(Auras.Kind kind) {
        return switch (kind) {
            case DODGE, SWIFTNESS -> CombatParticles.RETREAT_MOTE.get();
            case RIPPING, RAGING -> CombatParticles.COMBO_CRIMSON.get();
            case FORTITUDE -> CombatParticles.COMBO_STEEL.get();
            case ICY_VEINS, FREEZING -> CombatParticles.ICE_ENERGY.get();
            case FLAME_CLOAK, IMMOLATION -> CombatParticles.FIRE_ENERGY.get();
            case SHOCK -> CombatParticles.ELECTRIC_ENERGY.get();
            default -> CombatParticles.ARCANE_ENERGY.get();
        };
    }

    private static void register(RegisterParticleProvidersEvent event, SimpleParticleType type, Moment moment) {
        event.registerSpriteSet(type, sprites -> (options, level, x, y, z, code, width, height, random) -> {
            if (!Double.isFinite(code) || code != (int) code || code < 0 || code >= 11) { return null; }
            var kind = KINDS[(int) code];
            double r = Math.clamp(width * .6, .25, 1.5), h = Math.clamp(height, .3, 3);
            int count = moment == Moment.RELEASE ? 12 : moment == Moment.EXPIRED || moment == Moment.ABSORB ? 4 : 8;
            for (int i = 0; i < count; i++) {
                double angle = i * Math.PI * 2 / count;
                double dx = Math.cos(angle), dz = Math.sin(angle);
                double px = dx * r, pz = dz * r, py = h * .45;
                double vx = dx * .025, vz = dz * .025, vy = .035;
                if (moment == Moment.RELEASE) {
                    // Compact cast stamp, not a claimed area boundary or hit indicator.
                    px = dx * (r + .2); pz = dz * (r + .2); py = .12; vy = 0;
                } else if (moment == Moment.RECEIVED) {
                    switch (kind) {
                        case DODGE -> { px *= 1.3; py += (i % 2) * .1; vy = 0; }
                        case SWIFTNESS -> { px = (i < 4 ? -1 : 1) * r; pz = (i % 4 - 1.5) * .09; py = .15 + (i % 4) * .09; vy = .08; }
                        case RIPPING -> { vx = -dx * .055; vz = -dz * .055; py += (i % 2) * .15; }
                        case FORTITUDE -> { px = (i < 4 ? -1 : 1) * r; pz = 0; py = h * (.2 + (i % 4) * .15); vx = vz = 0; vy = .01; }
                        case RAGING -> { py = .2 + (i % 4) * .12; vy = .09; }
                        case ICY_VEINS -> { py = h * (.25 + (i % 2) * .3); vx = -dx * .025; vz = -dz * .025; }
                        case FREEZING -> { py = .12 + (i % 2) * h * .3; vy = -.015; }
                        case FLAME_CLOAK -> { py = h * (.2 + i / 16.0); vy = .065; }
                        case SHOCK -> { px *= i % 2 == 0 ? 1 : .6; py += (i % 3 - 1) * .15; vy = 0; }
                        case IMMOLATION -> { py = .18; vx = dx * .06; vz = dz * .06; }
                        case ENCHANTING -> { px = (i % 4 - 1.5) * r / 2; pz = i < 4 ? -r : r; py += (i % 2) * .13; }
                        default -> { }
                    }
                } else if (moment == Moment.EXPIRED) {
                    vy = -.035; vx *= .5; vz *= .5;
                } else {
                    vx *= moment == Moment.CONSUMED ? 3 : 1; vz *= moment == Moment.CONSUMED ? 3 : 1;
                    vy = moment == Moment.CONSUMED ? -.015 : .02;
                }
                level.addParticle(color(kind), x + px, y + py, z + pz, vx, vy, vz);
            }
            return null;
        });
    }
}
