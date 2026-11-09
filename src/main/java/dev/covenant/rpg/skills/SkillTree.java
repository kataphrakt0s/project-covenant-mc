package dev.covenant.rpg.skills;

import dev.covenant.rpg.classes.PlayerClass;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Brainstorm roster and allocation rules, separate from executable skill definitions. */
public final class SkillTree {
    private SkillTree() {}

    /** Unlock levels and prototype rank availability, shared by client and server. */
    public record Node(Identifier id, PlayerClass playerClass, int diagramLevel, int column,
            Optional<Identifier> prerequisite, Optional<Identifier> exclusive) {
        public Component displayName() { return Component.translatable("skill.covenant." + id.getPath()); }
        public Component description() { return Component.translatable("skill.covenant." + id.getPath() + ".description"); }
        public boolean implemented() { return SkillDefinition.find(id).isPresent(); }
        public int plannedMaximum() { return !implemented() ? 0 : id.getPath().equals("lunging_combo") || id.getPath().equals("rip_and_rush") ? 12 : 8; }
        public int availableMaximum() {
            if (!implemented()) { return 0; }
            return id.getPath().equals("piercing_shot") || id.getPath().equals("aimed_shot") ? 8 : 1;
        }
    }

    public static final List<Integer> LEVELS = List.of(30, 25, 20, 15, 10, 5, 3, 1);
    public static final List<Node> ALL = List.of(
            node("piercing_shot", PlayerClass.STALKER, 1, 0),
            node("disengage", PlayerClass.STALKER, 3, 2),
            node("bear_trap", PlayerClass.STALKER, 3, 7),
            node("aimed_shot", PlayerClass.STALKER, 5, 0, "piercing_shot"),
            node("hunters_pet", PlayerClass.STALKER, 5, 3),
            node("barrage", PlayerClass.STALKER, 5, 5),
            node("snipe", PlayerClass.STALKER, 10, 0),
            node("dodge_aura", PlayerClass.STALKER, 10, 3),
            node("frost_trap", PlayerClass.STALKER, 10, 7, "bear_trap"),
            node("arcane_arrow", PlayerClass.STALKER, 15, 1),
            node("poison_shot", PlayerClass.STALKER, 15, 5),
            node("multishot", PlayerClass.STALKER, 20, 3),
            node("flame_trap", PlayerClass.STALKER, 20, 7),
            node("swiftness_aura", PlayerClass.STALKER, 25, 3),
            node("snaring_shot", PlayerClass.STALKER, 25, 5, "poison_shot"),
            node("blast_trap", PlayerClass.STALKER, 25, 7, "flame_trap"),
            node("meteor_arrow", PlayerClass.STALKER, 30, 1, "arcane_arrow"),
            node("ballista", PlayerClass.STALKER, 30, 5),
            node("volley", PlayerClass.STALKER, 30, 7),
            node("cleave", PlayerClass.REAVER, 1, 0),
            node("rend", PlayerClass.REAVER, 3, 1),
            node("battle_cry", PlayerClass.REAVER, 3, 7),
            node("rage_storm", PlayerClass.REAVER, 5, 0, "cleave"),
            node("execute", PlayerClass.REAVER, 5, 3),
            node("blood_fever", PlayerClass.REAVER, 5, 5, "rend"),
            node("leap", PlayerClass.REAVER, 10, 0),
            node("ripping_aura", PlayerClass.REAVER, 10, 3, null, "fortitude_aura"),
            node("fortitude_aura", PlayerClass.REAVER, 10, 5, null, "ripping_aura"),
            node("lunging_combo", PlayerClass.REAVER, 15, 1),
            node("bloody_cloak", PlayerClass.REAVER, 15, 5),
            node("overpower", PlayerClass.REAVER, 20, 0),
            node("consume", PlayerClass.REAVER, 20, 3),
            node("enduring_cry", PlayerClass.REAVER, 20, 7, "battle_cry"),
            node("skull_bash", PlayerClass.REAVER, 25, 0),
            node("raging_aura", PlayerClass.REAVER, 25, 3),
            node("war_banner", PlayerClass.REAVER, 25, 7),
            node("rip_and_rush", PlayerClass.REAVER, 30, 1, "lunging_combo"),
            node("guillotine", PlayerClass.REAVER, 30, 3),
            node("demon_mode", PlayerClass.REAVER, 30, 5),
            node("arcane_bolt", PlayerClass.WEAVER, 1, 0),
            node("lightning_bolt", PlayerClass.WEAVER, 3, 3),
            node("mage_armour", PlayerClass.WEAVER, 3, 7),
            node("arcane_barrage", PlayerClass.WEAVER, 5, 0, "arcane_bolt"),
            node("ice_bolt", PlayerClass.WEAVER, 5, 1),
            node("discharge", PlayerClass.WEAVER, 5, 3),
            node("fire_bolt", PlayerClass.WEAVER, 5, 5),
            node("icy_veins", PlayerClass.WEAVER, 10, 1),
            node("flame_cloak", PlayerClass.WEAVER, 10, 5),
            node("teleport", PlayerClass.WEAVER, 10, 7),
            node("freezing_aura", PlayerClass.WEAVER, 15, 1, "icy_veins"),
            node("shock_aura", PlayerClass.WEAVER, 15, 3),
            node("immolation_aura", PlayerClass.WEAVER, 15, 5, "flame_cloak"),
            node("threefold_system", PlayerClass.WEAVER, 15, 7),
            node("ice_tomb", PlayerClass.WEAVER, 20, 1),
            node("firestorm", PlayerClass.WEAVER, 20, 5),
            node("weavers_web", PlayerClass.WEAVER, 20, 7),
            node("enchanting_aura", PlayerClass.WEAVER, 25, 0),
            node("beacon", PlayerClass.WEAVER, 25, 3),
            node("elemental_golem", PlayerClass.WEAVER, 25, 7),
            node("arcane_explosion", PlayerClass.WEAVER, 30, 0),
            node("blizzard", PlayerClass.WEAVER, 30, 1),
            node("scorching_ray", PlayerClass.WEAVER, 30, 5));

    public static Optional<Node> find(Identifier id) { return ALL.stream().filter(n -> n.id().equals(id)).findFirst(); }
    public static List<Node> forClass(PlayerClass type) { return ALL.stream().filter(n -> n.playerClass() == type).toList(); }

    /** Shared by server validation and client availability/tooltip presentation. */
    public static Optional<Component> blocked(Node node, SkillLoadout state) {
        if (node.prerequisite().filter(id -> !state.learned().contains(id)).isPresent()) {
            return Optional.of(Component.translatable("skills.covenant.requires", find(node.prerequisite().orElseThrow()).orElseThrow().displayName()));
        }
        if (node.exclusive().filter(state.learned()::contains).isPresent()) {
            return Optional.of(Component.translatable("skills.covenant.exclusive", find(node.exclusive().orElseThrow()).orElseThrow().displayName()));
        }
        return Optional.empty();
    }

    /** Trusted removal helper for fixtures/integrations; player refunds require a respec item. */
    public static SkillLoadout deallocate(SkillLoadout state, Identifier id) {
        var removed = new HashSet<Identifier>();
        removed.add(id);
        boolean changed;
        do {
            changed = false;
            for (var node : ALL) {
                if (state.learned().contains(node.id()) && node.prerequisite().filter(removed::contains).isPresent()) {
                    changed |= removed.add(node.id());
                }
            }
        } while (changed);
        var ranks = new java.util.HashMap<>(state.ranks());
        ranks.keySet().removeAll(removed);
        var bindings = new java.util.HashMap<>(state.bindings());
        bindings.values().removeIf(removed::contains);
        return new SkillLoadout(ranks, bindings);
    }

    private static Node node(String id, PlayerClass type, int level, int column) { return node(id, type, level, column, null); }
    private static Node node(String id, PlayerClass type, int level, int column, String prerequisite) {
        return node(id, type, level, column, prerequisite, null);
    }
    private static Node node(String id, PlayerClass type, int level, int column, String prerequisite, String exclusive) {
        return new Node(id(id), type, level, column, Optional.ofNullable(prerequisite).map(SkillTree::id),
                Optional.ofNullable(exclusive).map(SkillTree::id));
    }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("covenant", path); }
}
