package dev.covenant.rpg.skills;

import dev.covenant.rpg.classes.PlayerClass;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Small code-backed catalog. Times are ticks, costs are resource units, damage is health points. */
public record SkillDefinition(Identifier id, Set<PlayerClass> allowedClasses, double cost,
        int cooldownTicks, int castTicks, Effect effect, double power, double range) {
    public enum Effect { BOW_SHOT, CLEAVE, REND, ARCANE_BOLT, TRAP }
    public static final SkillDefinition PIERCING_SHOT = define("piercing_shot", PlayerClass.STALKER, 30, 40, 0, Effect.BOW_SHOT, 1.5, 0);
    /** Prototype tuning: 40 Focus, 4 seconds cooldown, 1 second wind-up, double native arrow base damage. */
    public static final SkillDefinition AIMED_SHOT = define("aimed_shot", PlayerClass.STALKER, 40, 80, 20, Effect.BOW_SHOT, 2, 0);
    public static final SkillDefinition CLEAVE = define("cleave", PlayerClass.REAVER, 20, 60, 0, Effect.CLEAVE, 1, 3);
    /** Instant native-reach melee hit; 20 Rage, two-second cooldown. Bleed tuning lives in Bleeding. */
    public static final SkillDefinition REND = define("rend", PlayerClass.REAVER, 20, 40, 0, Effect.REND, 1, 0);
    public static final SkillDefinition ARCANE_BOLT = define("arcane_bolt", PlayerClass.WEAVER, 20, 40, 10, Effect.ARCANE_BOLT, 6, 0);
    /** Instant throw; root duration and contact behavior live in TrapKind/TrapEntity. */
    public static final SkillDefinition BEAR_TRAP = define("bear_trap", PlayerClass.STALKER, 20, 160, 0, Effect.TRAP, 4, 3);
    /** Eight-second, three-block patch; one shared 50% slow regardless of overlapping owners. */
    public static final SkillDefinition FROST_TRAP = define("frost_trap", PlayerClass.STALKER, 30, 240, 0, Effect.TRAP, 0, 3);
    /** Six-second patch: three fire damage on entry, then at most once per twenty ticks per target. */
    public static final SkillDefinition FLAME_TRAP = define("flame_trap", PlayerClass.STALKER, 35, 240, 0, Effect.TRAP, 3, 3);
    /** One explosion, up to ten damage with three-block falloff and a low outward shove. */
    public static final SkillDefinition BLAST_TRAP = define("blast_trap", PlayerClass.STALKER, 40, 300, 0, Effect.TRAP, 10, 3);
    public static final List<SkillDefinition> ALL = List.of(PIERCING_SHOT, AIMED_SHOT, CLEAVE, REND, ARCANE_BOLT,
            BEAR_TRAP, FROST_TRAP, FLAME_TRAP, BLAST_TRAP);

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
