package dev.covenant.rpg.skills;

import dev.covenant.rpg.classes.PlayerClass;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Small code-backed catalog. Times are ticks and costs are resource units.
 * Power/range are interpreted by the effect family; unused fields are zero.
 * TrapKind owns trap damage, radius, and duration instead of duplicating them here.
 */
public record SkillDefinition(Identifier id, Set<PlayerClass> allowedClasses, double cost,
        int cooldownTicks, int castTicks, Effect effect, double power, double range) {
    public enum Effect { BOW_SHOT, CHANNEL, FIRESTORM, WAR_BANNER, DISENGAGE, HUNTERS_PET, ELEMENTAL_GOLEM, CLEAVE, REND, RAGE_STORM, BATTLE_CRY, PASSIVE, ARCANE_BOLT, ELEMENTAL_BOLT, LIGHTNING_BOLT, DISCHARGE, THREEFOLD, TELEPORT, TRAP, AURA, COMBO }
    public static final SkillDefinition BARRAGE = define("barrage", PlayerClass.STALKER, 6, 0, 0, Effect.CHANNEL, 1, 0);
    public static final SkillDefinition FIRESTORM = define("firestorm", PlayerClass.WEAVER, 40, 240, 15, Effect.FIRESTORM, 3, 24);
    public static final SkillDefinition WAR_BANNER = define("war_banner", PlayerClass.REAVER, 35, 700, 0, Effect.WAR_BANNER, 1, 6);
    /** Rank 1: 35 Focus; 600-tick cooldown starts on defeat/dismissal, not summon. See HunterPetData. */
    public static final SkillDefinition HUNTERS_PET = define("hunters_pet", PlayerClass.STALKER, 35, 600, 0, Effect.HUNTERS_PET, 0, 0);
    /** Rank 1: 40 Mana; 600-tick cooldown after dismissal/defeat. Active presses cycle forms for free. */
    public static final SkillDefinition ELEMENTAL_GOLEM = define("elemental_golem", PlayerClass.WEAVER, 40, 600, 0, Effect.ELEMENTAL_GOLEM, 0, 12);
    /** Rank 1: 20 Focus, six-second cooldown, four-block backward hop over six ticks. */
    public static final SkillDefinition DISENGAGE = define("disengage", PlayerClass.STALKER, 20, 120, 0, Effect.DISENGAGE, 0, 4);
    /** Rank 1: 30 Focus, eight-second cooldown, four magic damage and a four-second vulnerability. */
    public static final SkillDefinition ARCANE_ARROW = define("arcane_arrow", PlayerClass.STALKER, 30, 160, 0, Effect.BOW_SHOT, 4, 0);
    /** Rank 1: five arrows across thirty degrees; native damage coefficient 1.5 per arrow. */
    public static final SkillDefinition MULTISHOT = define("multishot", PlayerClass.STALKER, 30, 160, 0, Effect.BOW_SHOT, .75, 0);
    public static final SkillDefinition PIERCING_SHOT = define("piercing_shot", PlayerClass.STALKER, 30, 40, 0, Effect.BOW_SHOT, 1.5, 0);
    /** Prototype tuning: 40 Focus, 4 seconds cooldown, 1 second wind-up, double native arrow base damage. */
    public static final SkillDefinition AIMED_SHOT = define("aimed_shot", PlayerClass.STALKER, 40, 80, 20, Effect.BOW_SHOT, 2, 0);
    public static final SkillDefinition CLEAVE = define("cleave", PlayerClass.REAVER, 20, 60, 0, Effect.CLEAVE, 1, 3);
    /** Instant native-reach melee hit; 20 Rage, two-second cooldown. Bleed tuning lives in Bleeding. */
    public static final SkillDefinition REND = define("rend", PlayerClass.REAVER, 20, 40, 0, Effect.REND, 1, 0);
    /** Rank 1 shout: 30 Rage, thirty-second cooldown; duration, radius and strength in ReaverSkillData. */
    public static final SkillDefinition BATTLE_CRY = define("battle_cry", PlayerClass.REAVER, 30, 600, 0, Effect.BATTLE_CRY, .15, 6);
    /** Rank 1 travelling area: 35 Rage, twelve-second cooldown, sixteen-tick wind-up. */
    public static final SkillDefinition RAGE_STORM = define("rage_storm", PlayerClass.REAVER, 35, 240, 16, Effect.RAGE_STORM, 3, 8);
    /** Free self-only passive while assigned; never activates or starts a cooldown. */
    public static final SkillDefinition BLOOD_FEVER = define("blood_fever", PlayerClass.REAVER, 0, 0, 0, Effect.PASSIVE, 0, 4);
    /** Three fresh presses: 20/10/20 Rage; cooldown starts at sequence end. Stage tuning lives in Combos. */
    public static final SkillDefinition LUNGING_COMBO = define("lunging_combo", PlayerClass.REAVER, 20, 240, 0, Effect.COMBO, 0, 3);
    /** Circular Rend, six-block dash, bleed-consuming finisher; 30/20/20 Rage and 20-second end cooldown. */
    public static final SkillDefinition RIP_AND_RUSH = define("rip_and_rush", PlayerClass.REAVER, 30, 400, 0, Effect.COMBO, 0, 3);
    public static final SkillDefinition ARCANE_BOLT = define("arcane_bolt", PlayerClass.WEAVER, 20, 40, 10, Effect.ARCANE_BOLT, 6, 0);
    /** Prototype bolts: 20 Mana, two-second cooldown, half-second wind-up, six damage. */
    public static final SkillDefinition FIRE_BOLT = define("fire_bolt", PlayerClass.WEAVER, 20, 40, 10, Effect.ELEMENTAL_BOLT, 6, 0);
    public static final SkillDefinition ICE_BOLT = define("ice_bolt", PlayerClass.WEAVER, 20, 40, 10, Effect.ELEMENTAL_BOLT, 6, 0);
    /** Hits the first enemy or solid block within 24 blocks; no lightning status or native lightning entity. */
    public static final SkillDefinition LIGHTNING_BOLT = define("lightning_bolt", PlayerClass.WEAVER, 20, 40, 10, Effect.LIGHTNING_BOLT, 6, 24);
    /** Rank 1 defaults; server datapacks tune these three skills through WeaverSkillData. */
    public static final SkillDefinition DISCHARGE = define("discharge", PlayerClass.WEAVER, 30, 240, 10, Effect.DISCHARGE, 0, 5);
    public static final SkillDefinition THREEFOLD_SYSTEM = define("threefold_system", PlayerClass.WEAVER, 10, 500, 0, Effect.THREEFOLD, 0, 0);
    public static final SkillDefinition TELEPORT = define("teleport", PlayerClass.WEAVER, 25, 200, 0, Effect.TELEPORT, 0, 8);
    /** Instant throw; root duration and contact behavior live in TrapKind/TrapEntity. */
    public static final SkillDefinition BEAR_TRAP = define("bear_trap", PlayerClass.STALKER, 20, 160, 0, Effect.TRAP, 0, 0);
    /** Eight-second, 3.5-block patch; one shared 50% slow regardless of overlapping owners. */
    public static final SkillDefinition FROST_TRAP = define("frost_trap", PlayerClass.STALKER, 30, 240, 0, Effect.TRAP, 0, 0);
    /** Six-second patch: three fire damage on entry, then at most once per twenty ticks per target. */
    public static final SkillDefinition FLAME_TRAP = define("flame_trap", PlayerClass.STALKER, 35, 240, 0, Effect.TRAP, 0, 0);
    /** One explosion, up to ten damage with 3.5-block falloff and a low outward shove. */
    public static final SkillDefinition BLAST_TRAP = define("blast_trap", PlayerClass.STALKER, 40, 300, 0, Effect.TRAP, 0, 0);
    public static final SkillDefinition DODGE_AURA = define("dodge_aura", PlayerClass.STALKER, 25, 400, 0, Effect.AURA, 0, 6);
    public static final SkillDefinition SWIFTNESS_AURA = define("swiftness_aura", PlayerClass.STALKER, 25, 400, 0, Effect.AURA, 0, 6);
    public static final SkillDefinition RIPPING_AURA = define("ripping_aura", PlayerClass.REAVER, 40, 400, 0, Effect.AURA, 0, 6);
    public static final SkillDefinition FORTITUDE_AURA = define("fortitude_aura", PlayerClass.REAVER, 40, 400, 0, Effect.AURA, 0, 6);
    public static final SkillDefinition RAGING_AURA = define("raging_aura", PlayerClass.REAVER, 40, 600, 0, Effect.AURA, 0, 6);
    public static final SkillDefinition ICY_VEINS = define("icy_veins", PlayerClass.WEAVER, 30, 600, 0, Effect.AURA, 0, 6);
    public static final SkillDefinition FLAME_CLOAK = define("flame_cloak", PlayerClass.WEAVER, 30, 400, 0, Effect.AURA, 0, 6);
    public static final SkillDefinition FREEZING_AURA = define("freezing_aura", PlayerClass.WEAVER, 40, 500, 0, Effect.AURA, 0, 6);
    public static final SkillDefinition SHOCK_AURA = define("shock_aura", PlayerClass.WEAVER, 30, 500, 0, Effect.AURA, 0, 6);
    public static final SkillDefinition IMMOLATION_AURA = define("immolation_aura", PlayerClass.WEAVER, 40, 600, 0, Effect.AURA, 0, 6);
    public static final SkillDefinition ENCHANTING_AURA = define("enchanting_aura", PlayerClass.WEAVER, 40, 500, 0, Effect.AURA, 0, 6);
    public static final List<SkillDefinition> ALL = List.of(PIERCING_SHOT, AIMED_SHOT, DISENGAGE, HUNTERS_PET, ARCANE_ARROW, MULTISHOT, CLEAVE, REND, ARCANE_BOLT,
            LUNGING_COMBO, RIP_AND_RUSH, BATTLE_CRY, RAGE_STORM, BLOOD_FEVER, BEAR_TRAP, FROST_TRAP, FLAME_TRAP, BLAST_TRAP, FIRE_BOLT, ICE_BOLT, LIGHTNING_BOLT,
            DODGE_AURA, SWIFTNESS_AURA, RIPPING_AURA, FORTITUDE_AURA, RAGING_AURA, ICY_VEINS, FLAME_CLOAK, FREEZING_AURA, SHOCK_AURA, IMMOLATION_AURA, ENCHANTING_AURA,
            DISCHARGE, THREEFOLD_SYSTEM, TELEPORT, ELEMENTAL_GOLEM, BARRAGE, FIRESTORM, WAR_BANNER);

    public SkillDefinition {
        allowedClasses = Set.copyOf(allowedClasses);
        if (id == null || effect == null || allowedClasses.isEmpty() || !Double.isFinite(cost) || cost < 0
                || cooldownTicks < 0 || castTicks < 0 || !Double.isFinite(power) || power < 0
                || !Double.isFinite(range) || range < 0) { throw new IllegalArgumentException("Invalid skill definition"); }
    }

    public Component displayName() { return Component.translatable("skill." + id.getNamespace() + "." + id.getPath()); }
    public Component description() { return Component.translatable("skill." + id.getNamespace() + "." + id.getPath() + ".description"); }
    public static Optional<SkillDefinition> find(Identifier id) { return ALL.stream().filter(s -> s.id.equals(id)).findFirst(); }

    private static SkillDefinition define(String name, PlayerClass type, double cost, int cooldown, int cast,
            Effect effect, double power, double range) {
        return new SkillDefinition(Identifier.fromNamespaceAndPath("covenant", name), Set.of(type), cost, cooldown, cast, effect, power, range);
    }
}
