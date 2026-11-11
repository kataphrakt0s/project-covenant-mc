package dev.covenant.rpg.skills;

import dev.covenant.rpg.stats.RpgStats;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;

/** Shared elemental impact boundary; ownership and defenses stay in the native damage pipeline. */
public final class ElementalBolts {
    /** Fire Bolt's native burning duration: four seconds before burning-time modifiers. */
    public static final int BURN_TICKS = 80;
    private ElementalBolts() {}

    public static void hit(ServerLevel level, LivingEntity owner, Entity direct, LivingEntity target,
            SkillDefinition skill, int duration) {
        if (!Skills.canHit(owner, target)) { return; }
        var type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, skill.id()));
        var before = AuraDamage.before(target);
        double captured = direct != owner && direct.hasData(dev.covenant.rpg.RpgData.AURA_PROJECTILE_POWER)
                ? direct.getData(dev.covenant.rpg.RpgData.AURA_PROJECTILE_POWER) : AuraDamage.elementalMultiplier(owner, WeaverPower.element(skill));
        target.hurtServer(level, new DamageSource(type, direct, owner), (float) (skill.power() * captured));
        if (!target.isAlive() || !before.landed(target)) { return; }
        if (skill == SkillDefinition.FIRE_BOLT && !target.fireImmune()) {
            target.igniteForTicks(duration);
        } else if (skill == SkillDefinition.ICE_BOLT) {
            Chilled.apply(target, owner, duration);
        }
    }

    static int duration(ServerPlayer player, SkillDefinition skill) {
        return (int) Math.clamp(Math.ceil(RpgStats.scaleSkillValue(player, RpgStats.SKILL_DURATION,
                skill == SkillDefinition.FIRE_BOLT ? BURN_TICKS : Chilled.DURATION_TICKS)), 1, Integer.MAX_VALUE);
    }

    private static void chain(ServerPlayer player, LivingEntity first) {
        var hit = new java.util.HashSet<LivingEntity>(); hit.add(first);
        var previous = first;
        var type = player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, SkillDefinition.LIGHTNING_BOLT.id()));
        for (int jump = 0; jump < 2; jump++) {
            final var from = previous;
            var next = player.level().getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(4),
                    candidate -> !hit.contains(candidate) && Skills.canHit(player, candidate) && from.distanceToSqr(candidate) <= 16
                            && player.level().clip(new ClipContext(from.getEyePosition(), candidate.getEyePosition(),
                                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from)).getType() == net.minecraft.world.phys.HitResult.Type.MISS)
                    .stream().min(java.util.Comparator.comparingDouble((LivingEntity candidate) -> from.distanceToSqr(candidate))
                            .thenComparing(LivingEntity::getUUID)).orElse(null);
            if (next == null) { break; }
            SkillVisuals.lightning(player.level(), from.getEyePosition(), next.getEyePosition());
            SkillVisuals.lightningImpact(player.level(), next.getEyePosition());
            next.hurtServer(player.level(), new DamageSource(type, player, player), (float) (SkillDefinition.LIGHTNING_BOLT.power() * .5 * AuraDamage.elementalMultiplier(player, WeaverPower.Element.LIGHTNING)));
            hit.add(next); previous = next;
        }
    }

    static void lightning(ServerPlayer player) {
        var skill = SkillDefinition.LIGHTNING_BOLT;
        var start = player.getEyePosition();
        var delta = player.getLookAngle().scale(skill.range());
        // The native view-vector convenience query uses previous-tick rotation; resolve current server aim instead.
        var block = player.level().clipIncludingBorder(new ClipContext(start, start.add(delta),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        var entityHit = ProjectileUtil.getEntityHitResult(player.level(), player, start, block.getLocation(),
                player.getBoundingBox().expandTowards(delta).inflate(1),
                entity -> entity instanceof LivingEntity target && entity.isPickable() && Skills.canHit(player, target), 0);
        SkillVisuals.lightning(player.level(), start, entityHit == null ? block.getLocation() : entityHit.getLocation());
        if (entityHit != null || block.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            SkillVisuals.lightningImpact(player.level(), entityHit == null ? block.getLocation() : entityHit.getLocation());
            SkillSounds.impact(player.level(), entityHit == null ? block.getLocation() : entityHit.getLocation(), skill);
        }
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
            hit(player.level(), player, player, target, skill, 0);
            if (Auras.active(player, Auras.Kind.SHOCK)) { chain(player, target); }
        }
    }
}
