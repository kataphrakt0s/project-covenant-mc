package dev.covenant.client;

import dev.covenant.rpg.skills.CombatParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Short, moving volumes of light built from native particle sprites, with no item silhouette. */
final class EnergyParticle extends SimpleAnimatedParticle {
    private enum Kind { FIRE, ICE, ARCANE, ELECTRIC }
    private final Kind kind;
    private final float initialSize;
    private final float spin;

    private EnergyParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                           Kind kind, double vx, double vy, double vz) {
        super(level, x, y, z, sprites, 0);
        this.kind = kind;
        hasPhysics = false;
        xd = vx; yd = vy; zd = vz;
        lifetime = switch (kind) {
            case FIRE -> 5 + random.nextInt(3);
            case ICE -> 4 + random.nextInt(2);
            case ARCANE -> 4 + random.nextInt(2);
            case ELECTRIC -> 2 + random.nextInt(3);
        };
        initialSize = (kind == Kind.ELECTRIC ? .045f : .13f) * (1 + random.nextFloat() * .6f);
        quadSize = initialSize;
        setSize(initialSize * 2, initialSize * 2);
        roll = oRoll = random.nextFloat() * (float) (Math.PI * 2);
        spin = (random.nextFloat() - .5f) * (kind == Kind.FIRE ? .18f : .5f);
        setColor(switch (kind) {
            case FIRE -> random.nextBoolean() ? 0xFF4164 : 0xFFD4CF;
            case ICE -> random.nextBoolean() ? 0x91DEFF : 0xE4FAFF;
            case ARCANE -> random.nextBoolean() ? 0xAA74FF : 0xEADFFF;
            case ELECTRIC -> random.nextBoolean() ? 0x258AFF : 0xB5ECFF;
        });
        setSprite(sprites.get(0, lifetime - 1));
    }

    static void register(RegisterParticleProvidersEvent event) {
        register(event, CombatParticles.FIRE_ENERGY.get(), Kind.FIRE);
        register(event, CombatParticles.ICE_ENERGY.get(), Kind.ICE);
        register(event, CombatParticles.ARCANE_ENERGY.get(), Kind.ARCANE);
        register(event, CombatParticles.ELECTRIC_ENERGY.get(), Kind.ELECTRIC);
        registerBurst(event, CombatParticles.FIRE_COLLISION.get(), CombatParticles.FIRE_ENERGY.get(), Kind.FIRE);
        registerBurst(event, CombatParticles.ICE_COLLISION.get(), CombatParticles.ICE_ENERGY.get(), Kind.ICE);
        registerBurst(event, CombatParticles.LIGHTNING_COLLISION.get(), CombatParticles.ELECTRIC_ENERGY.get(), Kind.ELECTRIC);
    }

    private static void registerBurst(RegisterParticleProvidersEvent event, SimpleParticleType collision,
                                      SimpleParticleType energy, Kind kind) {
        event.registerSpriteSet(collision, sprites -> (options, level, x, y, z, vx, vy, vz, random) -> {
            // One existing authoritative collision packet; bounded breakup is generated locally.
            for (int i = 0; i < 10; i++) {
                level.addParticle(energy, x, y, z, (random.nextDouble() - .5) * .24,
                        (random.nextDouble() - .5) * .24, (random.nextDouble() - .5) * .24);
            }
            return new EnergyParticle(level, x, y, z, sprites, kind, 0, 0, 0);
        });
    }

    private static void register(RegisterParticleProvidersEvent event, SimpleParticleType type, Kind kind) {
        event.registerSpriteSet(type, sprites -> (options, level, x, y, z, vx, vy, vz, random) ->
                new EnergyParticle(level, x, y, z, sprites, kind, vx, vy, vz));
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z; oRoll = roll;
        if (++age >= lifetime) { remove(); return; }
        move(xd, yd, zd);
        xd *= .86; yd *= .86; zd *= .86;
        if (kind == Kind.FIRE) { yd += .012; }
        roll += spin;
        float remaining = 1 - (float) age / lifetime;
        quadSize = initialSize * (.3f + .7f * remaining);
        alpha = remaining * remaining;
        if (kind == Kind.FIRE) { gCol *= .8f; bCol *= .88f; }
        setSprite(sprites.get(age, lifetime - 1));
    }
}
