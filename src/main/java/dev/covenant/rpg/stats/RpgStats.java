package dev.covenant.rpg.stats;

import dev.covenant.Covenant;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * RPG stats are ordinary Minecraft attributes: no second effective-stat store or profile cache.
 * Modifier stat IDs are attribute registry IDs, allowing other mods' registered attributes too.
 */
public final class RpgStats {
    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, Covenant.MOD_ID);

    /** Maximum health in health points (2 = one heart); vanilla player base 20. */
    public static final Holder<Attribute> HEALTH = Attributes.MAX_HEALTH;
    /** Vanilla attack damage in health points; unarmed player base 1, before weapon modifiers. */
    public static final Holder<Attribute> DAMAGE = Attributes.ATTACK_DAMAGE;
    /** Vanilla armor points; base 0. Vanilla toughness and damage-source bypass rules still apply. */
    public static final Holder<Attribute> ARMOR = Attributes.ARMOR;
    /** Vanilla movement coefficient; player base approximately 0.1, not blocks per second. */
    public static final Holder<Attribute> MOVEMENT_SPEED = Attributes.MOVEMENT_SPEED;
    /** Fully charged attacks per second; vanilla player base 4 before weapon modifiers. */
    public static final Holder<Attribute> ATTACK_SPEED = Attributes.ATTACK_SPEED;

    /** Probability per incoming melee/projectile hit: default 0, clamped to [0, 1]. ADD 0.1 = +10 points. */
    public static final DeferredHolder<Attribute, Attribute> DODGE = register("dodge", 0, 1, Attribute.Sentiment.POSITIVE);
    /** Skill cost coefficient for any resource: default 1; range [0, 1024]; 0.8 costs 20% less. */
    public static final DeferredHolder<Attribute, Attribute> RESOURCE_COST = register("resource_cost", 1, 1024, Attribute.Sentiment.NEGATIVE);
    /** Skill cooldown coefficient: default 1; range [0, 1024]; 0.8 takes 20% less time. */
    public static final DeferredHolder<Attribute, Attribute> COOLDOWN = register("cooldown", 1, 1024, Attribute.Sentiment.NEGATIVE);
    /** Skill cast-time coefficient: default 1; range [0, 1024]; 0 is instant. */
    public static final DeferredHolder<Attribute, Attribute> CAST_TIME = register("cast_time", 1, 1024, Attribute.Sentiment.NEGATIVE);
    /** Skill duration coefficient: default 1; range [0, 1024]; 1.2 lasts 20% longer. */
    public static final DeferredHolder<Attribute, Attribute> SKILL_DURATION = register("skill_duration", 1, 1024, Attribute.Sentiment.POSITIVE);

    /** Focus regeneration coefficient; default 1, hard upper bound 1.25. No Focus capacity attribute. */
    public static final DeferredHolder<Attribute, Attribute> FOCUS_REGENERATION = register("focus_regeneration", 1, 1.25, Attribute.Sentiment.POSITIVE);
    /** Rage capacity coefficient applied to server config; default 1, range [0, 1024]. */
    public static final DeferredHolder<Attribute, Attribute> RAGE_CAPACITY = register("rage_capacity", 1, 1024, Attribute.Sentiment.POSITIVE);
    /** Damage-driven outgoing/incoming Rage gain multiplier; default 1, range [0, 1024]. */
    public static final DeferredHolder<Attribute, Attribute> RAGE_GENERATION = register("rage_generation", 1, 1024, Attribute.Sentiment.POSITIVE);
    /** Mana capacity coefficient applied to server config; default 1, range [0, 1024]. */
    public static final DeferredHolder<Attribute, Attribute> MANA_CAPACITY = register("mana_capacity", 1, 1024, Attribute.Sentiment.POSITIVE);
    /** Mana regeneration coefficient applied to server config; default 1, range [0, 1024]. */
    public static final DeferredHolder<Attribute, Attribute> MANA_REGENERATION = register("mana_regeneration", 1, 1024, Attribute.Sentiment.POSITIVE);

    /** Supported Covenant stats, in display order; registry IDs are also the debug command identifiers. */
    public static final List<Holder<Attribute>> SUPPORTED = List.of(HEALTH, DAMAGE, ARMOR,
            MOVEMENT_SPEED, ATTACK_SPEED, DODGE, RESOURCE_COST, COOLDOWN, CAST_TIME, SKILL_DURATION,
            FOCUS_REGENERATION, RAGE_CAPACITY, RAGE_GENERATION, MANA_CAPACITY, MANA_REGENERATION);

    private RpgStats() {}

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        modBus.addListener(RpgStats::addPlayerAttributes);
        modBus.addListener(RpgStats::setup);
        NeoForge.EVENT_BUS.addListener(PageAttributeModifiers::apply);
        NeoForge.EVENT_BUS.addListener(RpgStats::dodge);
        NeoForge.EVENT_BUS.addListener(EquipmentHealth::onClone);
    }

    /**
     * Applies an effective skill coefficient to a skill's own nonnegative base value. Units are
     * preserved (resource units, seconds, or ticks). Skills own rounding and minimum usable times.
     * Trusted server skill code should use this at execution time; clients may use synced values
     * for previews. This neither spends resources nor starts timers.
     */
    public static double scaleSkillValue(Player player, Holder<Attribute> coefficient, double baseValue) {
        if (!Double.isFinite(baseValue) || baseValue < 0) {
            throw new IllegalArgumentException("Skill base must be finite and nonnegative");
        }
        double result = baseValue * player.getAttributeValue(coefficient);
        if (!Double.isFinite(result) || result < 0) {
            throw new IllegalArgumentException("Effective skill value must be finite and nonnegative");
        }
        return result;
    }

    private static DeferredHolder<Attribute, Attribute> register(String name, double base, double maximum,
            Attribute.Sentiment sentiment) {
        return ATTRIBUTES.register(name, () -> new RangedAttribute(
                "attribute.covenant." + name, base, 0, maximum).setSyncable(true).setSentiment(sentiment));
    }

    private static void setup(FMLCommonSetupEvent event) {
        // Vanilla leaves attack damage unsynced. Include it in the native attribute packets too.
        event.enqueueWork(() -> DAMAGE.value().setSyncable(true));
    }

    private static void addPlayerAttributes(EntityAttributeModificationEvent event) {
        for (var attribute : ATTRIBUTES.getEntries()) {
            event.add(EntityType.PLAYER, attribute);
        }
    }

    private static void dodge(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()
                || event.getAmount() <= 0 || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            return;
        }
        var direct = event.getSource().getDirectEntity();
        if ((direct instanceof LivingEntity || direct instanceof Projectile
                || (direct instanceof dev.covenant.rpg.skills.TrapEntity && event.getSource().is(dev.covenant.rpg.skills.TrapKind.BEAR.damageType)))
                && direct != player && player.getRandom().nextDouble() < player.getAttributeValue(DODGE)) {
            // Cancel before armor/shield processing: a dodged hit consumes neither health nor durability.
            event.setCanceled(true);
        }
    }
}
