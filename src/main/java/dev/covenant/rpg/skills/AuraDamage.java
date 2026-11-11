package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.stats.RpgStats;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Narrow native damage hooks. A hit frame keeps separate shield accounting correct under nested damage. */
public final class AuraDamage {
    public static final ResourceKey<DamageType> IMMOLATION = type("immolation"), ENCHANTING = type("enchanting");
    private static final List<ResourceKey<DamageType>> CALCULATED_TYPES = List.of(Bleeding.DAMAGE_TYPE, RageStorm.DAMAGE_TYPE, IMMOLATION, ENCHANTING,
            type("fire_bolt"), type("ice_bolt"), type("lightning_bolt"),
            TrapKind.BEAR.damageType, TrapKind.FROST.damageType, TrapKind.FLAME.damageType, TrapKind.BLAST.damageType);
    private static ResourceKey<DamageType> type(String id) { return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("covenant", id)); }
    private static final ThreadLocal<Hit> HIT = new ThreadLocal<>();
    public static final class Hit {
        final Hit previous;
        final LivingEntity target;
        final DamageSource source;
        final int retaliationTicks;
        final float healthBefore, absorptionBefore;
        final boolean eligibleEnemy;
        float absorbed;
        Hit(LivingEntity target, DamageSource source) {
            this.previous = HIT.get(); this.target = target; this.source = source;
            healthBefore = target.getHealth(); absorptionBefore = target.getAbsorptionAmount();
            eligibleEnemy = source.getEntity() instanceof LivingEntity owner && Skills.canHit(owner, target);
            retaliationTicks = !Auras.active(target, Auras.Kind.FLAME_CLOAK) ? 0 : target instanceof Player player
                    ? (int) Math.clamp(Math.ceil(RpgStats.scaleSkillValue(player, RpgStats.SKILL_DURATION, 80)), 1, Integer.MAX_VALUE) : 80;
        }
    }
    /** Transient per-recipient hit accounting; committed projectile history lives in ProjectileCombat. */
    public static final class Combat {
        final Map<UUID, Long> ripping = new HashMap<>();
        double shieldSpent;
    }
    public record Before(float health, float absorption, double shieldSpent) {
        public boolean landed(LivingEntity target) {
            return target.getHealth() < health || target.getAbsorptionAmount() < absorption
                    || (target.hasData(RpgData.AURA_COMBAT) && target.getData(RpgData.AURA_COMBAT).shieldSpent > shieldSpent);
        }
    }
    public static Before before(LivingEntity target) {
        return new Before(target.getHealth(), target.getAbsorptionAmount(), target.hasData(RpgData.AURA_COMBAT) ? target.getData(RpgData.AURA_COMBAT).shieldSpent : 0);
    }
    public static Hit begin(LivingEntity target, DamageSource source) { var hit = new Hit(target, source); HIT.set(hit); return hit; }
    public static void end(Hit hit) { if (hit.previous == null) { HIT.remove(); } else { HIT.set(hit.previous); } }
    public static float shieldDamage(LivingDamageEvent.Post event) {
        var hit = HIT.get();
        return hit != null && hit.target == event.getEntity() && hit.source == event.getSource() ? hit.absorbed : 0;
    }
    public static float landedDamage(LivingDamageEvent.Post event) { return event.getInflictedDamage() + shieldDamage(event); }

    private AuraDamage() {}
    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, AuraDamage::incoming);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, AuraDamage::shield);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, AuraDamage::landed);
    }

    public static double multiplier(LivingEntity source, boolean fire) {
        return 1 + Auras.battleCryBonus(source) + (Auras.active(source, Auras.Kind.RAGING) ? .2 : 0)
                + (fire && Auras.active(source, Auras.Kind.IMMOLATION) ? .3 : 0);
    }
    /** Elemental buff joins outgoing bonuses additively, captured once at spell/projectile creation. */
    public static double elementalMultiplier(LivingEntity source, WeaverPower.Element element) {
        return multiplier(source, element == WeaverPower.Element.FIRE) + WeaverPower.bonus(source, element) + ElementalGolems.spellBonus(source);
    }
    /** Bleed power captures outgoing and bleed bonuses additively once; pulses never reapply them. */
    public static float bleedPower(LivingEntity source) {
        if (source.getAttribute(RpgStats.DAMAGE) == null) { return 0; }
        return (float) (source.getAttributeValue(RpgStats.DAMAGE) * .1
                * (1 + Auras.battleCryBonus(source) + (Auras.passive(source, Auras.Kind.RIPPING) ? .2 : 0) + (Auras.active(source, Auras.Kind.RAGING) ? .2 : 0)));
    }
    private static void incoming(LivingIncomingDamageEvent event) {
        try (var zone = Profiler.get().zone("covenant:damage_incoming")) {
            var source = event.getSource();
            double amount = event.getAmount();
            // Projectiles with committed power were already scaled at their firing boundary.
            var direct = source.getDirectEntity();
            if (direct instanceof Projectile && direct.hasData(RpgData.AURA_PROJECTILE_POWER)
                    && !(direct instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow)
                    && !(direct instanceof ElementalBolt) && !(direct instanceof ArcaneBolt)) {
                // Native crossbow rockets have no arrow base-damage field; retain their firing snapshot here.
                amount *= direct.getData(RpgData.AURA_PROJECTILE_POWER);
            }
            if (source.getEntity() instanceof LivingEntity player && !calculated(player, source)
                    && (direct == null || !direct.hasData(RpgData.AURA_PROJECTILE_POWER))) {
                var element = WeaverPower.element(source);
                amount *= element == null ? multiplier(player, false) : elementalMultiplier(player, element);
            }
            double vulnerability = (Auras.passive(event.getEntity(), Auras.Kind.SHOCK) ? .1 : 0)
                    + (event.getEntity().hasEffect(Vulnerability.EFFECT) ? Vulnerability.STRENGTH : 0);
            amount *= 1 + vulnerability;
            if (source.is(DamageTypeTags.IS_FIRE) && Auras.passive(event.getEntity(), Auras.Kind.FLAME_CLOAK)) { amount *= .8; }
            event.setAmount((float) amount);
        }
    }

    private static boolean calculated(LivingEntity player, DamageSource source) {
        if (Discharge.damage(source)) { return true; }
        if (player instanceof ServerPlayer serverPlayer && source == serverPlayer.getData(RpgData.SKILL_RUNTIME).skillDamageSource) { return true; }
        for (int i = 0; i < CALCULATED_TYPES.size(); i++) { if (source.is(CALCULATED_TYPES.get(i))) { return true; } }
        return false;
    }
    private static void shield(LivingDamageEvent.Pre event) {
        try (var zone = Profiler.get().zone("covenant:damage_shield")) {
            if (event.getNewDamage() > 0 && Auras.shield(event.getEntity()) > 0) {
                var owner = event.getEntity().getData(RpgData.AURAS).shieldOwner;
                if (owner != null) { dev.covenant.rpg.progression.CombatProgression.defended(event.getEntity(), event.getSource(), Set.of(owner)); }
            }
            float absorbed = Auras.absorb(event.getEntity(), event.getNewDamage());
            if (absorbed <= 0) { return; }
            var hit = HIT.get();
            if (hit != null && hit.target == event.getEntity() && hit.source == event.getSource()) { hit.absorbed += absorbed; }
            event.getEntity().getData(RpgData.AURA_COMBAT).shieldSpent += absorbed;
            event.setNewDamage(event.getNewDamage() - absorbed);
        }
    }

    private static boolean melee(DamageSource source) {
        return source.getDirectEntity() == source.getEntity() && source.getEntity() instanceof LivingEntity
                && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO));
    }
    private static boolean skill(ServerPlayer player, DamageSource source) {
        return player.getData(RpgData.SKILL_RUNTIME).skillDamageSource == source
                || ProjectileCombat.isSkill(source.getDirectEntity());
    }

    private static void landed(LivingDamageEvent.Post event) {
        try (var zone = Profiler.get().zone("covenant:damage_landed")) {
            var frame = HIT.get();
            if (frame != null && frame.target == event.getEntity() && frame.source == event.getSource() && frame.eligibleEnemy
                    && event.getSource().getEntity() instanceof ServerPlayer caster) {
                double actual = Math.min(frame.healthBefore, Math.max(0, event.getHealthDamage()))
                        + Math.min(frame.absorptionBefore, event.getReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION))
                        + frame.absorbed;
                WeaverPower.bank(caster, WeaverPower.element(event.getSource()), actual);
            }
            if (landedDamage(event) <= 0) { return; }
            var source = event.getSource();
            var target = event.getEntity();
            if (!(target.level() instanceof ServerLevel level)) { return; }
            var hit = HIT.get();
            if (melee(source) && source.getEntity() instanceof LivingEntity attacker && Skills.canHit(target, attacker)
                    && hit != null && hit.retaliationTicks > 0 && !attacker.fireImmune()) {
                attacker.igniteForTicks(hit.retaliationTicks);
            }
            if (!(source.getEntity() instanceof LivingEntity attacker) || !Skills.canHit(attacker, target)) { return; }
            boolean isSkill = attacker instanceof ServerPlayer player && skill(player, source);
            if (melee(source) && Auras.active(attacker, Auras.Kind.RIPPING)
                    && !(attacker instanceof ServerPlayer player && player.getData(RpgData.SKILL_RUNTIME).applyingRend)) {
                var deadlines = target.getData(RpgData.AURA_COMBAT).ripping;
                long now = Auras.now(target);
                deadlines.values().removeIf(until -> until <= now);
                if (!deadlines.containsKey(attacker.getUUID())) {
                    deadlines.put(attacker.getUUID(), now + 20);
                    applyBleed(target, attacker);
                }
            }
            if (isSkill || source.is(ENCHANTING)) { return; }
            float bonus = 0;
            if (melee(source) && Auras.active(attacker, Auras.Kind.ENCHANTING)) {
                bonus = (float) ((attacker instanceof Player player ? 2 * player.getAttackStrengthScale(.5f) : 2) * multiplier(attacker, false));
            } else if (source.getDirectEntity() instanceof Projectile projectile
                    && projectile.hasData(RpgData.ENCHANTED_SHOT)
                    && projectile.getData(RpgData.ENCHANTED_SHOT) > 0
                    && ProjectileCombat.claimEnchantment(projectile, target)) {
                bonus = projectile.getData(RpgData.ENCHANTED_SHOT);
            }
            if (bonus > 0 && target.isAlive()) {
                var type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ENCHANTING);
                target.hurtServer(level, new DamageSource(type, null, attacker), bonus);
            }
        }
    }

    public static void applyBleed(LivingEntity target, LivingEntity player) {
        float power = bleedPower(player);
        if (power > 0) { Bleeding.apply(target, player, power, player instanceof Player p
                ? (int) Math.clamp(Math.ceil(RpgStats.scaleSkillValue(p, RpgStats.SKILL_DURATION, Bleeding.REND_DURATION_TICKS)), 1, Integer.MAX_VALUE)
                : Bleeding.REND_DURATION_TICKS); }
    }

    /** Called by native bow/crossbow firing for each projectile, including multishot. */
    public static void snapshot(Projectile projectile, LivingEntity shooter, float bowStrength) {
        ProjectileCombat.captureSupport(projectile, shooter);
        projectile.setData(RpgData.AURA_PROJECTILE_POWER, multiplier(shooter, false));
        if (projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow) {
            arrow.setBaseDamage(((dev.covenant.mixin.ArrowPiercingAccessor) arrow).covenant$getBaseDamage() * projectile.getData(RpgData.AURA_PROJECTILE_POWER));
        }
        if (Auras.active(shooter, Auras.Kind.ENCHANTING)) {
            projectile.setData(RpgData.ENCHANTED_SHOT, (float) (2 * Math.clamp(bowStrength, 0, 1) * multiplier(shooter, false)));
        }
    }

    static void immolate(LivingEntity target, Auras.State state, long now) {
        var grants = state.passive.get(Auras.Kind.IMMOLATION);
        if (grants == null || grants.isEmpty()) { state.nextImmolation = -1; return; }
        if (state.nextImmolation < 0) { state.nextImmolation = now + 20; }
        if (now < state.nextImmolation) { return; }
        state.nextImmolation = now + 20;
        var level = (ServerLevel) target.level();
        ServerPlayer strongest = null;
        double power = -1;
        for (var grant : grants.entrySet()) {
            var owner = level.getServer().getPlayerList().getPlayer(grant.getKey());
            if (grant.getValue() <= now || owner == null || owner.level() != level) { continue; }
            double candidate = elementalMultiplier(owner, WeaverPower.Element.FIRE);
            if (candidate > power || (candidate == power && owner.getUUID().compareTo(strongest.getUUID()) < 0)) { strongest = owner; power = candidate; }
        }
        if (strongest == null) { return; }
        var type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(IMMOLATION);
        target.hurtServer(level, new DamageSource(type, null, strongest), (float) power);
    }
}
