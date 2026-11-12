package dev.covenant.test;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.progression.*;
import io.netty.buffer.Unpooled;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasurePieces;
import net.minecraft.world.level.storage.*;
import static dev.covenant.test.ProfessionGameTests.check;

/** Real server registries, attachment lifecycles, native loaded-chunk observations and resource reloads. */
final class ExplorationGameTests {
    private static Identifier mc(String path) { return Identifier.withDefaultNamespace(path); }
    private static ProfessionState.Track track(ServerPlayer player) { return player.getData(RpgData.PROFESSIONS).tracks().get(ProgressionData.EXPLORATION); }
    private static ExplorationData.Snapshot snapshot(GameTestHelper h) { return ExplorationData.snapshot(h.getLevel().getServer()); }
    private static void discover(ServerPlayer p, Identifier dimension, Identifier biome, Set<Identifier> structures) {
        p.setData(RpgData.EXPLORATION, p.getData(RpgData.EXPLORATION).discover(dimension, biome, structures));
        Exploration.evaluate(p, ExplorationData.snapshot(p.level().getServer()));
    }
    private static void completePool(ServerPlayer p, Identifier dimension, ExplorationData.Snapshot data) {
        var biomes = new HashSet<Identifier>(); var structures = new HashSet<Identifier>();
        for (var goal : data.goals()) {
            if (!goal.dimension().equals(dimension)) { continue; }
            for (var members : goal.targets().values()) {
                (goal.kind() == ExplorationData.Kind.BIOME ? biomes : structures).addAll(members);
            }
        }
        for (var biome : biomes) { discover(p, dimension, biome, structures); }
    }
    static void catalogue(GameTestHelper h) {
        var data = snapshot(h);
        check(data.goals().size() == 22, "All 22 authored milestones load against the pinned registries");
        check(data.goals().stream().filter(g -> g.pool().equals("Overworld")).count() == 10
                && data.goals().stream().filter(g -> g.pool().equals("Nether")).count() == 8
                && data.goals().stream().filter(g -> g.pool().equals("End")).count() == 4, "Default pools distribute 10/8/4 rewards");
        var goals = data.goals().stream().filter(g -> g.id().getPath().startsWith("overworld/biomes_")).toList();
        int total = goals.getLast().targets().size();
        for (int index = 0; index < 5; index++) {
            int percent = new int[] {10, 25, 50, 75, 100}[index];
            check(goals.get(index).required() == (total * percent + 99) / 100, "Percentage thresholds round upward against the full configured set");
        }
        check(goals.getLast().required() == total && total > 30, "The final Overworld goal requires every eligible biome");
        try (var reader = h.getLevel().getServer().getResourceManager().getResourceOrThrow(ExplorationData.FILE).openAsReader()) {
            var source = JsonParser.parseReader(reader).getAsJsonObject();
            for (String change : List.of("duplicate", "unknown", "empty", "fraction", "mixed")) {
                var json = source.deepCopy(); var first = json.getAsJsonArray("milestones").get(0).getAsJsonObject();
                switch (change) {
                    case "duplicate" -> json.getAsJsonArray("milestones").add(first.deepCopy());
                    case "unknown" -> first.add("targets", JsonParser.parseString("[\"missing:biome\"]"));
                    case "empty" -> first.add("targets", new JsonArray());
                    case "fraction" -> first.addProperty("percent", 10.5);
                    case "mixed" -> first.addProperty("count", 3);
                }
                boolean rejected = false;
                try { ExplorationData.parse(json, h.getLevel().registryAccess()); } catch (RuntimeException expected) { rejected = true; }
                check(rejected, "Reject invalid milestone definition: " + change);
            }
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        h.succeed();
    }
    static void progression(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var p = f.player(null); var building = p.getData(RpgData.PROFESSIONS).tracks().get(ProgressionData.BUILDING);
            var data = snapshot(h);
            completePool(p, mc("overworld"), data);
            check(track(p).level() == 10 && track(p).points() == 10, "All Overworld rewards provide ten levels and points");
            completePool(p, mc("the_nether"), data);
            check(track(p).level() == 18 && track(p).experience() == 0, "All Nether rewards add eight levels, never XP");
            discover(p, mc("the_end"), mc("the_end"), Set.of());
            check(track(p).level() == 19, "First End entry provides the nineteenth reward");
            discover(p, mc("the_end"), mc("end_highlands"), Set.of());
            check(track(p).level() == 20 && track(p).points() == 20, "Outer islands provide the twentieth reward");
            completePool(p, mc("the_end"), data);
            check(track(p).level() == 20 && p.getData(RpgData.EXPLORATION).claimed().size() == 22, "All remaining completions are recorded without exceeding the cap");
            var before = p.getData(RpgData.PROFESSIONS); Exploration.evaluate(p, data);
            check(p.getData(RpgData.PROFESSIONS).equals(before), "Repeated evaluation cannot repay claims");
            check(!Progression.awardProfession(p, h.getLevel(), ProgressionData.EXPLORATION, Long.MAX_VALUE)
                    && p.getData(RpgData.PROFESSIONS).equals(before), "Trusted/operator XP cannot advance Exploration");
            check(p.getData(RpgData.PROFESSIONS).tracks().get(ProgressionData.BUILDING).equals(building), "Exploration never manufactures Building XP");
            h.succeed();
        }
    }
    static void collections(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var p = f.player(null);
            discover(p, mc("the_nether"), mc("nether_wastes"), Set.of(mc("mansion")));
            check(track(p).level() == 1, "A structure seen in a different dimension cannot complete its Overworld goal");
            discover(p, mc("overworld"), mc("the_void"), Set.of(mc("mansion"), mc("ancient_city"), mc("monument")));
            check(track(p).level() == 4, "Major Overworld destinations each provide a complete level");
            for (var variant : List.of("plains", "desert", "savanna", "snowy")) {
                discover(p, mc("overworld"), mc("the_void"), Set.of(mc("village_" + variant)));
            }
            check(track(p).level() == 4, "Partial village collection does not award a level");
            discover(p, mc("overworld"), mc("the_void"), Set.of(mc("village_taiga")));
            check(track(p).level() == 5, "Every configured village variant collectively earns one level");
            discover(p, mc("overworld"), mc("the_void"), Set.of(mc("shipwreck"), mc("shipwreck_beached")));
            var landmarks = p.getData(RpgData.EXPLORATION_VIEW).entries().stream().filter(e -> e.id().getPath().equals("overworld/landmarks")).findFirst().orElseThrow();
            check(landmarks.progress() == 1 && landmarks.required() == 6, "Normal and beached shipwrecks count as one family in the owner view");
            discover(p, mc("overworld"), mc("the_void"), Set.of(mc("mineshaft"), mc("desert_pyramid"), mc("jungle_pyramid"), mc("swamp_hut"), mc("pillager_outpost")));
            check(track(p).level() == 6, "Six distinct common families earn one additional level");
            var wire = Unpooled.buffer();
            try {
                var view = p.getData(RpgData.EXPLORATION_VIEW); ExplorationView.STREAM_CODEC.encode(wire, view);
                check(ExplorationView.STREAM_CODEC.decode(wire).equals(view), "Checklist presentation round trips without raw discovery history");
            } finally { wire.release(); }
            var data = snapshot(h); var biomeGoal = data.goals().getFirst();
            var explorer = f.player(null); var iterator = biomeGoal.targets().keySet().iterator();
            for (int n = 0; n < biomeGoal.required() - 1; n++) { discover(explorer, mc("overworld"), iterator.next(), Set.of()); }
            check(track(explorer).level() == 0, "Partial biome coverage does not pay early");
            var last = iterator.next(); discover(explorer, mc("overworld"), last, Set.of());
            check(track(explorer).level() == 1, "First biome percentage threshold pays exactly once");
            discover(explorer, mc("overworld"), last, Set.of());
            check(track(explorer).level() == 1, "Revisiting the same biome cannot repay a threshold");
            h.succeed();
        }
    }
    static void observations(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var p = f.player(null); var chunk = h.getLevel().getChunkAt(p.blockPosition());
            var starts = new HashMap<>(chunk.getAllStarts()); var references = new HashMap<>(chunk.getAllReferences());
            var structure = h.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(mc("mansion"));
            try {
                var piece = new BuriedTreasurePieces.BuriedTreasurePiece(p.blockPosition());
                chunk.setStartForStructure(structure, new StructureStart(structure, chunk.getPos(), 0, new PiecesContainer(List.of(piece))));
                chunk.addReferenceForStructure(structure, chunk.getPos().pack());
                p.setGameMode(net.minecraft.world.level.GameType.CREATIVE); Exploration.tick(p);
                check(p.getData(RpgData.EXPLORATION).equals(ExplorationState.EMPTY), "Creative observation grants no discovery history");
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); Exploration.tick(p);
                check(p.getData(RpgData.EXPLORATION).in(mc("overworld")).structures().contains(mc("mansion")) && track(p).level() == 1,
                        "Loaded native structure references and bounds recognize arrival immediately");
                var saved = p.getData(RpgData.EXPLORATION); Exploration.tick(p);
                check(p.getData(RpgData.EXPLORATION).equals(saved), "A stationary repeat does not alter saved state");
                var nether = h.getLevel().getServer().getLevel(Level.NETHER);
                var destination = new net.minecraft.core.BlockPos(0, 180, 0);
                nether.getChunkAt(destination); // Fixture-only readiness; production never requests chunks.
                p.teleportTo(nether, 0.5, 180, 0.5, Set.of(), 0, 0, false); p.connection.markClientLoaded();
                Exploration.tick(p);
                check(p.getData(RpgData.EXPLORATION).dimensions().containsKey(mc("the_nether")), "A teleport/airborne arrival records the destination dimension");
            } finally { chunk.setAllStarts(starts); chunk.setAllReferences(references); }
            h.succeed();
        }
    }
    static void storage(GameTestHelper h) {
        try (var f = new ProgressionGameTests.Fixture(h)) {
            var p = f.player(null);
            discover(p, mc("overworld"), mc("plains"), Set.of(mc("mansion")));
            var expected = p.getData(RpgData.EXPLORATION); var progress = p.getData(RpgData.PROFESSIONS);
            var encoded = ExplorationState.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();
            check(ExplorationState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(expected), "Discovery/claim codec retains permanent history");
            var clone = f.player(null); clone.restoreFrom(p, false);
            check(clone.getData(RpgData.EXPLORATION).equals(expected) && clone.getData(RpgData.PROFESSIONS).equals(progress), "Native death copy retains discoveries, claims and levels");
            var reporter = new ProblemReporter.Collector(); var output = TagValueOutput.createWithContext(reporter, h.getLevel().registryAccess());
            p.saveWithoutId(output); var disk = output.buildResult(); disk.remove("UUID");
            var loaded = f.player(null); loaded.load(TagValueInput.create(reporter, h.getLevel().registryAccess(), disk));
            check(reporter.isEmpty() && loaded.getData(RpgData.EXPLORATION).equals(expected), "Native saved player data restores discoveries on reconnect");
            Exploration.evaluate(loaded, snapshot(h));
            check(loaded.getData(RpgData.PROFESSIONS).equals(progress), "Restoring a saved claim does not repay it");
            var mining = ProfessionData.id("mining");
            loaded.setData(RpgData.PROFESSIONS, progress.with(mining, new ProfessionState.Track(5, 0,
                    Map.of(ProfessionData.VEIN_MINING, new ProfessionState.Allocation(1, 1)))));
            check(ProfessionAllocations.respec(loaded) && loaded.getData(RpgData.EXPLORATION).equals(expected)
                    && track(loaded).equals(progress.tracks().get(ProgressionData.EXPLORATION)), "All-profession respec preserves discovery history and direct point entitlement");
            h.succeed();
        }
    }
    static void reload(GameTestHelper h) {
        var server = h.getLevel().getServer(); var originalPacks = List.copyOf(server.getPackRepository().getSelectedIds());
        var pack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("covenant-exploration-test-" + UUID.randomUUID());
        var directory = pack.resolve("data/covenant/progression");
        try (var f = new ProgressionGameTests.Fixture(h)) {
            try {
                var p = f.player(null); discover(p, mc("overworld"), mc("plains"), Set.of(mc("mansion")));
                var oldLevel = track(p).level(); var initialClaims = p.getData(RpgData.EXPLORATION).claimed();
                JsonObject json;
                try (var reader = server.getResourceManager().getResourceOrThrow(ExplorationData.FILE).openAsReader()) { json = JsonParser.parseReader(reader).getAsJsonObject(); }
                var newGoal = JsonParser.parseString("{\"id\":\"example:known_plains\",\"title\":\"Known plains\",\"pool\":\"Custom\",\"dimension\":\"minecraft:overworld\",\"kind\":\"biome\",\"targets\":[\"minecraft:plains\"],\"count\":1}").getAsJsonObject();
                json.getAsJsonArray("milestones").add(newGoal);
                Files.createDirectories(directory);
                Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Exploration reload test\",\"min_format\":101,\"max_format\":101}}");
                Files.writeString(directory.resolve("exploration.json"), json.toString());
                server.getPackRepository().reload(); var selected = new ArrayList<>(originalPacks); selected.add("file/" + pack.getFileName());
                server.reloadResources(selected).join();
                check(track(p).level() == oldLevel + 1 && p.getData(RpgData.EXPLORATION).claimed().contains(Identifier.parse("example:known_plains")),
                        "A real successful resource reload pays a new goal from recorded discoveries without revisiting");
                newGoal.add("targets", JsonParser.parseString("[\"minecraft:desert\"]"));
                Files.writeString(directory.resolve("exploration.json"), json.toString()); server.reloadResources(selected).join();
                check(track(p).level() == oldLevel + 1 && p.getData(RpgData.EXPLORATION).claimed().containsAll(initialClaims), "Changed claimed requirements neither revoke nor repay earned levels");
                var retained = snapshot(h); var state = p.getData(RpgData.EXPLORATION);
                newGoal.add("targets", JsonParser.parseString("[\"missing:biome\"]")); Files.writeString(directory.resolve("exploration.json"), json.toString());
                boolean rejected = false; try { server.reloadResources(selected).join(); } catch (RuntimeException expected) { rejected = true; }
                check(rejected && snapshot(h).equals(retained) && p.getData(RpgData.EXPLORATION).equals(state), "Invalid target rejects reload and preserves the live catalogue and claims");
                h.succeed();
            } finally {
                server.reloadResources(originalPacks).join();
                Files.deleteIfExists(directory.resolve("exploration.json")); Files.deleteIfExists(pack.resolve("pack.mcmeta"));
                Files.deleteIfExists(directory); Files.deleteIfExists(directory.getParent()); Files.deleteIfExists(pack.resolve("data")); Files.deleteIfExists(pack);
                server.getPackRepository().reload();
            }
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
}
