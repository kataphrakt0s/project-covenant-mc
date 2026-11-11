package dev.covenant.client;

import dev.covenant.rpg.skills.CombatParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Low, open movement silhouettes assembled from native glitter sprites; no camera overlay or world light. */
final class MovementParticle extends SimpleAnimatedParticle {
    private final float initialSize;

    private MovementParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                             boolean blink, double vx, double vy, double vz) {
        super(level, x, y, z, sprites, 0);
        // Collision keeps the short breakup from drifting through nearby walls or floors.
        hasPhysics = true;
        xd = vx; yd = vy; zd = vz;
        lifetime = blink ? 6 : 4;
        initialSize = blink ? .065f : .045f;
        quadSize = initialSize;
        setSize(initialSize * 2, initialSize * 2);
        setColor(blink ? 0xAD65FF : 0x69D890);
        setSprite(sprites.get(0, lifetime - 1));
    }

    static void register(RegisterParticleProvidersEvent event) {
        mote(event, CombatParticles.RETREAT_MOTE.get(), false);
        mote(event, CombatParticles.BLINK_MOTE.get(), true);
        retreat(event, CombatParticles.DISENGAGE_DEPARTURE.get(), false);
        retreat(event, CombatParticles.DISENGAGE_ARRIVAL.get(), true);
        blink(event, CombatParticles.TELEPORT_DEPARTURE.get(), false);
        blink(event, CombatParticles.TELEPORT_ARRIVAL.get(), true);
        event.registerSpriteSet(CombatParticles.DISENGAGE_TRAIL.get(), sprites ->
                (options, level, x, y, z, dx, dy, dz, random) -> {
                    // Two stationary flecks on this actual segment, below the owner's aiming line.
                    for (int i = 1; i <= 2; i++) {
                        double t = i / 3.0;
                        level.addParticle(CombatParticles.RETREAT_MOTE.get(), x + dx * t, y + dy * t + .18,
                                z + dz * t, 0, 0, 0);
                    }
                    return null;
                });
    }

    private static void mote(RegisterParticleProvidersEvent event, SimpleParticleType type, boolean blink) {
        event.registerSpriteSet(type, sprites -> (options, level, x, y, z, vx, vy, vz, random) ->
                new MovementParticle(level, x, y, z, sprites, blink, vx, vy, vz));
    }

    private static void retreat(RegisterParticleProvidersEvent event, SimpleParticleType type, boolean arrival) {
        event.registerSpriteSet(type, sprites -> (options, level, x, y, z, yaw, unused, ignored, random) -> {
            // Two open chevrons at ankle height point along the captured backward heading.
            double rightX = Math.cos(yaw), rightZ = Math.sin(yaw);
            double backX = Math.sin(yaw), backZ = -Math.cos(yaw);
            for (int side : new int[] {-1, 1}) {
                for (int i = 0; i < 4; i++) {
                    double across = side * (.12 + i * .08), back = .24 - i * .08;
                    double ox = rightX * across + backX * back, oz = rightZ * across + backZ * back;
                    double speed = arrival ? -.055 : .055;
                    level.addParticle(CombatParticles.RETREAT_MOTE.get(), x + ox, y + .16, z + oz,
                            ox * speed, 0, oz * speed);
                }
            }
            return null;
        });
    }

    private static void blink(RegisterParticleProvidersEvent event, SimpleParticleType type, boolean arrival) {
        event.registerSpriteSet(type, sprites -> (options, level, x, y, z, yaw, unused, ignored, random) -> {
            // Broken rectangular side brackets echo the portal icon. The center and eye line stay open.
            double rightX = Math.cos(yaw), rightZ = Math.sin(yaw);
            for (int side : new int[] {-1, 1}) {
                for (int i = 0; i < 6; i++) {
                    double across = side * (i < 4 ? .42 : .26);
                    double height = i < 4 ? .18 + i * .28 : (i == 4 ? .18 : 1.02);
                    double speed = arrival ? .045 : -.045;
                    level.addParticle(CombatParticles.BLINK_MOTE.get(), x + across * rightX, y + height, z + across * rightZ,
                            across * rightX * speed, (height - .6) * speed, across * rightZ * speed);
                }
            }
            return null;
        });
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (++age >= lifetime) { remove(); return; }
        move(xd, yd, zd);
        float remaining = 1 - (float) age / lifetime;
        quadSize = initialSize * (.5f + .5f * remaining);
        alpha = remaining;
        setSprite(sprites.get(age, lifetime - 1));
    }
}
