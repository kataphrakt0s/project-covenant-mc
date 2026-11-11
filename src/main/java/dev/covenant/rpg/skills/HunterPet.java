package dev.covenant.rpg.skills;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Native wolf navigation, physiology and melee, with owner-directed skill targeting and GeckoLib visuals. */
public final class HunterPet extends Wolf implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.original_wolf.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.original_wolf.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.original_wolf.attack");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.original_wolf.hurt");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.original_wolf.death");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int attackInterval = 30;
    private long nextAttack;
    private ServerPlayer companionOwner;

    public HunterPet(EntityType<? extends Wolf> type, Level level) { super(type, level); }
    /** Stable server reference also covers the instant when native dimension transfer changes owner lookup. */
    ServerPlayer serverOwner() { return companionOwner; }
    @Override public void setOwner(LivingEntity owner) {
        super.setOwner(owner);
        companionOwner = owner instanceof ServerPlayer player ? player : null;
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ATTACK_DAMAGE, 3);
    }
    void initialize(ServerPlayer owner, HunterPetData.Balance balance) {
        setTame(true, false); setOwner(owner);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(balance.health());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(balance.damage());
        setHealth(getMaxHealth()); attackInterval = balance.attackInterval();
    }
    @Override protected void applyTamingSideEffects() { /* Skill attributes survive native load without a full heal. */ }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(1, new TamableAnimalPanicGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        goalSelector.addGoal(4, new LeapAtTargetGoal(this, .4f));
        goalSelector.addGoal(5, new MeleeAttackGoal(this, 1, true) {
            @Override protected boolean isTimeToAttack() { return tickCount >= nextAttack && SkillControls.canAct(HunterPet.this); }
            @Override protected void resetAttackCooldown() { nextAttack = (long) tickCount + attackInterval; }
        });
        goalSelector.addGoal(6, new FollowOwnerGoal(this, 1, 10, 2));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        // Landed-damage events select targets, including projectiles/spells; no wild hunting or pack alerts.
    }
    @Override public boolean canAttack(LivingEntity target) {
        return target != null && target.level() == level() && target.canBeSeenAsEnemy()
                && (!(target instanceof Player) || level().getDifficulty() != Difficulty.PEACEFUL)
                && getOwner() instanceof ServerPlayer owner && HunterPets.assigned(owner) && Skills.canHit(owner, target);
    }
    @Override public boolean wantsToAttack(LivingEntity target, LivingEntity owner) { return canAttack(target); }
    @Override public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity living) || !canAttack(living) || !SkillControls.canAct(this)) { return false; }
        triggerAnim("main", "attack");
        return super.doHurtTarget(level, target);
    }
    @Override public void tick() {
        if (!level().isClientSide() && isAlive()) {
            try (var zone = Profiler.get().zone("covenant:pet_tick")) {
                var owner = serverOwner();
                if (owner == null || HunterPets.current(owner) != this) { discard(); return; }
                if (owner.hasDisconnected()) { HunterPets.store(owner); return; }
                if (!HunterPets.assigned(owner)) { HunterPets.dismiss(owner); return; }
                if (owner.level() != level()) { HunterPets.store(owner); return; }
                if (getTarget() != null && (!canAttack(getTarget()) || distanceToSqr(owner) > 1024)) { setTarget(null); }
                super.tick();
            }
        } else { super.tick(); }
    }
    @Override public void tryToTeleportToOwner() { HunterPets.teleportToOwner(this); }
    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide()) { HunterPets.removed(this, reason); }
        super.remove(reason);
    }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var food = player.getItemInHand(hand);
        if (isFood(food) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide()) { feed(player, hand, food, 2, 2); }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override public boolean canMate(Animal partner) { return false; }
    @Override public boolean canFallInLove() { return false; }
    @Override public boolean canBeLeashed() { return false; }
    @Override public boolean canUseSlot(net.minecraft.world.entity.EquipmentSlot slot) { return false; }
    @Override public void setOrderedToSit(boolean sitting) { super.setOrderedToSit(false); }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isAlive()) { triggerAnim("main", "hurt"); }
        return hurt;
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("CovenantAttackInterval", attackInterval);
        output.putLong("CovenantAttackRemaining", Math.max(0, nextAttack - tickCount));
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        attackInterval = Math.clamp(input.getIntOr("CovenantAttackInterval", 30), 1, 72000);
        nextAttack = (long) tickCount + Math.clamp(input.getLongOr("CovenantAttackRemaining", 0), 0, 72000);
        setOrderedToSit(false); setInSittingPose(false);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<HunterPet>("main", 3, state -> {
            var pet = state.animatable();
            if (pet.isDeadOrDying()) { return state.setAndContinue(DEATH); }
            if (pet.hurtTime > 0) { return state.setAndContinue(HURT); }
            var controller = state.controller();
            if (controller.isTriggeredAnimation("attack") || controller.isTriggeredAnimation("hurt")) {
                // GeckoLib 5.5.2 retains completed triggers and otherwise skips this handler forever.
                // Keep pending/playing actions intact, then explicitly hand control back to locomotion.
                var point = controller.getCurrentAnimationPoint();
                if (point == null || !point.hasFinished()) { return PlayState.CONTINUE; }
                controller.stopTriggeredAnimation();
            }
            // Remote wolves can travel through interpolation with no native limb-speed flag.
            // Native cached tick displacement detects that travel without using AI intent or stale velocity.
            double travel = pet.getKnownSpeed().horizontalDistanceSqr();
            boolean moved = !pet.isPassenger() && travel >= .000025 && travel <= 1;
            return state.setAndContinue(state.isMoving() || moved ? WALK : IDLE);
        }).receiveTriggeredAnimations()
                .triggerableAnim("attack", ATTACK).triggerableAnim("hurt", HURT).triggerableAnim("death", DEATH));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
