package dev.covenant.rpg.skills;

import dev.covenant.Covenant;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native saved/synced relative timer; movement hooks suppress input, never existing velocity. */
public final class Rooted extends MobEffect {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Covenant.MOD_ID);
    public static final DeferredHolder<MobEffect, Rooted> EFFECT = EFFECTS.register("rooted", Rooted::new);

    private Rooted() { super(MobEffectCategory.HARMFUL, 0xB6B6B6); }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Rooted::died);
    }

    public static boolean contains(LivingEntity target) { return target.hasEffect(EFFECT); }

    public static void apply(LivingEntity target, int duration) {
        if (!target.isAlive() || duration <= 0) { return; }
        var previous = target.getEffect(EFFECT);
        if (previous == null || (!previous.isInfiniteDuration() && previous.getDuration() < duration)) {
            // Native forceAddEffect keeps saving/syncing and applicability hooks, while avoiding
            // blanket addEffect overrides on bosses such as the Wither.
            target.forceAddEffect(new MobEffectInstance(EFFECT, duration, 0, false, true, false), null);
        }
    }

    private static void died(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) { event.getEntity().removeEffect(EFFECT); }
    }
}
