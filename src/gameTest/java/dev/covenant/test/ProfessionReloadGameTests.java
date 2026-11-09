package dev.covenant.test;

import com.google.gson.JsonParser;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.progression.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;

/** Isolated reload batch: tuning changes future purchases without repricing saved allocations. */
final class ProfessionReloadGameTests {
    static void run(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var originalPacks = List.copyOf(server.getPackRepository().getSelectedIds());
        var pack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("covenant-profession-test-" + UUID.randomUUID()).toAbsolutePath().normalize();
        var directory = pack.resolve("data/covenant/progression");
        try (var f = new ProgressionGameTests.Fixture(helper)) {
            try {
                var player = f.player(null); var offline = f.player(null); var harvester = f.player(null);
                var cropPos = helper.absolutePos(new net.minecraft.core.BlockPos(4, 270, 4));
                var matureWheat = net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.CropBlock.AGE, 7);
                helper.getLevel().setBlock(cropPos.below(), net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState(), 2);
                helper.getLevel().setBlock(cropPos, matureWheat, 2); harvester.gameMode.destroyBlock(cropPos);
                ProfessionGameTests.check(harvester.getData(RpgData.PROFESSIONS).tracks().get(ProfessionData.id("farming")).experience() == 5,
                        "Natural crop activity starts with the shipped reward");
                var miningId = ProfessionData.id("mining");
                harvester.setData(RpgData.PROFESSIONS, harvester.getData(RpgData.PROFESSIONS).with(miningId, new ProfessionState.Track(2, 0)));
                ProfessionAllocations.apply(harvester, miningId, ProfessionData.snapshot(server).revision(), Map.of(), Map.of(ProfessionData.VEIN_MINING, 1));
                var gatheringState = harvester.getData(RpgData.PROFESSIONS);
                for (var target : List.of(player, offline)) {
                    var state = target.getData(RpgData.PROFESSIONS);
                    target.setData(RpgData.PROFESSIONS, state.with(ProfessionGameTests.LOGGING,
                            new ProfessionState.Track(17, 31)));
                    ProfessionAllocations.apply(target, ProfessionGameTests.LOGGING, ProfessionData.snapshot(server).revision(),
                            Map.of(), Map.of(ProfessionGameTests.OFFENSIVE, 1));
                }
                var initial = player.getData(RpgData.PROFESSIONS); var oldRevision = ProfessionData.snapshot(server).revision();
                server.getPlayerList().saveAll(); var disk = server.getPlayerList().loadPlayerData(offline.nameAndId()).orElseThrow(); disk.remove("UUID");
                server.getPlayerList().remove(offline);
                com.google.gson.JsonObject json;
                try (var reader = server.getResourceManager().getResourceOrThrow(ProfessionData.FILE).openAsReader()) { json = JsonParser.parseReader(reader).getAsJsonObject(); }
                com.google.gson.JsonObject mastery = null;
                for (var element : json.getAsJsonArray("abilities")) {
                    if (element.getAsJsonObject().get("id").getAsString().equals(ProfessionGameTests.OFFENSIVE.toString())) { mastery = element.getAsJsonObject(); }
                }
                if (mastery == null) { throw new IllegalStateException("Missing mastery definition"); }
                mastery.getAsJsonArray("ranks").get(0).getAsJsonObject().addProperty("cost", 3);
                mastery.getAsJsonArray("ranks").get(0).getAsJsonObject().addProperty("bonus", .07);
                mastery.getAsJsonArray("ranks").get(1).getAsJsonObject().addProperty("cost", 4);
                for (var ability : json.getAsJsonArray("abilities")) {
                    if (ability.getAsJsonObject().get("id").getAsString().equals(ProfessionData.VEIN_MINING.toString())) {
                        ability.getAsJsonObject().getAsJsonArray("ranks").get(0).getAsJsonObject().addProperty("blocks", 2);
                    }
                }
                json.getAsJsonObject("activity_rewards").addProperty("covenant:farming/mature_crop", 13);
                Files.createDirectories(directory);
                Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Disposable profession reload test\",\"min_format\":101,\"max_format\":101}}");
                Files.writeString(directory.resolve("professions.json"), json.toString());
                server.getPackRepository().reload();
                var selected = new ArrayList<>(originalPacks); selected.add("file/" + pack.getFileName());
                server.reloadResources(selected).join();
                var updated = ProfessionData.snapshot(server); ProfessionView.refresh(player);
                ProfessionGameTests.check(updated.revision() != oldRevision && player.getData(RpgData.PROFESSION_VIEW).revision() == updated.revision(), "Reload replaces owner presentation revision");
                ProfessionGameTests.check(player.getData(RpgData.PROFESSIONS).equals(initial), "Changing costs/bonuses never reprices or mutates existing purchases");
                ProfessionGameTests.check(harvester.getData(RpgData.PROFESSIONS).tracks().get(ProfessionData.id("farming")).experience() == 5,
                        "Reload preserves previously earned natural activity XP");
                helper.getLevel().setBlock(cropPos, matureWheat, 2); harvester.gameMode.destroyBlock(cropPos);
                ProfessionGameTests.check(harvester.getData(RpgData.PROFESSIONS).tracks().get(ProfessionData.id("farming")).experience() == 18,
                        "Future native harvests use the reloaded reward");
                var orePositions = java.util.stream.IntStream.range(0, 3).mapToObj(index -> helper.absolutePos(new BlockPos(4 + index, 3, 4))).toList();
                for (var pos : orePositions) { helper.getLevel().setBlock(pos, net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState(), 2); }
                harvester.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
                harvester.gameMode.destroyBlock(orePositions.getFirst());
                ProfessionGameTests.check(orePositions.stream().filter(pos -> helper.getLevel().getBlockState(pos).isAir()).count() == 2
                        && harvester.getData(RpgData.PROFESSIONS).tracks().get(miningId).allocations().equals(gatheringState.tracks().get(miningId).allocations()),
                        "Future native batches use reloaded limits while retaining purchased ranks/paid costs");
                var loaded = f.player(null); var reporter = new ProblemReporter.Collector();
                loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
                ProfessionGameTests.check(reporter.isEmpty() && loaded.getData(RpgData.PROFESSIONS).equals(initial), "Offline paid reservations survive tuning reload");
                ProfessionAllocations.apply(player, ProfessionGameTests.LOGGING, oldRevision,
                        initial.tracks().get(ProfessionGameTests.LOGGING).allocations(), Map.of(ProfessionGameTests.OFFENSIVE, 2));
                ProfessionGameTests.check(player.getData(RpgData.PROFESSIONS).equals(initial), "Stale price previews cannot purchase against new tuning");
                ProfessionAllocations.apply(player, ProfessionGameTests.LOGGING, updated.revision(),
                        initial.tracks().get(ProfessionGameTests.LOGGING).allocations(), Map.of(ProfessionGameTests.OFFENSIVE, 2));
                ProfessionGameTests.check(player.getData(RpgData.PROFESSIONS).tracks().get(ProfessionGameTests.LOGGING).allocations()
                        .get(ProfessionGameTests.OFFENSIVE).paidPoints() == 5, "Future rank pays four new points while retaining the old one-point price");
                var paid = player.getData(RpgData.PROFESSIONS);
                mastery.getAsJsonArray("ranks").get(0).getAsJsonObject().addProperty("cost", -1);
                Files.writeString(directory.resolve("professions.json"), json.toString());
                boolean failed = false;
                try { server.reloadResources(selected).join(); } catch (java.util.concurrent.CompletionException expected) { failed = true; }
                ProfessionGameTests.check(failed && ProfessionData.snapshot(server) == updated && player.getData(RpgData.PROFESSIONS).equals(paid), "Invalid reload retains the live catalogue and all paid reservations");
                mastery.getAsJsonArray("ranks").get(0).getAsJsonObject().addProperty("cost", 3);
                for (int index = json.getAsJsonArray("abilities").size() - 1; index >= 0; index--) {
                    var abilityId = json.getAsJsonArray("abilities").get(index).getAsJsonObject().get("id").getAsString();
                    if (abilityId.equals(ProfessionGameTests.OFFENSIVE.toString()) || abilityId.equals(ProfessionData.VEIN_MINING.toString())) {
                        json.getAsJsonArray("abilities").remove(index);
                    }
                }
                Files.writeString(directory.resolve("professions.json"), json.toString()); server.reloadResources(selected).join();
                ProfessionGameTests.check(player.getData(RpgData.PROFESSIONS).equals(paid), "Removed definition preserves unknown allocations and spent points");
                for (var pos : orePositions) { helper.getLevel().setBlock(pos, net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState(), 2); }
                harvester.gameMode.destroyBlock(orePositions.getFirst());
                ProfessionGameTests.check(orePositions.stream().filter(pos -> helper.getLevel().getBlockState(pos).isAir()).count() == 1
                        && harvester.getData(RpgData.PROFESSIONS).tracks().get(miningId).rank(ProfessionData.VEIN_MINING) == 1,
                        "Removing a gathering definition disables expansion without deleting its paid reservation");
                for (var pos : orePositions) { helper.getLevel().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2); }
                ProfessionGameTests.check(ProfessionAllocations.respec(player)
                        && player.getData(RpgData.PROFESSIONS).tracks().get(ProfessionGameTests.LOGGING).points() == 17, "Removed content can still be refunded by all-profession respec");
                helper.succeed();
            } finally {
                server.reloadResources(originalPacks).join();
                // Delete only this test's named files and known empty directories; never recurse through a world.
                Files.deleteIfExists(directory.resolve("professions.json")); Files.deleteIfExists(pack.resolve("pack.mcmeta"));
                Files.deleteIfExists(directory); Files.deleteIfExists(directory.getParent()); Files.deleteIfExists(pack.resolve("data")); Files.deleteIfExists(pack);
                server.getPackRepository().reload();
            }
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
}
