package dev.covenant.rpg.skills;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.stats.RpgStats;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;

/** Unsaved, stationary, owner-local deployments. Shared placement/lifecycle, distinct damage/support behavior. */
public final class GroundSkillEntity extends Entity {
    public static final ResourceKey<DamageType> FIRE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, SkillDefinition.FIRESTORM.id());
    private static final EntityDataAccessor<Boolean> BANNER = SynchedEntityData.defineId(GroundSkillEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(GroundSkillEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<org.joml.Vector3fc> IMPACT_FROM = SynchedEntityData.defineId(GroundSkillEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<org.joml.Vector3fc> IMPACT_TO = SynchedEntityData.defineId(GroundSkillEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> IMPACT_SEQUENCE = SynchedEntityData.defineId(GroundSkillEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FALL_TICKS = SynchedEntityData.defineId(GroundSkillEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SPLASH = SynchedEntityData.defineId(GroundSkillEntity.class, EntityDataSerializers.FLOAT);
    private ServerPlayer owner;
    private boolean released;
    private long until;
    private double nextImpact;
    private CombatSkillData.Firestorm fire;
    private CombatSkillData.Banner banner;
    private int burnTicks;
    private float damage;
    private final Map<UUID, Long> hitAfter = new HashMap<>();
    private final List<Impact> impacts = new ArrayList<>();
    final Set<LivingEntity> recipients = new HashSet<>();
    private record Impact(Vec3 from, Vec3 to, long at) {}
    public GroundSkillEntity(EntityType<? extends GroundSkillEntity> type, Level level) { super(type, level); }
    public boolean banner() { return entityData.get(BANNER); }
    public double radius() { return entityData.get(RADIUS); }
    public int impactSequence() { return entityData.get(IMPACT_SEQUENCE); }
    public int fallTicks() { return entityData.get(FALL_TICKS); }
    public float splashRadius() { return entityData.get(SPLASH); }
    public Vec3 impactFrom() { return new Vec3(entityData.get(IMPACT_FROM)); }
    public Vec3 impactTo() { return new Vec3(entityData.get(IMPACT_TO)); }
    ServerPlayer owner() { return owner; }
    double healing() { return banner.healing(); }
    private SkillDefinition skill() { return banner() ? SkillDefinition.WAR_BANNER : SkillDefinition.FIRESTORM; }

    static GroundSkillEntity prepare(ServerPlayer owner, SkillDefinition skill) {
        var entity = new GroundSkillEntity(Skills.GROUND_SKILL.get(), owner.level());
        entity.owner = owner;
        entity.entityData.set(BANNER, skill.effect() == SkillDefinition.Effect.WAR_BANNER);
        var balance = CombatSkillData.get(owner.level().getServer());
        entity.fire = balance.firestorm(); entity.banner = balance.banner();
        entity.entityData.set(SPLASH,(float)entity.fire.splash());
        Vec3 point;
        if (entity.banner()) {
            if (!owner.onGround()) { return null; }
            point = owner.position();
        } else {
            var from = owner.getEyePosition(); var to = from.add(owner.getLookAngle().scale(skill.range()));
            if (!loaded(owner.level(), new AABB(from, to))) { return null; }
            var hit = owner.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) { return null; }
            point = hit.getLocation();
        }
        entity.setPos(point); entity.setYRot(owner.getYRot());
        entity.entityData.set(RADIUS, (float) (entity.banner() ? entity.banner.radius() : entity.fire.radius()));
        if (!entity.supported() || !owner.level().getWorldBorder().isWithinBounds(entity.getBoundingBox())
                || (entity.banner() && owner.level().getBlockCollisions(entity, entity.getBoundingBox()).iterator().hasNext())) { return null; }
        entity.burnTicks = duration(owner, entity.fire.burnTicks());
        entity.damage = (float) (entity.fire.damage() * AuraDamage.elementalMultiplier(owner, WeaverPower.Element.FIRE));
        entity.setData(RpgData.AURA_PROJECTILE_POWER, 1d); // Committed damage is already scaled once.
        ProjectileCombat.captureSupport(entity, owner);
        return owner.level().addFreshEntity(entity) ? entity : null;
    }
    static int duration(ServerPlayer owner, int ticks) {
        return (int) Math.clamp(Math.ceil(RpgStats.scaleSkillValue(owner, RpgStats.SKILL_DURATION, ticks)), 1, 72000);
    }
    void release() {
        var runtime = owner.getData(RpgData.SKILL_RUNTIME);
        var old = banner() ? runtime.banner : runtime.firestorm;
        if (banner()) { runtime.banner = this; } else { runtime.firestorm = this; }
        released = true;
        until = ClassResources.now(owner) + duration(owner, banner() ? banner.duration() : fire.duration());
        nextImpact = ClassResources.now(owner);
        if (old != null && old != this) { old.discard(); }
        if (banner()) { refreshBanner(); }
    }
    public static void clear(ServerPlayer player) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        if (runtime.firestorm != null) { runtime.firestorm.discard(); }
        if (runtime.banner != null) { runtime.banner.discard(); }
    }
    private boolean validOwner() {
        return owner != null && owner.isAlive() && !owner.isRemoved() && !owner.hasDisconnected() && !owner.isSpectator()
                && owner.level() == level() && PlayerProfiles.getSelectedClass(owner).orElse(null) == (banner() ? PlayerClass.REAVER : PlayerClass.WEAVER)
                && owner.getData(RpgData.SKILL_LOADOUT).learned().contains(skill().id());
    }
    public static boolean loaded(Level level, AABB box) {
        for (int x = net.minecraft.util.Mth.floor(box.minX) >> 4; x <= net.minecraft.util.Mth.floor(box.maxX) >> 4; x++) {
            for (int z = net.minecraft.util.Mth.floor(box.minZ) >> 4; z <= net.minecraft.util.Mth.floor(box.maxZ) >> 4; z++) {
                if (!level.hasChunk(x,z)) { return false; }
            }
        }
        return true;
    }
    private boolean supported() {
        var box = new AABB(getX()-.02,getY()-.04,getZ()-.02,getX()+.02,getY()+.001,getZ()+.02);
        return loaded(level(), box) && level().getBlockCollisions(this,box).iterator().hasNext();
    }
    @Override public void tick() {
        try (var zone = net.minecraft.util.profiling.Profiler.get().zone("covenant:ground_skill")) {
            super.tick();
            if (level().isClientSide() || !released) { return; }
            long now = ClassResources.now(owner);
            if (!validOwner() || now >= until || !supported()) { discard(); return; }
            if (banner()) { if (tickCount % 10 == 0) { refreshBanner(); } return; }
            if (now + 1e-7 >= nextImpact) {
                if (now - nextImpact > 1) { nextImpact = now; }
                nextImpact += 20 / fire.rate();
                scheduleImpact(now);
                hitAfter.values().removeIf(deadline -> deadline <= now);
            }
            for (var iterator = impacts.iterator(); iterator.hasNext();) {
                var impact = iterator.next();
                if (now >= impact.at) { iterator.remove(); impact(impact.from, impact.to, now); }
            }
        }
    }
    private void scheduleImpact(long now) {
        double angle = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * radius();
        var ground = position().add(Math.cos(angle)*r, .04, Math.sin(angle)*r);
        var top = ground.add(0,fire.height(),0);
        if (!loaded(level(), new AABB(ground.add(0,-.3,0),top)) || !level().getWorldBorder().isWithinBounds(BlockPos.containing(ground))) { return; }
        if (level().getBlockCollisions(this, new AABB(ground,ground).inflate(.01)).iterator().hasNext()) { return; }
        var ceiling = level().clip(new ClipContext(ground,top,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        if (ceiling.getType() == HitResult.Type.BLOCK) { top = ceiling.getLocation().add(0,-.04,0); }
        var bottom = ground.add(0,-.3,0);
        var floor = level().clip(new ClipContext(top,bottom,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        if (floor.getType() != HitResult.Type.BLOCK || floor.getDirection() != Direction.UP) { return; }
        var target = floor.getLocation();
        int travel = Math.max(1,(int)Math.ceil((top.y-target.y)/.8));
        if (now+travel>=until) { return; }
        impacts.add(new Impact(top,bottom,now+travel));
        entityData.set(IMPACT_FROM,top.subtract(position()).toVector3f());
        entityData.set(IMPACT_TO,target.subtract(position()).toVector3f());
        entityData.set(FALL_TICKS,travel);
        entityData.set(IMPACT_SEQUENCE,entityData.get(IMPACT_SEQUENCE)+1);
        ((ServerLevel)level()).sendParticles(CombatParticles.FIRE_ENERGY.get(),top.x,top.y,top.z,0,0,-.8,0,1);
    }
    void impact(Vec3 from, Vec3 to, long now) {
        if (!loaded(level(),new AABB(from,to).inflate(fire.splash()))) { return; }
        var hit = level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        if (hit.getType() != HitResult.Type.BLOCK) { return; }
        var point = hit.getLocation().add(0,.03,0);
        var server = (ServerLevel)level();
        server.sendParticles(CombatParticles.FIRE_COLLISION.get(),point.x,point.y,point.z,3,.15,.05,.15,.03);
        for (var target : level().getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(fire.splash()), t -> Skills.canHit(owner,t))) {
            var nearest = target.getBoundingBox().getCenter();
            double dx=Math.max(target.getBoundingBox().minX-point.x,Math.max(0,point.x-target.getBoundingBox().maxX));
            double dy=Math.max(target.getBoundingBox().minY-point.y,Math.max(0,point.y-target.getBoundingBox().maxY));
            double dz=Math.max(target.getBoundingBox().minZ-point.z,Math.max(0,point.z-target.getBoundingBox().maxZ));
            if (dx*dx+dy*dy+dz*dz > fire.splash()*fire.splash() || now < hitAfter.getOrDefault(target.getUUID(),Long.MIN_VALUE)
                    || level().clip(new ClipContext(point,nearest,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS) { continue; }
            hitAfter.put(target.getUUID(), now+fire.hitTicks());
            var before = AuraDamage.before(target);
            var type = server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(FIRE_DAMAGE);
            var source = new DamageSource(type,this,owner);
            var runtime = owner.getData(RpgData.SKILL_RUNTIME); var previous = runtime.skillDamageSource;
            runtime.skillDamageSource=source;
            try { target.hurtServer(server,source,damage); }
            finally { runtime.skillDamageSource=previous; }
            if (target.isAlive() && before.landed(target) && !target.fireImmune()) { target.igniteForTicks(burnTicks); }
        }
    }
    private void refreshBanner() {
        for (var target : level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(radius()), t ->
                t.isAlive() && !t.isSpectator() && position().distanceToSqr(t.position()) <= radius()*radius() && Auras.allied(owner,t))) {
            WarBanners.grant(this,target);
            dev.covenant.rpg.progression.CombatProgression.supportAlly(owner,target);
        }
    }
    @Override public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide() && owner != null) {
            var runtime=owner.getData(RpgData.SKILL_RUNTIME);
            if (runtime.banner==this) { runtime.banner=null; }
            if (runtime.firestorm==this) { runtime.firestorm=null; }
            for (var target : recipients) { WarBanners.remove(this,target); }
            recipients.clear(); impacts.clear();
        }
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BANNER,false); builder.define(RADIUS,0f); builder.define(SPLASH,1f);
        builder.define(IMPACT_FROM,new org.joml.Vector3f()); builder.define(IMPACT_TO,new org.joml.Vector3f());
        builder.define(IMPACT_SEQUENCE,0); builder.define(FALL_TICKS,1);
    }
    @Override protected void readAdditionalSaveData(ValueInput input) { discard(); }
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
}
