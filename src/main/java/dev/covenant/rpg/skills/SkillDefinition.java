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
    public enum Effect { BOW_SHOT, CLEAVE, ARCANE_BOLT }
    public static final SkillDefinition PIERCING_SHOT = define("piercing_shot", PlayerClass.STALKER, 30, 40, 0, Effect.BOW_SHOT, 1.5, 0);
    public static final SkillDefinition CLEAVE = define("cleave", PlayerClass.REAVER, 20, 60, 0, Effect.CLEAVE, 1, 3);
    public static final SkillDefinition ARCANE_BOLT = define("arcane_bolt", PlayerClass.WEAVER, 20, 40, 10, Effect.ARCANE_BOLT, 6, 0);
    public static final List<SkillDefinition> ALL = List.of(PIERCING_SHOT, CLEAVE, ARCANE_BOLT);

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
