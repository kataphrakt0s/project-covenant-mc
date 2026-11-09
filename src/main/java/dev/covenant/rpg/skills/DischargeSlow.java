package dev.covenant.rpg.skills;

import dev.covenant.Covenant;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native saved/synced slow; amplifier encodes percentage points minus one (1..100%). */
public final class DischargeSlow extends MobEffect {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Covenant.MOD_ID);
    public static final DeferredHolder<MobEffect, DischargeSlow> EFFECT = EFFECTS.register("discharge_slow", DischargeSlow::new);
    private DischargeSlow() { super(MobEffectCategory.HARMFUL, 0x80CAFF); }
    static void register(IEventBus bus) { EFFECTS.register(bus); }
    static void apply(LivingEntity target, LivingEntity owner, double strength, int ticks) {
        int percent = (int) Math.round(strength * 100);
        if (percent <= 0) { return; }
        target.addEffect(new MobEffectInstance(EFFECT, ticks, percent - 1, false, true, false), owner);
        CovenantSlows.update(target);
    }
    static double strength(LivingEntity target) {
        var effect = target.getEffect(EFFECT); return effect == null ? 0 : (effect.getAmplifier() + 1) / 100.0;
    }
}
