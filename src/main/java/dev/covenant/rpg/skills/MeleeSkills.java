package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.stats.RpgStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/** Native-reach targeting and melee effects shared by instant skills and combo stages. */
final class MeleeSkills {
    private MeleeSkills() {}

    static void cleave(ServerPlayer player, SkillDefinition skill) {
        SkillVisuals.cleave(player, skill.range());
        float damage = (float) (player.getAttributeValue(RpgStats.DAMAGE) * skill.power() * AuraDamage.multiplier(player, false));
        boolean landed = false;
        for (var target : targets(player, skill)) {
            if (!player.isAlive()) { break; }
            var before = AuraDamage.before(target);
            hurtMeleeSkill(player, target, damage);
            if (before.landed(target)) {
                SkillVisuals.cleaveHit(player.level(), target);
                landed = true;
            }
        }
        if (landed) { SkillSounds.meleeHit(player); }
    }

    static void rend(ServerPlayer player, LivingEntity target) {
        if (target != null) {
            if (applyRend(player, target, (float) (player.getAttributeValue(RpgStats.DAMAGE) * AuraDamage.multiplier(player, false)),
                    AuraDamage.bleedPower(player), Combos.duration(player, Bleeding.REND_DURATION_TICKS))) {
                SkillSounds.meleeHit(player);
            }
        }
    }

    /** Uses the same held-item attack range and block-clipped crosshair pick as native melee. */
    static LivingEntity rendTarget(ServerPlayer player) {
        var range = player.getAttackRangeWith(player.getMainHandItem());
        var hit = range.getClosesetHit(player, 1, entity -> entity.isPickable() && !entity.isSpectator());
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target
                && Skills.canHit(player, target) && player.hasLineOfSight(target) ? target : null;
    }

    static boolean hurtMeleeSkill(ServerPlayer player, LivingEntity target, float damage) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        var previous = runtime.skillDamageSource;
        var source = player.damageSources().playerAttack(player);
        runtime.skillDamageSource = source;
        try { return target.hurtServer(player.level(), source, damage); }
        finally { runtime.skillDamageSource = previous; }
    }

    static boolean applyRend(ServerPlayer player, LivingEntity target, float damage, float bleed, int duration) {
        var before = AuraDamage.before(target);
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        boolean previous = runtime.applyingRend;
        runtime.applyingRend = true;
        try { hurtMeleeSkill(player, target, damage); }
        finally { runtime.applyingRend = previous; }
        if (before.landed(target)) {
            if (bleed > 0) { Bleeding.apply(target, player, bleed, duration); }
            SkillVisuals.cleaveHit(player.level(), target);
            return true;
        }
        return false;
    }

    static java.util.List<LivingEntity> targets(ServerPlayer player, SkillDefinition skill) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(skill.range()),
                target -> Skills.canHit(player, target) && player.distanceToSqr(target) <= skill.range() * skill.range()
                        && inCleaveArc(player, target)
                        && player.hasLineOfSight(target));
    }

    private static boolean inCleaveArc(ServerPlayer player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double yaw = Math.toRadians(player.getYRot());
        return distance < 0.00001 || (-Math.sin(yaw) * dx + Math.cos(yaw) * dz)
                >= distance * Math.cos(Math.toRadians(Skills.CLEAVE_HALF_ANGLE_DEGREES));
    }

}
