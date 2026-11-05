package dev.covenant.rpg.skills;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A single-hit magical bolt using vanilla projectile collision, item rendering, and damage attribution. */
public final class ArcaneBolt extends ThrowableItemProjectile {
    public ArcaneBolt(EntityType<? extends ArcaneBolt> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override protected Item getDefaultItem() { return Items.AMETHYST_SHARD; }

    @Override public void tick() {
        super.tick();
        if (isRemoved()) { return; }
        if (level().isClientSide()) { SkillVisuals.boltTrail(this); }
        if (!level().isClientSide() && tickCount >= 100) { discard(); }
    }

    @Override protected void onHitEntity(EntityHitResult hit) {
        if (level() instanceof ServerLevel level && getOwner() instanceof LivingEntity owner
                && hit.getEntity() instanceof LivingEntity target && Skills.canHit(owner, target)) {
            target.hurtServer(level, damageSources().indirectMagic(this, owner), (float) SkillDefinition.ARCANE_BOLT.power());
        }
    }

    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel level) {
            SkillVisuals.boltImpact(level, hit.getLocation());
            discard();
        }
    }
}
