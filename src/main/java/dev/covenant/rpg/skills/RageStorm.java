package dev.covenant.rpg.skills;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        if (validOwner() && loaded(getBoundingBox().inflate(config.radius()))) { pulse(); }
        else { discard(); }
    }
    public static void clear(ServerPlayer owner) {
        for (var storm : owner.getData(RpgData.SKILL_RUNTIME).storms.toArray(RageStorm[]::new)) { storm.discard(); }
    }
    public int attempts(LivingEntity target) { return attempts.getOrDefault(target.getUUID(), 0); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
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
        super.tick();
        if (!(level() instanceof ServerLevel level)) { return; }
        if (!released || !validOwner()) { discard(); return; }
        double step = Math.min(config.speed(), config.distance() - travelled);
        var next = position().add(direction.scale(step));
        if (!loaded(new AABB(position(), next).inflate(config.radius()))) { discard(); return; }
        var hit = level.clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) { discard(); return; }
        setPos(next);
        travelled += step;
        if (++age % config.interval() == 0) { pulse(); }
        if (age % 2 == 0) {
            // Bounded visible volume with no custom client simulation or gameplay packets.
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY(), getZ(), 10,
                    config.radius() * .55, config.radius() * .55, config.radius() * .55, .01);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY(), getZ(), 1, .5, .5, .5, 0);
        }
        if (travelled >= config.distance() - 1e-6) { discard(); }
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
            target.hurtServer(level, source, damage);
        }
    }
}
