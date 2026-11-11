package dev.covenant.rpg.skills;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.Level;

/** Native companion behavior with shared body clips and an independent decorative core controller. */
public final class ElementalGolem extends TamableAnimal implements GeoEntity {
    /** Owner benefit and visual selection. Combat and navigation stay identical across forms. */
    public enum Form {
        ARCANE("arcane"), FIRE("fire"), ICE("ice"), LIGHTNING("lightning");
        private final String id;
        private final net.minecraft.resources.Identifier benefitId;
        Form(String id) { this.id = id; this.benefitId=net.minecraft.resources.Identifier.fromNamespaceAndPath("covenant","elemental_golem_"+id); }
        public String id() { return id; }
        public net.minecraft.resources.Identifier benefitId() { return benefitId; }
        public Form next() { return FORMS[(ordinal()+1)%FORMS.length]; }
    }
    private static final Form[] FORMS = Form.values();
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(ElementalGolem.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.elemental_golem.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.elemental_golem.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.elemental_golem.attack");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.elemental_golem.hurt");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.elemental_golem.death");
    private static final RawAnimation CORE = RawAnimation.begin().thenLoop("animation.elemental_golem.core_spin");
    private int attackInterval = 40;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ServerPlayer companionOwner;
    private long nextAttack;

    public ElementalGolem(EntityType<? extends TamableAnimal> type, Level level) { super(type, level); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data); data.define(FORM, Form.ARCANE.ordinal());
    }
    public Form form() { return FORMS[Math.clamp(entityData.get(FORM), 0, FORMS.length - 1)]; }
    /** Server-owned and tracking-synced; native owner snapshots retain this form. */
    public void setForm(Form form) {
        if (level().isClientSide()) { throw new IllegalStateException("Golem appearance is selected on the server"); }
        entityData.set(FORM, form.ordinal());
        if (serverOwner()!=null && ElementalGolems.current(serverOwner())==this) { ElementalGolems.refresh(serverOwner()); }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.MOVEMENT_SPEED, .25).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.STEP_HEIGHT, 1);
    }

    @Override public void setOwner(LivingEntity owner) {
        super.setOwner(owner);
        companionOwner = owner instanceof ServerPlayer player ? player : null;
    }
    ServerPlayer serverOwner() { return companionOwner; }
    void initialize(ServerPlayer owner, ElementalGolemData.Balance balance) {
        setTame(true,false); setOwner(owner);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(balance.health());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(balance.damage());
        setHealth(getMaxHealth()); attackInterval=balance.attackInterval();
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(5, new MeleeAttackGoal(this, 1, true) {
            @Override protected boolean isTimeToAttack() { return tickCount >= nextAttack && SkillControls.canAct(ElementalGolem.this); }
            @Override protected void resetAttackCooldown() { nextAttack = (long) tickCount + attackInterval; }
        });
        goalSelector.addGoal(6, new FollowOwnerGoal(this, 1, 10, 2));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        // Shared landed-damage events select targets; no independent hunting or wolf leap/panic.
    }

    @Override public boolean canAttack(LivingEntity target) {
        return Companions.canAttack(this, target) && serverOwner()!=null && ElementalGolems.assigned(serverOwner());
    }
    @Override public boolean wantsToAttack(LivingEntity target, LivingEntity owner) { return canAttack(target); }
    @Override public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity living) || !canAttack(living) || !SkillControls.canAct(this)) { return false; }
        triggerAnim("main", "attack");
        return super.doHurtTarget(level, target);
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isAlive()) { triggerAnim("main", "hurt"); }
        return hurt;
    }
    @Override public void tryToTeleportToOwner() { Companions.teleportToOwner(this); }

    @Override public void tick() {
        if (!level().isClientSide() && isAlive()) {
            var owner = serverOwner();
            if (owner == null || ElementalGolems.current(owner) != this) { discard(); return; }
            if (owner.hasDisconnected()) { ElementalGolems.store(owner); return; }
            if (!ElementalGolems.assigned(owner)) { ElementalGolems.dismiss(owner); return; }
            if (owner.level()!=level()) { ElementalGolems.store(owner); return; }
            if (getTarget() != null && (!canAttack(getTarget()) || distanceToSqr(owner) > 1024)) { setTarget(null); }
        }
        super.tick();
        if (level().isClientSide()) { SkillVisuals.golemAmbient(this); }
    }
    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide()) { ElementalGolems.removed(this, reason); }
        super.remove(reason);
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if (!isAlive() || !stack.is(Items.IRON_INGOT) || getHealth()>=getMaxHealth()) { return InteractionResult.PASS; }
        if (level().isClientSide()) { return InteractionResult.SUCCESS; }
        float before=getHealth();
        heal(25);
        if (getHealth()<=before) { return InteractionResult.PASS; }
        playSound(SoundEvents.IRON_GOLEM_REPAIR,1,1+(random.nextFloat()-random.nextFloat())*.2f);
        stack.consume(1,player);
        return InteractionResult.SUCCESS;
    }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("CovenantForm",form().id());
        output.putInt("CovenantAttackInterval",attackInterval);
        output.putLong("CovenantAttackRemaining",Math.max(0,nextAttack-tickCount));
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String selected=input.getStringOr("CovenantForm","arcane");
        for (var form:FORMS) { if (form.id().equals(selected)) { setForm(form); break; } }
        attackInterval=Math.clamp(input.getIntOr("CovenantAttackInterval",40),1,72000);
        nextAttack=(long)tickCount+Math.clamp(input.getLongOr("CovenantAttackRemaining",0),0,72000);
        setOrderedToSit(false); setInSittingPose(false);
    }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }
    @Override public boolean canMate(Animal partner) { return false; }
    @Override public boolean canFallInLove() { return false; }
    @Override public boolean canBeLeashed() { return false; }
    @Override public void setOrderedToSit(boolean sitting) { super.setOrderedToSit(false); }
    @Override public boolean canUseSlot(net.minecraft.world.entity.EquipmentSlot slot) { return false; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ElementalGolem>("main", 2, state -> {
            var golem = state.animatable();
            if (golem.isDeadOrDying()) { return state.setAndContinue(DEATH); }
            if (golem.hurtTime > 0) { return state.setAndContinue(HURT); }
            var controller = state.controller();
            if (controller.isTriggeredAnimation("attack") || controller.isTriggeredAnimation("hurt")) {
                var point = controller.getCurrentAnimationPoint();
                if (point == null || !point.hasFinished()) { return PlayState.CONTINUE; }
                controller.stopTriggeredAnimation();
            }
            double travel = golem.getKnownSpeed().horizontalDistanceSqr();
            boolean moved = !golem.isPassenger() && travel >= .000025 && travel <= 1;
            return state.setAndContinue(state.isMoving() || moved ? WALK : IDLE);
        }).receiveTriggeredAnimations().triggerableAnim("attack", ATTACK).triggerableAnim("hurt", HURT));
        // Stable per-entity variation avoids lockstep cores without per-tick randomness or gameplay state.
        double coreSpeed = .85 + Integer.toUnsignedLong(getUUID().hashCode()) % 31 / 100.0;
        controllers.add(new AnimationController<ElementalGolem>("core", 0, state -> {
            state.controller().setAnimationSpeed(state.animatable().isDeadOrDying() ? 0 : coreSpeed);
            return state.setAndContinue(CORE);
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
