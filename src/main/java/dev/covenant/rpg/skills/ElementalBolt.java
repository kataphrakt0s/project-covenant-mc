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

/** Transient single-collision projectile; its registered type identifies its element on both sides. */
public final class ElementalBolt extends ThrowableItemProjectile {
    private int duration;

    public ElementalBolt(EntityType<? extends ElementalBolt> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public SkillDefinition skill() { return getType() == Skills.FIRE_BOLT.get() ? SkillDefinition.FIRE_BOLT : SkillDefinition.ICE_BOLT; }
    void initialize(int duration) {
        this.duration = duration;
        if (getOwner() instanceof LivingEntity owner) { setData(dev.covenant.rpg.RpgData.AURA_PROJECTILE_POWER, AuraDamage.elementalMultiplier(owner, WeaverPower.element(skill()))); }
    }
    @Override protected Item getDefaultItem() { return getType() == Skills.FIRE_BOLT.get() ? Items.FIRE_CHARGE : Items.SNOWBALL; }

    @Override public void tick() {
        super.tick();
        if (isRemoved()) { return; }
        if (level().isClientSide()) { SkillVisuals.elementalTrail(this); }
        else if (tickCount >= 100) { discard(); }
    }

    @Override protected void onHitEntity(EntityHitResult hit) {
        if (level() instanceof ServerLevel level && getOwner() instanceof LivingEntity owner
                && hit.getEntity() instanceof LivingEntity target) {
            ElementalBolts.hit(level, owner, this, target, skill(), duration);
        }
    }

    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel level) {
            SkillVisuals.elementalImpact(level, hit.getLocation(), skill());
            discard();
        }
    }
}
