package dev.covenant.rpg.skills;

import dev.covenant.Covenant;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Presentation-only native particles; all respect Minecraft's particle limiter and distance checks. */
public final class CombatParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Covenant.MOD_ID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ARCANE_MOTE = type("arcane_mote");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ARCANE_COLLISION = type("arcane_collision");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CONTACT = type("combat_contact");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PIERCING_STREAK = type("piercing_streak");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CLEAVE = type("cleave");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EMBER = type("spell_ember");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ICE_SHARD = type("spell_ice_shard");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE_COLLISION = type("fire_collision");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ICE_COLLISION = type("ice_collision");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LIGHTNING_MOTE = type("lightning_mote");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LIGHTNING_COLLISION = type("lightning_collision");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LIGHTNING_THREAD = type("lightning_thread");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AIMED_STREAK = type("aimed_streak");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ARCANE_STREAK = type("arcane_streak");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOLLEY_STREAK = type("volley_streak");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AIMED_GATHER = type("aimed_gather");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ARCANE_CONTACT = type("arcane_contact");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> REND_CONTACT = type("rend_contact");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLEED_TICK = type("bleed_tick");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE_ENERGY = type("fire_energy");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ICE_ENERGY = type("ice_energy");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ARCANE_ENERGY = type("arcane_energy");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ELECTRIC_ENERGY = type("electric_energy");

    private CombatParticles() {}

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> type(String name) {
        return TYPES.register(name, () -> new SimpleParticleType(false));
    }

    public static void register(IEventBus modBus) { TYPES.register(modBus); }
}
