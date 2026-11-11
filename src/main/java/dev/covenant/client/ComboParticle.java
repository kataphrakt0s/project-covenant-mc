package dev.covenant.client;

import dev.covenant.rpg.skills.CombatParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Sparse steel/crimson strokes, using native sprites and local four-tick lifetimes. */
final class ComboParticle extends SimpleAnimatedParticle {
    private ComboParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                          boolean crimson, double vx, double vy, double vz) {
        super(level, x, y, z, sprites, 0);
        hasPhysics = true;
        xd = vx; yd = vy; zd = vz;
        lifetime = 4;
        quadSize = .075f;
        setSize(.15f, .15f);
        setColor(crimson ? 0xF03758 : 0xCFDCE8);
        setSprite(sprites.get(0, lifetime - 1));
    }

    static void register(RegisterParticleProvidersEvent event) {
        mote(event, CombatParticles.COMBO_STEEL.get(), false);
        mote(event, CombatParticles.COMBO_CRIMSON.get(), true);
        trail(event, CombatParticles.LUNGE_TRAIL.get(), false);
        trail(event, CombatParticles.RUSH_TRAIL.get(), true);
        sweep(event, CombatParticles.RIP_SWEEP.get(), false);
        sweep(event, CombatParticles.RUSH_FINISHER.get(), true);
        contact(event, CombatParticles.COMBO_CONTACT.get(), false);
        contact(event, CombatParticles.COMBO_CONSUME.get(), true);
        event.registerSpriteSet(CombatParticles.LUNGE_STRIKE.get(), sprites ->
                (options, level, x, y, z, dx, dy, dz, random) -> {
                    // A compact jab at the hand communicates the action, not an invented melee range.
                    for (int i = 0; i < 5; i++) {
                        double along = i * .08;
                        level.addParticle(CombatParticles.COMBO_STEEL.get(), x + dx * along, y + dy * along, z + dz * along,
                                dx * .025, dy * .025, dz * .025);
                    }
                    return null;
                });
    }

    private static void mote(RegisterParticleProvidersEvent event, SimpleParticleType type, boolean crimson) {
        event.registerSpriteSet(type, sprites -> (options, level, x, y, z, vx, vy, vz, random) ->
                new ComboParticle(level, x, y, z, sprites, crimson, vx, vy, vz));
    }

    private static void trail(RegisterParticleProvidersEvent event, SimpleParticleType type, boolean rush) {
        event.registerSpriteSet(type, sprites -> (options, level, x, y, z, dx, dy, dz, random) -> {
            // Samples lie on the real segment, even for vertical travel and terrain slides.
            for (int i = 1; i <= 3; i++) {
                double t = i / 4.0;
                level.addParticle((rush ? CombatParticles.COMBO_CRIMSON : CombatParticles.COMBO_STEEL).get(),
                        x + dx * t, y + dy * t, z + dz * t, 0, 0, 0);
            }
            return null;
        });
    }

    private static void sweep(RegisterParticleProvidersEvent event, SimpleParticleType type, boolean finisher) {
        event.registerSpriteSet(type, sprites -> (options, level, x, y, z, yaw, radius, unused, random) -> {
            double boundedRadius = Math.clamp(radius, 0, 3);
            // Both stages are genuinely circular. Opening cuts tangentially; the finisher closes inward.
            // Twenty-four small samples leave the center and targets visible, without a filled disk.
            for (int i = 0; i < 24; i++) {
                double angle = yaw + i * Math.PI / 12;
                double cx = Math.cos(angle), sz = Math.sin(angle);
                var mote = finisher && i % 3 == 0 ? CombatParticles.COMBO_STEEL : CombatParticles.COMBO_CRIMSON;
                level.addParticle(mote.get(), x + cx * boundedRadius, y, z + sz * boundedRadius,
                        finisher ? -cx * .1 : -sz * .04, 0, finisher ? -sz * .1 : cx * .04);
            }
            return null;
        });
    }

    private static void contact(RegisterParticleProvidersEvent event, SimpleParticleType type, boolean consume) {
        event.registerSpriteSet(type, sprites -> (options, level, x, y, z, unusedX, unusedY, unusedZ, random) -> {
            // Accepted hit: four pale chips. Qualifying bleed consumption: six crimson inward flecks.
            int count = consume ? 6 : 4;
            for (int i = 0; i < count; i++) {
                double angle = i * Math.PI * 2 / count;
                double radius = consume ? .26 : .08;
                double speed = consume ? -.055 : .035;
                level.addParticle((consume ? CombatParticles.COMBO_CRIMSON : CombatParticles.COMBO_STEEL).get(),
                        x + Math.cos(angle) * radius, y + Math.sin(angle) * radius, z,
                        Math.cos(angle) * speed, Math.sin(angle) * speed, 0);
            }
            return null;
        });
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (++age >= lifetime) { remove(); return; }
        move(xd, yd, zd);
        alpha = 1 - (float) age / lifetime;
        setSprite(sprites.get(age, lifetime - 1));
    }
}
