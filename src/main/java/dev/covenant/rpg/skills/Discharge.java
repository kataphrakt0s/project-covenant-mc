package dev.covenant.rpg.skills;

import dev.covenant.rpg.stats.RpgStats;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** One committed radial spell: consume charges before callbacks, resolve elemental defenses independently. */
public final class Discharge {
    public static final ResourceKey<DamageType> FIRE = type("discharge_fire"), ICE = type("discharge_ice"),
            LIGHTNING = type("discharge_lightning"), EXPLOSION = type("discharge_explosion");
    private static ResourceKey<DamageType> type(String name) { return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("covenant", name)); }
    private Discharge() {}
    public static boolean damage(DamageSource source) { return source.is(FIRE) || source.is(ICE) || source.is(LIGHTNING) || source.is(EXPLOSION); }

    static boolean clear(ServerPlayer owner, Vec3 from, LivingEntity target) {
        return owner.level().clip(new ClipContext(from, target.getEyePosition(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS;
    }
    private static int duration(ServerPlayer owner, int ticks) {
        return (int) Math.clamp(Math.ceil(RpgStats.scaleSkillValue(owner, RpgStats.SKILL_DURATION, ticks)), 1, Integer.MAX_VALUE);
    }
    private static boolean hit(ServerPlayer owner, LivingEntity target, ResourceKey<DamageType> type, double power) {
        if (power <= 0 || !Skills.canHit(owner, target)) { return false; }
        var before = AuraDamage.before(target);
        var holder = owner.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type);
        // Null direct entity makes this a non-dodgeable area hit; causing player retains attribution.
        target.hurtServer(owner.level(), new DamageSource(holder, null, owner), (float) power);
        return before.landed(target);
    }
    static void execute(ServerPlayer player) {
        var config = WeaverSkillData.get(player.level().getServer()).discharge();
        int[] stacks = WeaverPower.consumeCharges(player);
        WeaverVisuals.discharge(player, stacks, config);
        var center = player.position(); var eye = player.getEyePosition();
        double fire = stacks[0] * config.damage() * AuraDamage.elementalMultiplier(player, WeaverPower.Element.FIRE);
        double ice = stacks[1] * config.damage() * AuraDamage.elementalMultiplier(player, WeaverPower.Element.ICE);
        double lightning = stacks[2] * config.damage() * AuraDamage.elementalMultiplier(player, WeaverPower.Element.LIGHTNING);
        double blast = config.explosion() * AuraDamage.elementalMultiplier(player, WeaverPower.Element.FIRE);
        int slowTicks = duration(player, config.slowTicks()), freezeTicks = duration(player, config.freezeTicks()), burnTicks = duration(player, config.burnTicks());
        var targets = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(config.radius()),
                t -> Skills.canHit(player, t) && center.distanceToSqr(t.position()) <= config.radius() * config.radius() && clear(player, eye, t));
        targets.sort(Comparator.comparingDouble((LivingEntity t) -> center.distanceToSqr(t.position())).thenComparing(LivingEntity::getUUID));
        Set<UUID> claimed = new HashSet<>(); LivingEntity first = targets.isEmpty() ? null : targets.getFirst();
        for (var target : targets) {
            claimed.add(target.getUUID());
            boolean landed = hit(player, target, ICE, ice);
            if (landed && target.isAlive()) {
                if (stacks[1] == config.stacks()) { Auras.dischargeFreeze(player, target, freezeTicks); }
                else { DischargeSlow.apply(target, player, config.slow(), slowTicks); }
            }
            boolean fireLanded = hit(player, target, FIRE, fire);
            if (fireLanded && target.isAlive() && !target.fireImmune()) { target.igniteForTicks(burnTicks); }
            landed |= fireLanded;
            landed |= hit(player, target, LIGHTNING, lightning);
            if (stacks[0] == config.stacks()) {
                landed |= hit(player, target, EXPLOSION, blast * Math.max(0, 1 - center.distanceTo(target.position()) / config.radius()));
            }
            if (landed) { WeaverVisuals.contact(player, target); }
        }
        if (stacks[2] == config.stacks() && first != null) {
            var from = first.getEyePosition();
            for (int jump = 0; jump < config.chainTargets(); jump++) {
                final var origin = from;
                var next = player.level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(origin, origin).inflate(config.chainRange()),
                        t -> !claimed.contains(t.getUUID()) && Skills.canHit(player, t)
                                && center.distanceToSqr(t.position()) > config.radius() * config.radius()
                                && origin.distanceToSqr(t.getEyePosition()) <= config.chainRange() * config.chainRange() && clear(player, origin, t))
                        .stream().min(Comparator.comparingDouble((LivingEntity t) -> origin.distanceToSqr(t.getEyePosition())).thenComparing(LivingEntity::getUUID)).orElse(null);
                if (next == null) { break; }
                claimed.add(next.getUUID()); SkillVisuals.lightning(player.level(), origin, next.getEyePosition());
                if (hit(player, next, LIGHTNING, lightning * config.chainMultiplier())) { WeaverVisuals.contact(player, next); }
                from = next.getEyePosition();
            }
        }
    }
}
