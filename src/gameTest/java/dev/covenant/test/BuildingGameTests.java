package dev.covenant.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.progression.*;
import io.netty.buffer.Unpooled;
import java.nio.file.*;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import static dev.covenant.test.ProfessionGameTests.check;

/** Meta-progression contracts exercise the production award boundary and native player persistence. */
final class BuildingGameTests {
    private static final Identifier BUILDING = ProgressionData.BUILDING;
    private static final Identifier MINING = ProfessionData.id("mining");
    private static final Identifier LOGGING = ProfessionData.id("logging");

    private static ProfessionState.Track building(ServerPlayer player) { return player.getData(RpgData.PROFESSIONS).tracks().get(BUILDING); }
    private static void set(ServerPlayer player, Identifier id, ProfessionState.Track track) {
        player.setData(RpgData.PROFESSIONS, player.getData(RpgData.PROFESSIONS).with(id, track));
    }

    static void awards(GameTestHelper helper) {
        try (var f = new ProgressionGameTests.Fixture(helper)) {
            var player = f.player(null);
            for (var id : ProgressionData.XP_PROFESSIONS) {
                if (id.equals(BUILDING)) { continue; }
                Progression.awardProfession(player, helper.getLevel(), id, 100);
                check(player.getData(RpgData.PROFESSIONS).tracks().get(id).level() == 1, "Every source retains its complete reward");
            }
            check(building(player).level() == 2 && building(player).experience() == 25 && building(player).points() == 2,
                    "Five 100-XP sources grant 325 Building XP and its own points");
            var split = f.player(null); var batch = f.player(null);
            for (int i = 0; i < 20; i++) { Progression.awardProfession(split, helper.getLevel(), MINING, 1); }
            Progression.awardProfession(batch, helper.getLevel(), LOGGING, 20);
            check(building(split).equals(building(batch)) && building(split).experience() == 13 && building(split).experienceFraction() == 0,
                    "Twenty one-XP rewards equal one twenty-XP reward with no rounding loss");
            Progression.awardProfession(batch, helper.getLevel(), BUILDING, 100);
            check(building(batch).level() == 1 && building(batch).experience() == 13, "Direct operator Building XP never feeds itself");
            var balance = ProgressionData.get(helper.getLevel().getServer());
            for (var dimension : helper.getLevel().getServer().getAllLevels()) {
                check(balance.gate(dimension, BUILDING) == 20, "Building defaults to Level 20 in every dimension");
            }
            var mastered = f.player(null);
            Progression.awardProfession(mastered, helper.getLevel(), MINING, Long.MAX_VALUE);
            Progression.awardProfession(mastered, helper.getLevel(), LOGGING, Long.MAX_VALUE);
            check(building(mastered).level() == 20 && building(mastered).points() == 20 && building(mastered).experience() == 0
                    && building(mastered).experienceFraction() == 0, "Large source awards stop at twenty Building points with no banked overflow");
            check(!PlayerProfiles.get(player).created(), "Building progression never requires or selects a combat class");
            helper.succeed();
        }
    }

    static void caps(GameTestHelper helper) {
        try (var f = new ProgressionGameTests.Fixture(helper)) {
            var player = f.player(null);
            set(player, MINING, new ProfessionState.Track(19, 1990));
            Progression.awardProfession(player, helper.getLevel(), MINING, 100);
            check(building(player).experience() == 6 && building(player).experienceFraction() == 50,
                    "Only ten accepted source XP contribute 6.5 Building XP; overflow is excluded");
            var cappedSource = player.getData(RpgData.PROFESSIONS);
            check(!Progression.awardProfession(player, helper.getLevel(), MINING, Long.MAX_VALUE)
                    && player.getData(RpgData.PROFESSIONS).equals(cappedSource), "A capped source cannot contribute again");
            set(player, BUILDING, new ProfessionState.Track(19, 1999, Map.of(), 95));
            Progression.awardProfession(player, helper.getLevel(), LOGGING, 4);
            check(building(player).level() == 20 && building(player).experience() == 0 && building(player).experienceFraction() == 0,
                    "Crossing Building's cap discards both whole and fractional overflow");
            var full = building(player);
            Progression.awardProfession(player, helper.getLevel(), LOGGING, 100);
            check(building(player).equals(full) && player.getData(RpgData.PROFESSIONS).tracks().get(LOGGING).level() == 1,
                    "Building's cap never reduces source progression");
            set(player, BUILDING, new ProfessionState.Track(19, 1999, Map.of(), 95));
            Progression.awardProfession(player, helper.getLevel(), BUILDING, 1);
            check(building(player).level() == 20 && building(player).experienceFraction() == 0,
                    "Direct operator Building XP also discards fractional overflow at its cap");
            helper.succeed();
        }
    }

    static void storage(GameTestHelper helper) {
        try (var f = new ProgressionGameTests.Fixture(helper)) {
            var player = f.player(null);
            Progression.awardProfession(player, helper.getLevel(), MINING, 1);
            check(building(player).experience() == 0 && building(player).experienceFraction() == 65, "Sub-XP credit is retained");
            set(player, LOGGING, new ProfessionState.Track(5, 12,
                    Map.of(ProfessionData.OFFENSIVE_MASTERY, new ProfessionState.Allocation(1, 1))));
            var expected = player.getData(RpgData.PROFESSIONS);
            var json = ProfessionState.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();
            check(ProfessionState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(expected), "Saved codec retains all five tracks and fractional credit");
            var wire = Unpooled.buffer();
            try {
                ProfessionState.STREAM_CODEC.encode(wire, expected);
                check(ProfessionState.STREAM_CODEC.decode(wire).equals(expected), "Owner wire round trip retains fractional credit");
                wire.clear();
                ProfessionView.STREAM_CODEC.encode(wire, player.getData(RpgData.PROFESSION_VIEW));
                var view = ProfessionView.STREAM_CODEC.decode(wire);
                check(view.tracks().size() == 7 && view.tracks().get(BUILDING).maximum() == 20,
                        "Owner presentation includes Building's requirements and caps");
            } finally { wire.release(); }
            Progression.refreshView(player);
            check(player.getData(RpgData.PROFESSIONS).equals(expected), "Refreshing owner presentation grants no XP");
            var clone = f.player(null); clone.restoreFrom(player, false);
            check(clone.getData(RpgData.PROFESSIONS).equals(expected), "Death copy retains Building credit and point entitlement");
            var server = helper.getLevel().getServer();
            server.getPlayerList().saveAll();
            var disk = server.getPlayerList().loadPlayerData(player.nameAndId()).orElseThrow(); disk.remove("UUID");
            var loaded = f.player(null); var reporter = new ProblemReporter.Collector();
            loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
            check(reporter.isEmpty() && loaded.getData(RpgData.PROFESSIONS).equals(expected), "Native save/load retains fractional credit without catch-up");
            check(ProfessionAllocations.respec(loaded) && building(loaded).equals(building(player)), "Shared respec preserves Building credit");
            for (int i = 0; i < 19; i++) { Progression.awardProfession(loaded, helper.getLevel(), MINING, 1); }
            check(building(loaded).experience() == 13 && building(loaded).experienceFraction() == 0, "Restored fractional credit completes the original exact XP total");
            var beforeTravel = loaded.getData(RpgData.PROFESSIONS);
            var nether = server.getLevel(Level.NETHER);
            loaded.teleportTo(nether, 0, 150, 0, Set.of(), 0, 0, false); loaded.connection.markClientLoaded();
            check(loaded.getData(RpgData.PROFESSIONS).equals(beforeTravel), "Native dimension transfer preserves all profession progress");
            helper.succeed();
        }
    }

    static void planned(GameTestHelper helper) {
        try (var f = new ProgressionGameTests.Fixture(helper)) {
            var player = f.player(null); set(player, BUILDING, new ProfessionState.Track(20, 0));
            var snapshot = ProfessionData.snapshot(helper.getLevel().getServer());
            var nodes = player.getData(RpgData.PROFESSION_VIEW).nodes(BUILDING);
            check(nodes.size() == 3 && nodes.stream().allMatch(n -> !n.implemented() && n.ranks().isEmpty()),
                    "Building exposes three planned entries without invented ranks, costs or gameplay");
            var before = player.getData(RpgData.PROFESSIONS);
            for (var node : nodes) {
                ProfessionAllocations.apply(player, BUILDING, snapshot.revision(), Map.of(), Map.of(node.id(), 1));
            }
            check(player.getData(RpgData.PROFESSIONS).equals(before) && building(player).points() == 20,
                    "Server rejects forged planned-skill purchases without spending points");
            try {
                var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
                helper.getLevel().getServer().getCommands().getDispatcher().execute("covenant profession level covenant:mining 20", source);
                check(building(player).equals(before.tracks().get(BUILDING)), "Operator level edits do not create Building XP");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) { throw new IllegalStateException(error); }
            helper.succeed();
        }
    }

    static void activity(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 180)) {
            var player = f.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            check(player.gameMode.destroyBlock(f.set(4, 0, 4, Blocks.COAL_ORE.defaultBlockState())), "Native coal harvest succeeds");
            check(building(player).experience() == 6 && building(player).experienceFraction() == 50,
                    "Confirmed natural harvesting contributes once through the shared XP boundary");
            var before = building(player);
            var placed = f.placeOre(player, 4, 0, 4);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            check(player.gameMode.destroyBlock(placed) && building(player).equals(before), "Placed-ore exclusion also blocks Building XP");
            helper.succeed();
        }
    }

    static void reload(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var originalPacks = List.copyOf(server.getPackRepository().getSelectedIds());
        var pack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("covenant-building-test-" + UUID.randomUUID()).toAbsolutePath().normalize();
        var directory = pack.resolve("data/covenant/progression");
        try (var f = new ProgressionGameTests.Fixture(helper)) {
            try {
                var player = f.player(null);
                set(player, MINING, new ProfessionState.Track(0, 95));
                set(player, BUILDING, new ProfessionState.Track(0, 99, Map.of(), 65));
                var initial = player.getData(RpgData.PROFESSIONS);
                com.google.gson.JsonObject json;
                try (var reader = server.getResourceManager().getResourceOrThrow(ProgressionData.FILE).openAsReader()) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }
                var caps = json.getAsJsonArray("dimensions").get(0).getAsJsonObject().getAsJsonObject("caps");
                caps.addProperty(MINING.toString(), 1); caps.addProperty(BUILDING.toString(), 1);
                Files.createDirectories(directory);
                Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Disposable Building reload test\",\"min_format\":101,\"max_format\":101}}");
                Files.writeString(directory.resolve("balance.json"), json.toString());
                server.getPackRepository().reload();
                var selected = new ArrayList<>(originalPacks); selected.add("file/" + pack.getFileName());
                server.reloadResources(selected).join();
                check(player.getData(RpgData.PROFESSIONS).equals(initial), "Balance reload never rewrites Building XP or fractional credit");
                Progression.awardProfession(player, helper.getLevel(), MINING, 100);
                check(player.getData(RpgData.PROFESSIONS).tracks().get(MINING).level() == 1 && building(player).level() == 1
                        && building(player).experience() == 0 && building(player).experienceFraction() == 0,
                        "Independent source and Building dimension caps discard their own overflow");
                Progression.awardProfession(player, server.getLevel(Level.NETHER), MINING, 1);
                check(building(player).experienceFraction() == 65, "Building uses the source dimension, allowing the next award under the Nether fallback cap");
                var retained = building(player);
                Progression.awardProfession(player, helper.getLevel(), LOGGING, 100);
                check(building(player).equals(retained) && player.getData(RpgData.PROFESSIONS).tracks().get(LOGGING).level() == 1,
                        "Lower Building gate preserves old credit and does not block another source");

                json.getAsJsonArray("profession_requirements").set(0, new com.google.gson.JsonPrimitive(50));
                Files.writeString(directory.resolve("balance.json"), json.toString()); server.reloadResources(selected).join();
                set(player, MINING, new ProfessionState.Track(0, 100));
                set(player, BUILDING, new ProfessionState.Track(0, 7, Map.of(), 80));
                retained = building(player);
                Progression.awardProfession(player, helper.getLevel(), MINING, 1);
                check(building(player).equals(retained), "Normalizing old source XP to its cap creates no new Building credit");

                caps.addProperty(MINING.toString(), 20); caps.addProperty(BUILDING.toString(), 20);
                var curve = json.getAsJsonArray("profession_requirements");
                for (int index = 0; index < curve.size(); index++) { curve.set(index, new com.google.gson.JsonPrimitive(Long.MAX_VALUE)); }
                Files.writeString(directory.resolve("balance.json"), json.toString()); server.reloadResources(selected).join();
                player.setData(RpgData.PROFESSIONS, ProfessionState.INITIAL);
                Progression.awardProfession(player, helper.getLevel(), MINING, Long.MAX_VALUE);
                long whole = Long.MAX_VALUE / 100 * 65 + Long.MAX_VALUE % 100 * 65 / 100;
                check(building(player).level() == 0 && building(player).experience() == whole && building(player).experienceFraction() == 55,
                        "Conversion remains exact and overflow-safe for a fully accepted Long.MAX_VALUE source award");
                helper.succeed();
            } finally {
                server.reloadResources(originalPacks).join();
                Files.deleteIfExists(directory.resolve("balance.json")); Files.deleteIfExists(pack.resolve("pack.mcmeta"));
                Files.deleteIfExists(directory); Files.deleteIfExists(directory.getParent()); Files.deleteIfExists(pack.resolve("data")); Files.deleteIfExists(pack);
                server.getPackRepository().reload();
            }
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
}
