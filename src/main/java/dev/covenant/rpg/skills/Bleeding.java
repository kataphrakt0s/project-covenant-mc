package dev.covenant.rpg.skills;

import dev.covenant.Covenant;
import dev.covenant.rpg.RpgData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Server-owned, bounded bleed stacks on targets. No world scans, chunk loading, or offline catch-up. */
public final class Bleeding {
    /** Rend's base duration: five seconds at 20 TPS, scaled by the existing skill-duration multiplier. */
    public static final int REND_DURATION_TICKS = 100;
    /** Each stack deals 10% of application-time native attack damage on each one-second pulse. */
    public static final double REND_DAMAGE_FRACTION = 0.10;
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "bleed"));

    private Bleeding() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Bleeding::tick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Bleeding::died);
    }

    /** Positive damage confirmation belongs to the applying skill; this trusted hook owns stack mutation. */
    public static void apply(LivingEntity target, ServerPlayer attacker, float damage, int durationTicks) {
        ClassResources.requireServer(attacker);
        apply(target, (LivingEntity) attacker, damage, durationTicks);
    }

    /** Allied pets use the same target-owned stacks, with their own living-actor UUID attribution. */
    public static void apply(LivingEntity target, LivingEntity attacker, float damage, int durationTicks) {
        if (!(attacker.level() instanceof ServerLevel level) || !level.getServer().isSameThread()) {
            throw new IllegalStateException("Bleed application requires the logical server thread");
        }
        if (!(target.level() instanceof ServerLevel) || target.level() != attacker.level()) {
            throw new IllegalArgumentException("Bleed application requires a target in the attacker's server level");
        }
        if (!target.isAlive()) { return; }
        target.getData(RpgData.BLEED).apply(attacker.getUUID(), damage, durationTicks);
    }

    public static int count(LivingEntity target) {
        return target.hasData(RpgData.BLEED) ? target.getData(RpgData.BLEED).count() : 0;
    }

    /** Shared consumption boundary for future finishers; removes every attacker's stacks. */
    public static int consume(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level) || !level.getServer().isSameThread()) {
            throw new IllegalStateException("Bleed consumption requires the logical server thread");
        }
        int removed = count(target);
        target.removeData(RpgData.BLEED);
        return removed;
    }

    private static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target) || !(target.level() instanceof ServerLevel level)
                || !target.hasData(RpgData.BLEED)) { return; }
        if (!target.isAlive()) { target.removeData(RpgData.BLEED); return; }
        var state = target.getData(RpgData.BLEED);
        if (state.advancePulse()) {
            // At most three snapshots once per second; damage/death hooks may consume or reapply bleeds.
            var pulse = state.stacks().toArray(BleedState.Stack[]::new);
            var type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE_TYPE);
            for (var stack : pulse) {
                if (!target.isAlive() || !target.hasData(RpgData.BLEED)
                        || target.getData(RpgData.BLEED) != state || !state.stacks().contains(stack)) { break; }
                var entity = level.getEntity(stack.attacker());
                LivingEntity attacker = entity instanceof LivingEntity living ? living : level.getServer().getPlayerList().getPlayer(stack.attacker());
                // No direct strike/projectile: bleed cannot be dodged. The causing player still gets combat credit.
                var source = new DamageSource(type, null, attacker);
                var before = AuraDamage.before(target);
                // Preserve UUID kill credit even when the applying player is offline.
                if (attacker == null || attacker instanceof ServerPlayer) { target.setLastHurtByPlayer(stack.attacker(), 100); }
                target.hurtServer(level, source, stack.damage());
                if (before.landed(target)) {
                    SkillVisuals.bleedTick(level, target);
                }
            }
        }
        if (target.hasData(RpgData.BLEED) && target.getData(RpgData.BLEED) == state) {
            state.age();
            if (state.count() == 0) { target.removeData(RpgData.BLEED); }
        }
    }

    private static void died(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) { event.getEntity().removeData(RpgData.BLEED); }
    }
}
