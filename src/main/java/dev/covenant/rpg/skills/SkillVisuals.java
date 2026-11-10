package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Bounded native-particle effects. No client classes, gameplay state, or independent damage pipeline. */
public final class SkillVisuals {
    /** Cosmetic bow-pose duration: six game ticks (0.3 seconds at 20 TPS), with no firing delay. */
    public static final int BOW_DRAW_TICKS = 6;
    private static final DustParticleOptions BLUE = new DustParticleOptions(0x80CAFF, 0.8f);
    private static final DustParticleOptions YELLOW = new DustParticleOptions(0xFFF0A0, 0.6f);
    private static final DustParticleOptions RED = new DustParticleOptions(0xE34336, 0.8f);
    private static final DustParticleOptions FIRE = new DustParticleOptions(0xFF762B, 0.8f);
    private static final DustParticleOptions ICE = new DustParticleOptions(0xC2F4FF, 0.65f);
    private static final DustParticleOptions LIGHTNING = new DustParticleOptions(0xFFF4C2, 0.6f);
    private static final int PIERCING_STYLE = 0;
    private static final int AIMED_STYLE = 1;
    private static final int ARCANE_STYLE = 2;
    private static final int MULTISHOT_STYLE = 3;

    private SkillVisuals() {}

    static void elementalTrail(ElementalBolt bolt) {
        boolean fire = bolt.skill() == SkillDefinition.FIRE_BOLT;
        trail(bolt, fire ? FIRE : ICE, 2);
        if (bolt.tickCount % 3 == 0) {
            bolt.level().addParticle(fire ? ParticleTypes.SMALL_FLAME : ParticleTypes.SNOWFLAKE,
                    bolt.getX(), bolt.getY(), bolt.getZ(), 0, 0, 0);
        }
    }

    static void elementalImpact(ServerLevel level, Vec3 pos, SkillDefinition skill) {
        boolean fire = skill == SkillDefinition.FIRE_BOLT;
        level.sendParticles(fire ? FIRE : ICE, pos.x, pos.y, pos.z, 6, 0.12, 0.12, 0.12, 0);
        level.sendParticles(fire ? ParticleTypes.SMALL_FLAME : ParticleTypes.SNOWFLAKE,
                pos.x, pos.y, pos.z, 3, 0.1, 0.1, 0.1, 0.015);
    }

    static void lightning(ServerLevel level, Vec3 start, Vec3 end) {
        var delta = end.subtract(start);
        var visual = new LightningVisual(level.dimension().identifier(), start, end);
        for (var player : level.players()) {
            double t = delta.lengthSqr() == 0 ? 0 : Math.clamp(player.position().subtract(start).dot(delta) / delta.lengthSqr(), 0, 1);
            if (player.position().distanceToSqr(start.add(delta.scale(t))) <= 32 * 32 && player.connection.hasChannel(LightningVisual.TYPE)) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, visual);
            }
        }
    }

    static void trapSnap(ServerLevel level, TrapEntity trap) {
        var particle = switch (trap.kind()) {
            case BEAR -> ParticleTypes.CRIT;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case FLAME -> ParticleTypes.SMALL_FLAME;
            case BLAST -> ParticleTypes.EXPLOSION;
        };
        level.sendParticles(particle,
                trap.getX(), trap.getY() + 0.2, trap.getZ(), trap.kind() == TrapKind.BLAST ? 1 : 8,
                0.15, 0.1, 0.15, 0.01);
        if (trap.kind() == TrapKind.FROST) {
            level.sendParticles(ICE, trap.getX(), trap.getY() + .2, trap.getZ(), 12, .25, .1, .25, .04);
        } else if (trap.kind() == TrapKind.BEAR) {
            // Opposing inward contact streaks reinforce the mechanical snap, never imply a lasting area.
            for (int side = -1; side <= 1; side += 2) {
                level.sendParticles(ParticleTypes.CRIT, trap.getX(), trap.getY() + .15,
                        trap.getZ() + side * .3, 0, 0, 0, -side, .12);
            }
        } else if (trap.kind() == TrapKind.FLAME) {
            level.sendParticles(FIRE, trap.getX(), trap.getY() + .15, trap.getZ(), 6, .2, .05, .2, .02);
        } else if (trap.kind() == TrapKind.BLAST) {
            // Short outward pressure/debris, without TNT fuse smoke or a persistent damage ring.
            level.sendParticles(ParticleTypes.POOF, trap.getX(), trap.getY() + .15, trap.getZ(), 8, .2, .08, .2, .08);
        }
    }

    static void frostExpiry(TrapEntity trap) {
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            trap.level().addParticle(ParticleTypes.SNOWFLAKE, trap.getX() + Math.cos(angle) * .35,
                    trap.getY() + .08, trap.getZ() + Math.sin(angle) * .35, 0, .015, 0);
        }
    }

    static void flameExpiry(TrapEntity trap) {
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 2 * i / 4;
            trap.level().addParticle(ParticleTypes.SMOKE, trap.getX() + Math.cos(angle) * .2,
                    trap.getY() + .08, trap.getZ() + Math.sin(angle) * .2, 0, .01, 0);
        }
    }

    static void bowDraw(ServerPlayer player, long ticks) {
        player.setData(RpgData.BOW_DRAW_DURATION, ticks);
        player.setData(RpgData.BOW_DRAW_UNTIL, player.level().getGameTime() + ticks);
    }

    static void charge(ServerPlayer player, SkillDefinition skill) {
        if (player.tickCount % 2 != 0) { return; }
        var hand = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.25, 0);
        var dust = skill == SkillDefinition.FIRE_BOLT ? FIRE : skill == SkillDefinition.ICE_BOLT ? ICE
                : skill == SkillDefinition.LIGHTNING_BOLT ? LIGHTNING : BLUE;
        var accent = skill == SkillDefinition.FIRE_BOLT ? ParticleTypes.SMALL_FLAME
                : skill == SkillDefinition.ICE_BOLT ? ParticleTypes.SNOWFLAKE
                : skill == SkillDefinition.LIGHTNING_BOLT ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD;
        player.level().sendParticles(dust, hand.x, hand.y, hand.z, 2, 0.08, 0.08, 0.08, 0);
        player.level().sendParticles(accent, hand.x, hand.y, hand.z, 1, 0.04, 0.04, 0.04, 0.005);
    }

    static void rageCharge(ServerPlayer player) {
        if (player.tickCount % 2 != 0) { return; }
        var hand = player.getEyePosition().add(player.getLookAngle().scale(.6)).add(0, -.25, 0);
        player.level().sendParticles(RED, hand.x, hand.y, hand.z, 4, .15, .15, .15, 0);
    }

    static void cleave(ServerPlayer player, double range) {
        sweep(player, range, false);
    }

    /** Rip and Rush affects the full circle; its cue must not imply Cleave's frontal restriction. */
    static void circularSweep(ServerPlayer player, double range) {
        sweep(player, range, true);
    }

    private static void sweep(ServerPlayer player, double range, boolean circular) {
        var level = player.level();
        for (int i = 0; i < 24; i++) {
            double angle = circular ? Math.PI * 2 * i / 24
                    : Math.toRadians(player.getYRot() + 90 - Skills.CLEAVE_HALF_ANGLE_DEGREES
                            + i * (2 * Skills.CLEAVE_HALF_ANGLE_DEGREES) / 23.0);
            double x = player.getX() + Math.cos(angle) * range;
            double z = player.getZ() + Math.sin(angle) * range;
            level.sendParticles(RED, x, player.getY() + 0.7, z, 1, 0, 0, 0, 0);
            if (i % 6 == 0) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        player.getX() + Math.cos(angle) * range * 0.5, player.getY() + 0.9,
                        player.getZ() + Math.sin(angle) * range * 0.5, 1, 0, 0, 0, 0);
            }
        }
    }

    static void cleaveHit(ServerLevel level, LivingEntity target) {
        level.sendParticles(RED, target.getX(), target.getY(0.5), target.getZ(), 7, 0.2, 0.25, 0.2, 0);
    }

    static void bleedTick(ServerLevel level, LivingEntity target) {
        level.sendParticles(RED, target.getX(), target.getY(0.5), target.getZ(), 2, 0.15, 0.2, 0.15, 0);
    }

    static void boltImpact(ServerLevel level, Vec3 position) {
        level.sendParticles(BLUE, position.x, position.y, position.z, 8, 0.12, 0.12, 0.12, 0);
        level.sendParticles(ParticleTypes.END_ROD, position.x, position.y, position.z, 3, 0.05, 0.05, 0.05, 0.025);
    }

    /** Called only on clients. A tracked projectile supplies its own lifecycle; no recurring particle packets. */
    static void boltTrail(Entity bolt) {
        trail(bolt, BLUE, 3);
        if (bolt.tickCount % 2 == 0) {
            bolt.level().addParticle(ParticleTypes.END_ROD, bolt.getX(), bolt.getY(), bolt.getZ(), 0, 0, 0);
        }
    }

    /** Cosmetic style is independent of saved projectile provenance and supplied before initial tracking. */
    static void markArrow(AbstractArrow arrow, SkillDefinition skill) {
        int style = skill == SkillDefinition.ARCANE_ARROW ? ARCANE_STYLE
                : skill == SkillDefinition.AIMED_SHOT ? AIMED_STYLE
                : skill == SkillDefinition.MULTISHOT ? MULTISHOT_STYLE : PIERCING_STYLE;
        arrow.setData(RpgData.SKILL_SHOT_STYLE, style);
        arrow.setData(RpgData.SKILL_SHOT, true);
    }

    /** Client entity callback checks only marked arrows, without scanning the world's entities. */
    public static void arrowTick(EntityTickEvent.Post event) {
        try (var zone = Profiler.get().zone("covenant:arrow_visuals")) {
            if (event.getEntity() instanceof AbstractArrow arrow && arrow.level().isClientSide()
                    && !arrow.isRemoved() && arrow.tickCount <= 100
                    && arrow.hasData(RpgData.SKILL_SHOT) && arrow.getData(RpgData.SKILL_SHOT)) {
                int style = arrow.getData(RpgData.SKILL_SHOT_STYLE);
                // Five-arrow fans share a deliberately small total particle budget.
                trail(arrow, style == ARCANE_STYLE ? BLUE : YELLOW, style == MULTISHOT_STYLE ? 1 : 3);
                if (style == ARCANE_STYLE && arrow.tickCount % 3 == 0) {
                    arrow.level().addParticle(ParticleTypes.END_ROD, arrow.getX(), arrow.getY(), arrow.getZ(), 0, 0, 0);
                }
            }
        }
    }

    /** Run after other impact handlers so canceled collisions do not play an impact effect. */
    public static void arrowImpact(ProjectileImpactEvent event) {
        try (var zone = Profiler.get().zone("covenant:arrow_impact_visuals")) {
            var arrow = event.getProjectile();
            if (!(arrow instanceof AbstractArrow nativeArrow) || !(arrow.level() instanceof ServerLevel level)
                    || !arrow.hasData(RpgData.SKILL_SHOT) || !arrow.getData(RpgData.SKILL_SHOT)) { return; }
            var hit = event.getRayTraceResult().getLocation();
            int style = arrow.getData(RpgData.SKILL_SHOT_STYLE);
            level.sendParticles(style == ARCANE_STYLE ? BLUE : YELLOW, hit.x, hit.y, hit.z,
                    style == MULTISHOT_STYLE ? 2 : 4, 0.06, 0.06, 0.06, 0);
            if (style != MULTISHOT_STYLE) {
                level.sendParticles(style == AIMED_STYLE ? ParticleTypes.CRIT : ParticleTypes.END_ROD,
                        hit.x, hit.y, hit.z, style == PIERCING_STYLE ? 1 : 3, 0.02, 0.02, 0.02, 0.01);
            }
            if (event.getRayTraceResult().getType() == HitResult.Type.BLOCK || nativeArrow.getPierceLevel() == 0) {
                arrow.removeData(RpgData.SKILL_SHOT);
                arrow.removeData(RpgData.SKILL_SHOT_STYLE);
            }
        }
    }

    private static void trail(Entity projectile, DustParticleOptions particle, int count) {
        // Fill the last tick's path, bounded even for externally accelerated projectiles.
        double dx = projectile.getX() - projectile.xo;
        double dy = projectile.getY() - projectile.yo;
        double dz = projectile.getZ() - projectile.zo;
        if (dx * dx + dy * dy + dz * dz > 36) { return; }
        for (int i = 0; i < count; i++) {
            double fraction = (double) i / count;
            projectile.level().addParticle(particle, projectile.getX() - dx * fraction,
                    projectile.getY() - dy * fraction, projectile.getZ() - dz * fraction, 0, 0, 0);
        }
    }
}
