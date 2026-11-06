package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    private SkillVisuals() {}

    static void elementalTrail(ElementalBolt bolt) {
        trail(bolt, bolt.skill() == SkillDefinition.FIRE_BOLT ? FIRE : BLUE, 3);
    }

    static void elementalImpact(ServerLevel level, Vec3 pos, SkillDefinition skill) {
        level.sendParticles(skill == SkillDefinition.FIRE_BOLT ? FIRE : BLUE,
                pos.x, pos.y, pos.z, 10, 0.12, 0.12, 0.12, 0);
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

    /** Tracked entity state drives local particles for every viewer, without recurring server packets. */
    static void trap(TrapEntity trap) {
        if (trap.tickCount % 5 != 0 || trap.phase() == TrapEntity.Phase.REMOVED) { return; }
        var dust = new DustParticleOptions(trap.kind().color,
                trap.phase() == TrapEntity.Phase.ARMED ? 1.0f : 0.6f);
        boolean patch = trap.phase() == TrapEntity.Phase.TRIGGERED;
        double radius = patch ? trap.kind().radius : TrapEntity.CONTACT_RADIUS;
        int count = patch ? 40 : 12;
        var center = trap.position().add(0, 0.1, 0);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            var point = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            if (!patch || trap.clearLine(center, point)) {
                trap.level().addParticle(dust, point.x, point.y, point.z, 0, 0, 0);
            }
        }
        if (trap.phase() == TrapEntity.Phase.ARMED) {
            trap.level().addParticle(ParticleTypes.END_ROD, trap.getX(), trap.getY() + 0.2, trap.getZ(), 0, 0.005, 0);
        }
    }

    static void trapSnap(ServerLevel level, TrapEntity trap) {
        level.sendParticles(trap.kind() == TrapKind.BLAST ? ParticleTypes.EXPLOSION : ParticleTypes.CRIT,
                trap.getX(), trap.getY() + 0.2, trap.getZ(), trap.kind() == TrapKind.BLAST ? 1 : 8,
                0.15, 0.1, 0.15, 0.01);
    }

    static void bowDraw(ServerPlayer player, long ticks) {
        player.setData(RpgData.BOW_DRAW_DURATION, ticks);
        player.setData(RpgData.BOW_DRAW_UNTIL, player.level().getGameTime() + ticks);
    }

    static void charge(ServerPlayer player) {
        if (player.tickCount % 2 != 0) { return; }
        var hand = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.25, 0);
        player.level().sendParticles(BLUE, hand.x, hand.y, hand.z, 3, 0.08, 0.08, 0.08, 0);
        player.level().sendParticles(ParticleTypes.END_ROD, hand.x, hand.y, hand.z, 1, 0.04, 0.04, 0.04, 0.005);
    }

    static void cleave(ServerPlayer player, double range) {
        var level = player.level();
        for (int i = 0; i < 24; i++) {
            double angle = Math.toRadians(player.getYRot() + 90 - Skills.CLEAVE_HALF_ANGLE_DEGREES
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
        level.sendParticles(BLUE, position.x, position.y, position.z, 10, 0.12, 0.12, 0.12, 0);
        level.sendParticles(ParticleTypes.END_ROD, position.x, position.y, position.z, 5, 0.05, 0.05, 0.05, 0.025);
    }

    /** Called only on clients. A tracked projectile supplies its own lifecycle; no recurring particle packets. */
    static void boltTrail(Entity bolt) {
        trail(bolt, BLUE, 3);
        if (bolt.tickCount % 2 == 0) {
            bolt.level().addParticle(ParticleTypes.END_ROD, bolt.getX(), bolt.getY(), bolt.getZ(), 0, 0, 0);
        }
    }

    /** Client entity callback checks only marked arrows, without scanning the world's entities. */
    public static void arrowTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.level().isClientSide()
                && !arrow.isRemoved() && arrow.tickCount <= 100
                && arrow.hasData(RpgData.SKILL_SHOT) && arrow.getData(RpgData.SKILL_SHOT)) {
            trail(arrow, YELLOW, 4);
        }
    }

    /** Run after other impact handlers so canceled collisions do not play an impact effect. */
    public static void arrowImpact(ProjectileImpactEvent event) {
        var arrow = event.getProjectile();
        if (!(arrow instanceof AbstractArrow nativeArrow) || !(arrow.level() instanceof ServerLevel level)
                || !arrow.hasData(RpgData.SKILL_SHOT) || !arrow.getData(RpgData.SKILL_SHOT)) { return; }
        var hit = event.getRayTraceResult().getLocation();
        level.sendParticles(YELLOW, hit.x, hit.y, hit.z, 5, 0.06, 0.06, 0.06, 0);
        level.sendParticles(ParticleTypes.END_ROD, hit.x, hit.y, hit.z, 2, 0.02, 0.02, 0.02, 0.01);
        if (event.getRayTraceResult().getType() == HitResult.Type.BLOCK || nativeArrow.getPierceLevel() == 0) {
            arrow.removeData(RpgData.SKILL_SHOT);
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
