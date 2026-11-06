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
    }

    /** Native application respects immunity, keeps a longer same-strength effect, and allows milk removal. */
    public static void apply(LivingEntity target, LivingEntity owner, int ticks) {
        if (target.level().isClientSide() || !target.isAlive() || ticks <= 0) { return; }
        if (target.addEffect(new MobEffectInstance(EFFECT, ticks, 0, false, true, true), owner)
                && owner instanceof net.minecraft.server.level.ServerPlayer player) {
            dev.covenant.rpg.progression.CombatProgression.supportEnemy(player, target);
        }
    }

}
