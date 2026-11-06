package dev.covenant.rpg.skills;

import dev.covenant.Covenant;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One native saved/synced harmful status shared by both combos and every attacker. */
public final class ArmorShred extends MobEffect {
    /** Prototype: 20% armor reduction, five-second base duration; toughness is unchanged. */
    public static final int DURATION_TICKS = 100;
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Covenant.MOD_ID);
    public static final DeferredHolder<MobEffect, ArmorShred> EFFECT = EFFECTS.register("armor_shred", ArmorShred::new);

    private ArmorShred() {
        super(MobEffectCategory.HARMFUL, 0xA78372);
        addAttributeModifier(Attributes.ARMOR, Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "armor_shred"),
                -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
        NeoForge.EVENT_BUS.addListener(ArmorShred::died);
    }

    /** Native application respects immunity, keeps a longer same-strength effect, and allows milk removal. */
    public static void apply(LivingEntity target, LivingEntity owner, int ticks) {
        if (target.level().isClientSide() || !target.isAlive() || ticks <= 0) { return; }
        target.addEffect(new MobEffectInstance(EFFECT, ticks, 0, false, true, true), owner);
    }

    private static void died(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) { event.getEntity().removeEffect(EFFECT); }
    }
}
