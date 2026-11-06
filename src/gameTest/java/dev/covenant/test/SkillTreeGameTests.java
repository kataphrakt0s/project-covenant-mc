package dev.covenant.test;

import com.mojang.authlib.GameProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLoadout;
import dev.covenant.rpg.skills.SkillLoadouts;
import dev.covenant.rpg.skills.SkillTree;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;

/** Full roster allocation is exercised through the same server boundary as GUI packet handlers. */
final class SkillTreeGameTests {
    static void run(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var players = new java.util.ArrayList<ServerPlayer>();
        var player = makePlayer(helper);
        players.add(player);
        try {
            check(SkillTree.ALL.size() == 62 && SkillTree.ALL.stream().map(SkillTree.Node::id).distinct().count() == 62,
                    "All 62 brainstorm nodes have unique stable IDs");
            check(SkillDefinition.ALL.size() == 25 && SkillTree.ALL.stream().filter(SkillTree.Node::implemented).count() == 25,
                    "Twenty-five executable skills, including eleven auras and two combos");
            var expectedLinks = Set.of("piercing_shot>aimed_shot", "bear_trap>frost_trap", "poison_shot>snaring_shot",
                    "flame_trap>blast_trap", "arcane_arrow>meteor_arrow", "cleave>rage_storm", "battle_cry>enduring_cry",
                    "lunging_combo>rip_and_rush", "arcane_bolt>arcane_barrage", "icy_veins>freezing_aura", "flame_cloak>immolation_aura");
            var actualLinks = SkillTree.ALL.stream().filter(n -> n.prerequisite().isPresent())
                    .map(n -> n.prerequisite().orElseThrow().getPath() + ">" + n.id().getPath()).collect(Collectors.toSet());
            check(actualLinks.equals(expectedLinks), "Exact brainstorm prerequisites");
            check(SkillTree.ALL.stream().filter(n -> n.exclusive().isPresent()).map(n -> n.id().getPath()).collect(Collectors.toSet())
                    .equals(Set.of("ripping_aura", "fortitude_aura")), "Only the pictured aura pair is exclusive");
            check(key(SkillLoadouts.learn(player, id("ballista"), true), "commands.covenant.skill.class"), "No-class allocation rejected");
            PlayerProfiles.selectClass(player, PlayerClass.STALKER);
            var resources = player.getData(RpgData.SKILLS);
            for (var node : SkillTree.ALL) {
                check(SkillTree.LEVELS.contains(node.diagramLevel()) && node.column() >= 0 && node.column() < 8, "Valid diagram position");
                if (node.exclusive().isPresent()) {
                    var other = SkillTree.find(node.exclusive().orElseThrow()).orElseThrow();
                    check(other.exclusive().orElseThrow().equals(node.id()) && other.playerClass() == node.playerClass(), "Symmetric class-local exclusions");
                }
                if (node.prerequisite().isEmpty()) { continue; }
                var prerequisite = SkillTree.find(node.prerequisite().orElseThrow()).orElseThrow();
                check(prerequisite.playerClass() == node.playerClass() && prerequisite.diagramLevel() < node.diagramLevel(),
                        "Prerequisite is in the same tree and a lower tier");
                PlayerProfiles.switchClass(player, node.playerClass());
                player.setData(RpgData.SKILL_LOADOUT, SkillLoadout.EMPTY);
                check(key(SkillLoadouts.learn(player, node.id(), true), "skills.covenant.requires"), "Cannot skip any prerequisite");
                check(player.getData(RpgData.SKILL_LOADOUT).equals(SkillLoadout.EMPTY), "Rejected learning leaves state unchanged");
                SkillLoadouts.learn(player, prerequisite.id(), true);
                SkillLoadouts.learn(player, node.id(), true);
                check(player.getData(RpgData.SKILL_LOADOUT).learned().containsAll(List.of(node.id(), prerequisite.id())),
                        "Prerequisite and dependent remain learned together");
                var unrelated = SkillTree.forClass(node.playerClass()).stream().filter(n -> n.prerequisite().isEmpty()
                        && n.exclusive().isEmpty() && !n.id().equals(prerequisite.id())).findFirst().orElseThrow();
                SkillLoadouts.learn(player, unrelated.id(), true);
                SkillLoadouts.bind(player, 0, node.id());
                SkillLoadouts.bind(player, 1, prerequisite.id());
                SkillLoadouts.bind(player, 2, unrelated.id());
                SkillLoadouts.learn(player, prerequisite.id(), false);
                var state = player.getData(RpgData.SKILL_LOADOUT);
                check(state.learned().equals(Set.of(unrelated.id())) && state.bindings().equals(java.util.Map.of(2, unrelated.id())),
                        "Removing each prerequisite clears dependent learning/bindings and preserves unrelated nodes");
                check(key(SkillLoadouts.learn(player, node.id(), true), "skills.covenant.requires"), "Dependent cannot be relearned without prerequisite");
            }
            PlayerProfiles.switchClass(player, PlayerClass.REAVER);
            for (var first : List.of("ripping_aura", "fortitude_aura")) {
                var second = first.equals("ripping_aura") ? "fortitude_aura" : "ripping_aura";
                player.setData(RpgData.SKILL_LOADOUT, SkillLoadout.EMPTY);
                SkillLoadouts.learn(player, id(first), true);
                SkillLoadouts.bind(player, 0, id(first));
                var before = player.getData(RpgData.SKILL_LOADOUT);
                check(key(SkillLoadouts.learn(player, id(second), true), "skills.covenant.exclusive"), "Aura exclusivity works in both directions");
                check(player.getData(RpgData.SKILL_LOADOUT).equals(before), "Excluded choice does not replace learned aura");
                SkillLoadouts.learn(player, id(first), false);
                SkillLoadouts.learn(player, id(second), true);
                check(player.getData(RpgData.SKILL_LOADOUT).learned().equals(Set.of(id(second)))
                        && player.getData(RpgData.SKILL_LOADOUT).bindings().isEmpty(), "Free switch after deallocation clears old binding");
            }
            player.setData(RpgData.SKILL_LOADOUT, SkillLoadout.EMPTY);
            // Allocate all three trees in tier order, retaining inactive learning as with existing class switches.
            for (var type : PlayerClass.all()) {
                PlayerProfiles.switchClass(player, type);
                for (var node : SkillTree.forClass(type)) {
                    if (!node.id().equals(id("fortitude_aura"))) { SkillLoadouts.learn(player, node.id(), true); }
                }
            }
            var full = player.getData(RpgData.SKILL_LOADOUT);
            check(full.learned().size() == 61, "All compatible nodes learn at level one without a point budget");
            for (int slot = 0; slot < 8; slot++) { SkillLoadouts.bind(player, slot, id("scorching_ray")); }
            full = player.getData(RpgData.SKILL_LOADOUT);
            var beforeActivation = player.getData(RpgData.SKILLS);
            check(key(SkillLoadouts.activateSlot(player, 0), "skills.covenant.planned"), "Planned skill can bind but cannot execute");
            check(player.getData(RpgData.SKILLS).equals(beforeActivation), "Placeholder activation spends no resource and creates no cooldown");
            check(key(SkillLoadouts.learn(player, id("ballista"), true), "commands.covenant.skill.class"), "Wrong-class planned learning rejected");
            check(key(SkillLoadouts.bind(player, 0, id("ballista")), "commands.covenant.skill.class"), "Inactive planned skill cannot bind");
            player.setGameMode(GameType.SPECTATOR);
            check(key(SkillLoadouts.learn(player, id("scorching_ray"), false), "commands.covenant.skill.unavailable"), "Spectator cannot edit tree");
            check(player.getData(RpgData.SKILL_LOADOUT).equals(full), "Rejected spectator edit preserves loadout");
            player.setGameMode(GameType.SURVIVAL);
            var encoded = SkillLoadout.CODEC.encodeStart(NbtOps.INSTANCE, full).getOrThrow();
            check(SkillLoadout.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow().equals(full), "Full tree NBT codec retains planned nodes");
            server.getPlayerList().saveAll();
            var disk = server.getPlayerList().loadPlayerData(player.nameAndId()).orElseThrow();
            disk.remove("UUID");
            var loaded = makePlayer(helper);
            players.add(loaded);
            var reporter = new ProblemReporter.Collector();
            loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
            check(reporter.isEmpty() && loaded.getData(RpgData.SKILL_LOADOUT).equals(full), "Actual player-file save/load retains full trees and bindings");
            for (boolean endReturn : new boolean[]{false, true}) {
                var clone = makePlayer(helper);
                players.add(clone);
                clone.restoreFrom(loaded, endReturn);
                check(clone.getData(RpgData.SKILL_LOADOUT).equals(full), "Death/End copying retains full tree");
            }
            // Unlearning remains available for inactive classes and still honors dependent removal.
            SkillLoadouts.learn(player, id("bear_trap"), false);
            check(!player.getData(RpgData.SKILL_LOADOUT).learned().contains(id("frost_trap")), "Inactive class deallocation removes dependents");
            check(resources.balances().get(PlayerClass.STALKER.id()).equals(player.getData(RpgData.SKILLS).balances().get(PlayerClass.STALKER.id())),
                    "Tree operations preserve the original class resource balance");
            helper.succeed();
        } finally { players.forEach(server.getPlayerList()::remove); }
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        // The deprecated GameTest mock overrides gameMode() to Creative, even after setGameMode.
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "tree-test-player"), false);
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("covenant", path); }
    private static boolean key(Component value, String expected) {
        return value.getContents() instanceof TranslatableContents text && text.getKey().equals(expected);
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
    }
}
