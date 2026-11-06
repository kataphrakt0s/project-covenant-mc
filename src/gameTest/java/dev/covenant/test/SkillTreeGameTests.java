package dev.covenant.test;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.progression.Progression;
import dev.covenant.rpg.skills.*;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;

/** Purchases exercise the same atomic boundary as Apply, including failures and stale snapshots. */
final class SkillTreeGameTests {
    static void run(GameTestHelper helper) {
        try (var f = new ProgressionGameTests.Fixture(helper)) {
            var player = f.player(null);
            check(SkillTree.ALL.size() == 62 && SkillTree.ALL.stream().map(SkillTree.Node::id).distinct().count() == 62, "Complete unique roster");
            check(SkillTree.ALL.stream().filter(SkillTree.Node::implemented).count() == SkillDefinition.ALL.size(), "Playable nodes resolve to skills");
            for (var node : SkillTree.ALL) {
                node.prerequisite().ifPresent(id -> {
                    var parent = SkillTree.find(id).orElseThrow();
                    check(parent.playerClass() == node.playerClass() && parent.diagramLevel() < node.diagramLevel(), "Ordered class-local prerequisite");
                });
                node.exclusive().ifPresent(id -> check(SkillTree.find(id).orElseThrow().exclusive().orElseThrow().equals(node.id()), "Symmetric exclusion"));
                check(node.availableMaximum() <= node.plannedMaximum(), "Ranks within plan");
            }
            var shot = id("piercing_shot"); var aimed = id("aimed_shot");
            SkillLoadouts.apply(player, Map.of(), Map.of(shot, 1)); check(state(player).ranks().isEmpty(), "Classless purchase rejected");
            PlayerProfiles.selectClass(player, PlayerClass.STALKER);
            check(SkillLoadouts.availablePoints(player) == 1, "Level one starts with one point");
            SkillLoadouts.apply(player, Map.of(), Map.of(shot, 2)); check(state(player).ranks().isEmpty(), "Whole batch budget");
            SkillLoadouts.apply(player, Map.of(), Map.of(id("bear_trap"), 1)); check(state(player).ranks().isEmpty(), "Level gate");
            SkillLoadouts.apply(player, Map.of(), Map.of(id("ballista"), 1)); check(state(player).ranks().isEmpty(), "Unimplemented unavailable");
            SkillLoadouts.apply(player, Map.of(), Map.of(shot, 1)); check(state(player).rank(shot) == 1, "Rank one committed");
            SkillLoadouts.bind(player, 0, shot);
            SkillLoadouts.apply(player, Map.of(), Map.of(shot, 2)); check(state(player).rank(shot) == 1, "Stale snapshot rejected");
            PlayerProfiles.update(player, p -> Progression.atLevel(p, 60));
            var old = state(player);
            SkillLoadouts.apply(player, old.ranks(), Map.of(shot, 8, aimed, 8));
            check(state(player).spent() == 16 && state(player).bindings().equals(old.bindings()), "Batch prerequisites and binding preserved");
            old = state(player);
            SkillLoadouts.apply(player, old.ranks(), Map.of(shot, 9, aimed, 8)); check(state(player).equals(old), "Rank limit");
            SkillLoadouts.apply(player, old.ranks(), Map.of(shot, 7, aimed, 8)); check(state(player).equals(old), "No committed refunds");
            SkillLoadouts.learn(player, shot, false); check(state(player).equals(old), "Legacy request cannot refund");
            check(SkillLoadouts.respec(player), "Out-of-combat reset");
            SkillLoadouts.apply(player, Map.of(), Map.of(aimed, 1)); check(state(player).ranks().isEmpty(), "Prerequisite enforced");
            SkillLoadouts.apply(player, Map.of(), Map.of(shot, 1, aimed, 1));
            PlayerProfiles.update(player, p -> Progression.atLevel(p, 3));
            SkillLoadouts.apply(player, state(player).ranks(), Map.of(shot, 1, aimed, 2));
            check(state(player).rank(aimed) == 2, "Unlock level gates initial investment only");
            check(SkillLoadouts.respec(player), "Reset after testing lowered level");
            PlayerProfiles.update(player, p -> Progression.atLevel(p, 60));
            SkillLoadouts.apply(player, Map.of(), Map.of(id("bear_trap"), 2)); check(state(player).ranks().isEmpty(), "Deferred upgrades capped at one");
            PlayerProfiles.switchClass(player, PlayerClass.WEAVER);
            var nine = new java.util.HashMap<Identifier, Integer>();
            SkillTree.forClass(PlayerClass.WEAVER).stream().filter(SkillTree.Node::implemented).limit(9).forEach(node -> nine.put(node.id(), 1));
            check(nine.size() == 9, "Fixture contains nine playable nodes");
            SkillLoadouts.apply(player, Map.of(), nine); check(state(player).ranks().isEmpty(), "Eight distinct skills maximum");
            PlayerProfiles.switchClass(player, PlayerClass.REAVER);
            SkillLoadouts.apply(player, Map.of(), Map.of(id("ripping_aura"), 1, id("fortitude_aura"), 1));
            check(state(player).ranks().isEmpty(), "Mutual exclusion across batch");
            SkillLoadouts.apply(player, Map.of(), Map.of(id("rip_and_rush"), 1, id("lunging_combo"), 1));
            check(state(player).spent() == 2, "Playable combo branch");
            var before = state(player);
            var desired = Map.of(id("cleave"), 1, id("rip_and_rush"), 1, id("lunging_combo"), 1);
            player.setGameMode(GameType.SPECTATOR);
            SkillLoadouts.apply(player, before.ranks(), desired);
            check(state(player).equals(before) && !SkillLoadouts.respec(player), "Spectator rejected");
            player.setGameMode(GameType.SURVIVAL);
            ClassResources.markCombat(player);
            SkillLoadouts.apply(player, before.ranks(), desired);
            check(state(player).equals(before) && !SkillLoadouts.respec(player), "Combat blocks Apply and reset");
            helper.succeed();
        }
    }
    private static SkillLoadout state(net.minecraft.server.level.ServerPlayer player) { return player.getData(RpgData.SKILL_LOADOUT); }
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("covenant", name); }
    private static void check(boolean value, String message) { if (!value) { throw new IllegalStateException(message); } }
}
