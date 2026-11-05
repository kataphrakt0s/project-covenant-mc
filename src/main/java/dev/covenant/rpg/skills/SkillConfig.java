package dev.covenant.rpg.skills;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server tuning in covenant-server.toml. Rates use units per second at 20 TPS. */
public final class SkillConfig {
    public static final ModConfigSpec SPEC;
    /** Focus recovery before the bounded native regeneration multiplier; default 25 units/second. */
    public static final ModConfigSpec.DoubleValue FOCUS_REGEN;
    /** Rage capacity before its native capacity multiplier; default 100 units. */
    public static final ModConfigSpec.DoubleValue RAGE_CAPACITY;
    /** Time-based Rage generation during combat; default 10 units/second. */
    public static final ModConfigSpec.DoubleValue RAGE_GENERATION;
    /** Out-of-combat Rage loss; default 10 units/second. */
    public static final ModConfigSpec.DoubleValue RAGE_DEPLETION;
    /** Base Mana capacity before its native capacity multiplier; default 100 units. */
    public static final ModConfigSpec.DoubleValue MANA_CAPACITY;
    /** Base Mana recovery before its native regeneration multiplier; default 5 units/second. */
    public static final ModConfigSpec.DoubleValue MANA_REGEN;

    static {
        var b = new ModConfigSpec.Builder();
        FOCUS_REGEN = rate(b, "focusRegeneration", 25, "Focus units recovered per second. Capacity is always 100; stat bonuses to recovery cap at 25%.");
        RAGE_CAPACITY = rate(b, "rageCapacity", 100, "Base Rage capacity in units, multiplied by covenant:rage_capacity.");
        RAGE_GENERATION = rate(b, "rageGeneration", 10, "Rage units generated per second while in combat; damage refreshes a fixed seven-second grace period.");
        RAGE_DEPLETION = rate(b, "rageDepletion", 10, "Rage units lost per second after seven seconds without confirmed combat damage.");
        MANA_CAPACITY = rate(b, "manaCapacity", 100, "Base Mana capacity in units, multiplied by covenant:mana_capacity. No fixed 100-unit cap.");
        MANA_REGEN = rate(b, "manaRegeneration", 5, "Mana units recovered per second, multiplied by covenant:mana_regeneration.");
        SPEC = b.build();
    }

    private static ModConfigSpec.DoubleValue rate(ModConfigSpec.Builder b, String key, double value, String comment) {
        return b.comment(comment + " Default: " + value + "; range: 0 to 1000000.")
                .defineInRange(key, value, 0, 1_000_000);
    }

    private SkillConfig() {}
}
