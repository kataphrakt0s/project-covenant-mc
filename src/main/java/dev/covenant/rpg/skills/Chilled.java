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

/** Native saved/synced slow; repeated applications refresh one effect, never add another modifier. */
public final class Chilled extends MobEffect {
    /** Base duration: three seconds, scaled by the caster's skill-duration attribute. */
    public static final int DURATION_TICKS = 60;
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Covenant.MOD_ID);
    public static final DeferredHolder<MobEffect, Chilled> EFFECT = EFFECTS.register("chilled", Chilled::new);

    private Chilled() {
        super(MobEffectCategory.HARMFUL, 0x80CAFF);
    }

    public static void register(IEventBus bus) { EFFECTS.register(bus); }

    /** Retains a longer existing application and respects native effect immunity/applicability. */
    public static void apply(LivingEntity target, LivingEntity owner, int duration) {
        if (target.level().isClientSide() || !target.isAlive() || duration <= 0) { return; }
        target.addEffect(new MobEffectInstance(EFFECT, duration, 0, false, true, false), owner);
        CovenantSlows.update(target);
    }
}
