package dev.covenant.rpg.skills;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Short-lived native entity, fixed 3D release aim, local pulses and terrain clipping; never saved or chunk-forced. */
public final class RageStorm extends Entity {
    private static final EntityDataAccessor<Float> VISUAL_RADIUS = SynchedEntityData.defineId(RageStorm.class, EntityDataSerializers.FLOAT);
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath("covenant", "rage_storm"));
    private ServerPlayer owner;
    private ReaverSkillData.Storm config;
    private Vec3 direction = Vec3.ZERO;
    private float damage;
    private double travelled;
    private int age;
    private boolean released;
    private final Map<UUID, Integer> attempts = new HashMap<>();

    public RageStorm(EntityType<? extends RageStorm> type, Level level) { super(type, level); }
    public ServerPlayer getOwner() { return owner; }
    /** Release-time radius in blocks, zero until the paid release; native tracking supplies it to observers. */
    public float visualRadius() { return entityData.get(VISUAL_RADIUS); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        // The gameplay entity is a tiny center marker; distance culling must use its visible volume.
        double range = Math.max(1, visualRadius() * 2) * 64 * getViewScale();
        return visualRadius() > 0 && distance < range * range;
    }

    /** Prepare before spending; an entity-join rejection never spends resource or commits a cooldown. */
    static RageStorm prepare(ServerPlayer owner) {
        var storm = new RageStorm(Skills.RAGE_STORM.get(), owner.level());
        storm.owner = owner;
        storm.config = ReaverSkillData.get(owner.level().getServer()).storm();
        storm.direction = owner.getLookAngle().normalize();
        storm.damage = (float) (storm.config.damage() * AuraDamage.multiplier(owner, false));
        storm.setPos(owner.getEyePosition().add(0, -.1, 0));
        return owner.level().addFreshEntity(storm) ? storm : null;
    }

    void release() {
        released = true;
        owner.getData(RpgData.SKILL_RUNTIME).storms.add(this);
        if (validOwner() && loaded(getBoundingBox().inflate(config.radius()))) {
            entityData.set(VISUAL_RADIUS, (float) config.radius());
            pulse();
        }
        else { discard(); }
    }
    public static void clear(ServerPlayer owner) {
        for (var storm : owner.getData(RpgData.SKILL_RUNTIME).storms.toArray(RageStorm[]::new)) { storm.discard(); }
    }
    public int attempts(LivingEntity target) { return attempts.getOrDefault(target.getUUID(), 0); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(VISUAL_RADIUS, 0f); }
    @Override protected void readAdditionalSaveData(ValueInput input) { discard(); }
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public void remove(RemovalReason reason) {
        super.remove(reason);
        if (owner != null) { owner.getData(RpgData.SKILL_RUNTIME).storms.remove(this); }
    }
    private boolean validOwner() {
        return owner != null && owner.isAlive() && !owner.isRemoved() && !owner.hasDisconnected() && !owner.isSpectator()
                && owner.level() == level() && PlayerProfiles.getSelectedClass(owner).orElse(null) == PlayerClass.REAVER
                && owner.getData(RpgData.SKILL_LOADOUT).learned().contains(SkillDefinition.RAGE_STORM.id());
    }
    @Override public void tick() {
        try (var zone = Profiler.get().zone("covenant:rage_storm")) {
            super.tick();
            if (level().isClientSide()) {
                // Entity-owned decoration stops on native removal. No server particle stream or world scan.
                if (visualRadius() > 0 && tickCount % 2 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double angle = tickCount * .35 + i * Math.PI / 3;
                        double height = (i % 3 - 1) * visualRadius() * .55;
                        double ring = Math.sqrt(visualRadius() * visualRadius() - height * height) * .9;
                        level().addParticle(CombatParticles.COMBO_CRIMSON.get(), getX() + Math.cos(angle) * ring,
                                getY() + height, getZ() + Math.sin(angle) * ring, 0, 0, 0);
                    }
                }
                return;
            }
            if (!(level() instanceof ServerLevel level)) { return; }
            if (!released || !validOwner()) { discard(); return; }
            double step = Math.min(config.speed(), config.distance() - travelled);
            var next = position().add(direction.scale(step));
            if (!loaded(new AABB(position(), next).inflate(config.radius()))) { discard(); return; }
            var hit = level.clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.MISS) { ReaverVisuals.stormEnd(this); discard(); return; }
            setPos(next);
            travelled += step;
            if (++age % config.interval() == 0) { pulse(); }
            if (travelled >= config.distance() - 1e-6 && !isRemoved()) { ReaverVisuals.stormEnd(this); discard(); }
        }
    }
    private boolean loaded(AABB box) {
        return level().hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ));
    }
    private void pulse() {
        var level = (ServerLevel) level();
        var type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE_TYPE);
        var source = new DamageSource(type, this, owner);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(config.radius()), target ->
                Skills.canHit(owner, target) && target.getBoundingBox().getCenter().distanceToSqr(position()) <= config.radius() * config.radius())) {
            if (isRemoved() || !validOwner()) { break; }
            if (attempts(target) >= config.hits() || level.clip(new ClipContext(position(), target.getBoundingBox().getCenter(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) { continue; }
            attempts.merge(target.getUUID(), 1, Integer::sum);
            var before = AuraDamage.before(target);
            target.hurtServer(level, source, damage);
            if (before.landed(target)) { ComboVisuals.contact(owner, target); }
        }
    }
}
