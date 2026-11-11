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
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RETREAT_MOTE = type("retreat_mote");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLINK_MOTE = type("blink_mote");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DISENGAGE_DEPARTURE = type("disengage_departure");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DISENGAGE_TRAIL = type("disengage_trail");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DISENGAGE_ARRIVAL = type("disengage_arrival");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TELEPORT_DEPARTURE = type("teleport_departure");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TELEPORT_ARRIVAL = type("teleport_arrival");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COMBO_STEEL = type("combo_steel");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COMBO_CRIMSON = type("combo_crimson");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LUNGE_TRAIL = type("lunge_trail");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RUSH_TRAIL = type("rush_trail");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LUNGE_STRIKE = type("lunge_strike");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RIP_SWEEP = type("rip_sweep");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RUSH_FINISHER = type("rush_finisher");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COMBO_CONTACT = type("combo_contact");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> COMBO_CONSUME = type("combo_consume");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BATTLE_CRY_PULSE = type("battle_cry_pulse");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BATTLE_CRY_RECEIVED = type("battle_cry_received");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STORM_DISSIPATE = type("storm_dissipate");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DISCHARGE_FIRE = type("discharge_fire");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DISCHARGE_ICE = type("discharge_ice");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DISCHARGE_LIGHTNING = type("discharge_lightning");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> THREEFOLD_CONSUME = type("threefold_consume");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> THREEFOLD_EXPIRE = type("threefold_expire");

    private CombatParticles() {}

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AURA_RELEASE = type("aura_release");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AURA_RECEIVED = type("aura_received");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AURA_CONSUMED = type("aura_consumed");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AURA_EXPIRED = type("aura_expired");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AURA_ABSORB = type("aura_absorb");

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> type(String name) {
        return TYPES.register(name, () -> new SimpleParticleType(false));
    }

    public static void register(IEventBus modBus) { TYPES.register(modBus); }
}
