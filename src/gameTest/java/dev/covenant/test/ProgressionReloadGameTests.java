package dev.covenant.test;

import com.google.gson.JsonParser;
import dev.covenant.rpg.*;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.progression.*;
import dev.covenant.rpg.skills.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;

/** Separate batch: real successful/failed server reloads must never rewrite online or offline progress. */
final class ProgressionReloadGameTests {
    static void run(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var originalPacks = List.copyOf(server.getPackRepository().getSelectedIds());
        var pack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("covenant-progression-test-" + UUID.randomUUID()).toAbsolutePath().normalize();
        var directory = pack.resolve("data/covenant/progression");
        try (var f = new ProgressionGameTests.Fixture(helper)) {
            try {
                var online = f.player(PlayerClass.STALKER); var offline = f.player(PlayerClass.STALKER);
                for (var player : List.of(online, offline)) {
                    PlayerProfiles.update(player, p -> p.withProgression(5, 37, true));
                    SkillLoadouts.apply(player, Map.of(), Map.of(SkillDefinition.PIERCING_SHOT.id(), 1));
                }
                var expected = PlayerProfiles.get(online); var skills = online.getData(RpgData.SKILL_LOADOUT);
                server.getPlayerList().saveAll(); var disk = server.getPlayerList().loadPlayerData(offline.nameAndId()).orElseThrow(); disk.remove("UUID");
                server.getPlayerList().remove(offline);
                com.google.gson.JsonObject json;
                try (var reader = server.getResourceManager().getResourceOrThrow(ProgressionData.FILE).openAsReader()) { json = JsonParser.parseReader(reader).getAsJsonObject(); }
                var curve = json.getAsJsonArray("character_requirements");
                for (int index = 0; index < curve.size(); index++) { curve.set(index, new com.google.gson.JsonPrimitive(curve.get(index).getAsLong() * 2)); }
                Files.createDirectories(directory);
                Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Disposable progression reload test\",\"min_format\":101,\"max_format\":101}}");
                Files.writeString(directory.resolve("balance.json"), json.toString());
                server.getPackRepository().reload();
                var selected = new ArrayList<>(originalPacks); selected.add("file/" + pack.getFileName());
                server.reloadResources(selected).join();
                var harder = ProgressionData.get(server);
                check(harder.required(ProgressionData.CHARACTER, 5) == curve.get(4).getAsLong(), "Reload adopts harder curve");
                check(PlayerProfiles.get(online).equals(expected) && online.getData(RpgData.SKILL_LOADOUT).equals(skills), "Harder curve preserves raw XP, earned level, claim and allocations");
                var loaded = f.player(null); var reporter = new ProblemReporter.Collector(); loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
                check(reporter.isEmpty() && PlayerProfiles.get(loaded).equals(expected), "Offline player gets same preservation policy");
                json.addProperty("hostile_reward", -1); Files.writeString(directory.resolve("balance.json"), json.toString());
                boolean failed = false;
                try { server.reloadResources(selected).join(); } catch (java.util.concurrent.CompletionException error) { failed = true; }
                check(failed && ProgressionData.get(server) == harder && PlayerProfiles.get(online).equals(expected), "Failed reload keeps the complete live snapshot and player state");
                json.addProperty("hostile_reward", 10); json.getAsJsonArray("dimensions").get(0).getAsJsonObject().getAsJsonObject("caps").addProperty("covenant:character", 3);
                Files.writeString(directory.resolve("balance.json"), json.toString()); server.reloadResources(selected).join();
                check(!Progression.awardCharacter(online, helper.getLevel(), 100) && PlayerProfiles.get(online).equals(expected), "Lowered dimension gate blocks new XP without taking progress");
                helper.succeed();
            } finally {
                server.reloadResources(originalPacks).join();
                // Only the named disposable pack's known files/directories are removed; never recurse through the world.
                Files.deleteIfExists(directory.resolve("balance.json")); Files.deleteIfExists(pack.resolve("pack.mcmeta"));
                Files.deleteIfExists(directory); Files.deleteIfExists(directory.getParent()); Files.deleteIfExists(pack.resolve("data")); Files.deleteIfExists(pack);
                server.getPackRepository().reload();
            }
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
}
