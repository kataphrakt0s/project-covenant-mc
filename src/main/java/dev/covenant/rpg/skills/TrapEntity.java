package dev.covenant.rpg.skills;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Nonpersistent, terrain-colliding deployment and its optional patch. Never collides with creatures in flight. */
public final class TrapEntity extends Entity implements ItemSupplier {
    public enum Phase { FLIGHT, ARMING, ARMED, TRIGGERED, REMOVED }
    private static final Phase[] PHASES = Phase.values();
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(TrapEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(TrapEntity.class, EntityDataSerializers.INT);
    /** Horizontal half-width in blocks of the shared square hitbox-contact trigger. */
    public static final double CONTACT_RADIUS = 0.75;
    /** Contact region height in blocks above the supporting collision surface. */
    public static final double CONTACT_HEIGHT = 0.25;
    public static final int ARM_TICKS = 10;
    public static final int EXPIRE_TICKS = 600;
    private ServerPlayer owner;
    private int duration;
    private int phaseTicks;
    private int flightTicks;
    private ItemStack visual;
    private Set<LivingEntity> frosted = new HashSet<>();
    private Set<LivingEntity> nextFrosted = new HashSet<>();
    private final Map<UUID, Long> nextFlameHit = new HashMap<>();

    public TrapEntity(EntityType<? extends TrapEntity> type, Level level) { super(type, level); }

    void initialize(ServerPlayer owner, TrapKind kind, int duration) {
        this.owner = owner;
        this.duration = duration;
        entityData.set(KIND, kind.ordinal());
    }

    public TrapKind kind() { return TrapKind.byOrdinal(entityData.get(KIND)); }
    public Phase phase() { return PHASES[entityData.get(PHASE)]; }
    public int effectDuration() { return duration; }

    private void phase(Phase next) { entityData.set(PHASE, next.ordinal()); phaseTicks = 0; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, 0); builder.define(PHASE, 0);
    }

    @Override public ItemStack getItem() {
        if (visual == null || !visual.is(kind().visual)) { visual = new ItemStack(kind().visual); }
        return visual;
    }

    // EntityType.noSave prevents serialization; empty hooks intentionally cannot restore a deployment.
    @Override protected void readAdditionalSaveData(ValueInput input) { discard(); }
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }

    @Override public void tick() {
        try (var zone = Profiler.get().zone("covenant:trap")) {
            super.tick();
            if (level().isClientSide()) { SkillVisuals.trap(this); return; }
            if (!validOwner() || getY() < level().getMinY() - 4) { discard(); return; }
            if (wet()) { discard(); return; }
            if (phase() == Phase.FLIGHT) { fly(); return; }
            if (!supported()) { discard(); return; }
            if (phase() == Phase.ARMING) {
                if (++phaseTicks < ARM_TICKS) { return; }
                phase(Phase.ARMED);
            } else if (phase() == Phase.ARMED && ++phaseTicks >= EXPIRE_TICKS) {
                discard(); return;
            } else if (phase() == Phase.TRIGGERED) {
                if (++phaseTicks >= duration) { discard(); return; }
                patch(); return;
            }
            if (phase() == Phase.ARMED) {
                var contacts = level().getEntitiesOfClass(LivingEntity.class, contactBox(), target ->
                        Skills.canHit(owner, target) && uncovered(target));
                // Deterministic single trigger when several creatures contact on the arming tick.
                var target = contacts.stream().min(java.util.Comparator.comparingInt(Entity::getId)).orElse(null);
                if (target != null) { trigger(target); }
            }
        }
    }

    private boolean validOwner() {
        return owner != null && !owner.isRemoved() && owner.isAlive() && !owner.hasDisconnected()
                && owner.level() == level() && PlayerProfiles.getSelectedClass(owner).orElse(null) == PlayerClass.STALKER
                && owner.getData(RpgData.SKILL_LOADOUT).learned().contains(kind().skillId);
    }

    private void fly() {
        var delta = getDeltaMovement();
        // Eight forward steps plus the release offset gives three blocks, even when falling over a ledge.
        if (++flightTicks > 8) { delta = new Vec3(0, delta.y, 0); }
        if (!loaded(getBoundingBox().expandTowards(delta).inflate(1))) { discard(); return; }
        move(MoverType.SELF, delta);
        setDeltaMovement(horizontalCollision ? new Vec3(0, delta.y - 0.06, 0) : delta.add(0, -0.06, 0));
        if (wet()) { discard(); return; }
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
            if (!supported()) { discard(); return; }
            phase(Phase.ARMING);
        }
        if (flightTicks >= 200) { discard(); }
    }

    private boolean wet() {
        if (!loaded(getBoundingBox())) { return true; }
        for (var pos : BlockPos.betweenClosed(BlockPos.containing(getBoundingBox().minX, getY(), getBoundingBox().minZ),
                BlockPos.containing(getBoundingBox().maxX, getBoundingBox().maxY, getBoundingBox().maxZ))) {
            if (!level().getFluidState(pos).isEmpty()) { return true; }
        }
        // Waterlogged half blocks can have their fluid in the supporting block below our feet.
        return phase() != Phase.FLIGHT && !level().getFluidState(BlockPos.containing(getX(), getY() - 0.01, getZ())).isEmpty();
    }

    private boolean supported() {
        var support = new AABB(getX() - 0.02, getY() - 0.035, getZ() - 0.02,
                getX() + 0.02, getY() + 0.001, getZ() + 0.02);
        if (!loaded(support)) { return false; }
        return level().getBlockCollisions(this, support).iterator().hasNext();
    }

    public AABB contactBox() {
        return new AABB(getX() - CONTACT_RADIUS, getY(), getZ() - CONTACT_RADIUS,
                getX() + CONTACT_RADIUS, getY() + CONTACT_HEIGHT, getZ() + CONTACT_RADIUS);
    }

    private boolean loaded(AABB box) {
        return level().hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ));
    }

    /** Ground areas use feet distance and a low ray; solid walls and intervening floors block them. */
    public boolean covers(LivingEntity target) {
        return phase() == Phase.TRIGGERED && !isRemoved() && target.level() == level()
                && Skills.canHit(owner, target) && target.position().distanceToSqr(position()) < kind().radius * kind().radius
                && uncovered(target);
    }
    void creditControl(LivingEntity target) {
        if (owner != null) { dev.covenant.rpg.progression.CombatProgression.supportEnemy(owner, target); }
    }

    private boolean uncovered(LivingEntity target) {
        var from = position().add(0, 0.1, 0);
        var to = target.position().add(0, 0.1, 0);
        return clearLine(from, to);
    }

    boolean clearLine(Vec3 from, Vec3 to) {
        return loaded(new AABB(from, to)) && level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private void trigger(LivingEntity target) {
        phase(Phase.TRIGGERED);
        Traps.triggered(owner, this);
        switch (kind()) {
            case BEAR -> {
                var before = AuraDamage.before(target);
                hit(target, kind().damage);
                if (before.landed(target)) { Rooted.apply(target, duration); }
                SkillVisuals.trapSnap((ServerLevel) level(), this);
                discard();
            }
            case FROST, FLAME -> {
                if (duration == 0) { discard(); } else { patch(); }
            }
            case BLAST -> { blast(); discard(); }
        }
    }

    private void patch() {
        var targets = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(kind().radius), this::covers);
        if (kind() == TrapKind.FROST) {
            // Reuse membership sets: each existing/new recipient is visited once, without list scans.
            nextFrosted.addAll(targets);
            for (var target : frosted) {
                if (!nextFrosted.contains(target)) { FrostSlows.remove(target, this); }
            }
            // Re-add cleansed memberships even when the recipient never left this patch.
            for (var target : nextFrosted) { FrostSlows.add(target, this); }
            var old = frosted; frosted = nextFrosted; nextFrosted = old; nextFrosted.clear();
        } else {
            long now = level().getGameTime();
            for (var target : targets) {
                if (now >= nextFlameHit.getOrDefault(target.getUUID(), Long.MIN_VALUE)) {
                    nextFlameHit.put(target.getUUID(), now + 20);
                    hit(target, kind().damage);
                }
            }
        }
    }

    private void hit(LivingEntity target, float damage) {
        target.hurtServer((ServerLevel) level(), damageSource(), (float) (damage * AuraDamage.multiplier(owner, kind() == TrapKind.FLAME)));
    }

    private DamageSource damageSource() {
        var server = (ServerLevel) level();
        var type = server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(kind().damageType);
        // Only Bear has a direct strike eligible for Dodge; all types retain their causing owner.
        return new DamageSource(type, kind() == TrapKind.BEAR ? this : null, owner);
    }

    private void blast() {
        var source = damageSource();
        for (var target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(kind().radius), this::covers)) {
            double falloff = 1 - target.position().distanceTo(position()) / kind().radius;
            target.hurtServer((ServerLevel) level(), source, (float) (kind().damage * falloff * AuraDamage.multiplier(owner, false)));
            if (target.isInvulnerableTo((ServerLevel) level(), source) || !target.isAlive()) { continue; }
            double resistance = target.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
            double ordinary = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            double strength = falloff * (1 - resistance) * (1 - ordinary);
            var direction = target.position().subtract(position()).multiply(1, 0, 1);
            if (direction.lengthSqr() < 0.0001) { direction = new Vec3(0, 0, 1); }
            if (strength > 0 && target instanceof ServerPlayer player) { Combos.interruptMovement(player, false); }
            target.push(direction.normalize().scale(0.65 * strength).add(0, 0.22 * strength, 0));
            target.hurtMarked = true;
        }
        SkillVisuals.trapSnap((ServerLevel) level(), this);
    }

    @Override public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        if (!level().isClientSide()) {
            for (var target : frosted) { FrostSlows.remove(target, this); }
            frosted.clear(); nextFrosted.clear(); nextFlameHit.clear();
            if (owner != null) {
                var runtime = owner.getData(RpgData.SKILL_RUNTIME);
                runtime.traps.remove(kind(), this); runtime.patches.remove(kind(), this);
            }
        }
        phase(Phase.REMOVED);
    }
}
