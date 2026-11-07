package dev.covenant.test;

import com.google.gson.JsonParser;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.equipment.*;
import dev.covenant.rpg.progression.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import static dev.covenant.rpg.equipment.WorkbenchOperations.*;

/** Isolated native reload batch: costs change prospectively, snapshots and stale-preview protection survive. */
final class ItemizationReloadGameTests {
    private static void check(boolean value, String message) { if (!value) { throw new IllegalStateException(message); } }
    static void run(GameTestHelper h) {
        var server = h.getLevel().getServer(); var originalPacks = List.copyOf(server.getPackRepository().getSelectedIds());
        var pack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("covenant-itemization-test-" + UUID.randomUUID()).toAbsolutePath().normalize();
        var directory = pack.resolve("data/covenant/itemization");
        var pos = h.absolutePos(new BlockPos(3, 100, 2));
        try (var fixture = new ProgressionGameTests.Fixture(h)) {
            try {
                var p = fixture.player(null); var state = p.getData(RpgData.PROFESSIONS);
                p.setData(RpgData.PROFESSIONS, state.with(CRAFTING, new ProfessionState.Track(1, 0)));
                ProfessionAllocations.apply(p, CRAFTING, ProfessionData.snapshot(server).revision(), Map.of(), Map.of(BASIC, 1));
                p.getInventory().setItem(0, new ItemStack(Items.LAPIS_LAZULI, 32));
                h.getLevel().setBlock(pos, Workbench.BLOCK.get().defaultBlockState(), 3);
                var station = (WorkbenchBlockEntity) h.getLevel().getBlockEntity(pos); station.setItem(0, new ItemStack(Items.IRON_SWORD));
                check(craft(p, station, Operation.ADD, 0, 0, 0), "Initial operation uses shipped definitions");
                var itemBefore = station.getItem(0).copy(); var progressionBefore = p.getData(RpgData.PROFESSIONS);
                net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(p.connection.getConnection());
                var menu = new WorkbenchMenu(12, p.getInventory(), station); p.containerMenu = menu;
                menu.clickMenuButton(p, 0); menu.clickMenuButton(p, 0); // Reroll.
                int oldAction = 1000 + menu.view.token(); var oldData = ItemizationData.snapshot(server);
                com.google.gson.JsonObject json;
                try (var reader = server.getResourceManager().getResourceOrThrow(ItemizationData.FILE).openAsReader()) { json = JsonParser.parseReader(reader).getAsJsonObject(); }
                json.getAsJsonObject("settings").addProperty("potential_cost", 7);
                json.getAsJsonArray("rarities").get(0).getAsJsonObject().addProperty("potential", 100);
                json.getAsJsonArray("families").get(0).getAsJsonObject().getAsJsonArray("ranges").get(0).getAsJsonObject().addProperty("max", 2.0);
                var selector = JsonParser.parseString("{\"category\":\"covenant:main_hand\",\"items\":[\"minecraft:stick\"]}");
                json.getAsJsonArray("participation").add(selector);
                var debugFamily = json.getAsJsonArray("families").get(0).getAsJsonObject().deepCopy();
                debugFamily.addProperty("id", "covenant_test:stick_health");
                debugFamily.add("items", JsonParser.parseString("[\"minecraft:stick\"]"));
                json.getAsJsonArray("families").add(debugFamily);
                Files.createDirectories(directory);
                Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Disposable itemization reload test\",\"min_format\":101,\"max_format\":101}}");
                Files.writeString(directory.resolve("definitions.json"), json.toString());
                server.getPackRepository().reload(); var selected = new ArrayList<>(originalPacks); selected.add("file/" + pack.getFileName());
                server.reloadResources(selected).join();
                check(ItemizationData.snapshot(server).revision() != oldData.revision() && ItemStack.matches(itemBefore, station.getItem(0))
                        && p.getData(RpgData.PROFESSIONS).equals(progressionBefore), "Reload changes definitions without repricing earned progression or saved item rolls/Potential");
                check(!menu.clickMenuButton(p, oldAction) && ItemStack.matches(itemBefore, station.getItem(0)), "Old cost preview cannot spend against new definitions");
                check(menu.clickMenuButton(p, 1000 + menu.view.token()) && station.getItem(0).get(Workbench.QUALITY.get()).potential() == 8,
                        "Refreshed Reroll spends new seven-Potential cost from saved fifteen, without refilling to new hundred");
                station.setItem(0, new ItemStack(Items.STICK));
                check(craft(p, station, Operation.ADD, 0, 0, 0) && station.getItem(0).getMaxStackSize() == 1
                        && station.getItem(0).get(Workbench.QUALITY.get()).potential() == 93, "Explicit item participation opts stackable equipment in without a category tag");
                var source = p.createCommandSourceStack().withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS);
                var commands = server.getCommands().getDispatcher();
                check(commands.getCompletionSuggestions(commands.parse("covenant affix set ", source)).join().getList().stream()
                        .anyMatch(s -> s.getText().equals("covenant_test:stick_health")), "Command completion sees newly loaded families");
                var stick = new ItemStack(Items.STICK); p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, stick);
                commands.execute("covenant affix set covenant_test:stick_health covenant:common 1 max", source);
                var stickPage = AffixPages.get(stick).orElseThrow();
                check(stick.getMaxStackSize() == 1 && stick.get(Workbench.QUALITY.get()).potential() == 100
                        && stickPage.affixes().getFirst().modifiers().getFirst().amount() == 2.0,
                        "Commands use live item opt-in, families, roll bounds and Potential without category tags");
                commands.execute("covenant affix reset", source);
                check(!stick.has(RpgData.AFFIX_PAGE.get()) && !stick.has(Workbench.QUALITY.get()) && stick.getMaxStackSize() == 1,
                        "Reset preserves singleton override on originally stackable opted-in equipment");
                commands.execute("covenant affix set covenant_test:stick_health covenant:common 1 min", source);
                check(!AffixPages.get(stick).orElseThrow().identity().equals(stickPage.identity()), "Reset allows a genuinely fresh page identity");
                var sword = new ItemStack(Items.IRON_SWORD); p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword);
                boolean incompatible = false;
                try { commands.execute("covenant affix set covenant_test:stick_health covenant:common 1 max", source); }
                catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { incompatible = true; }
                check(incompatible && ItemStack.matches(sword, new ItemStack(Items.IRON_SWORD)), "Family allowlist rejects incompatible item before initialization");
                var updated = ItemizationData.snapshot(server); var itemAfter = station.getItem(0).copy();
                json.getAsJsonObject("settings").addProperty("potential_cost", -1); Files.writeString(directory.resolve("definitions.json"), json.toString());
                boolean failed = false; try { server.reloadResources(selected).join(); } catch (java.util.concurrent.CompletionException expected) { failed = true; }
                check(failed && ItemizationData.snapshot(server) == updated && ItemStack.matches(station.getItem(0), itemAfter), "Invalid reload retains the entire live catalogue and item state");
                h.succeed();
            } finally {
                server.reloadResources(originalPacks).join(); h.getLevel().removeBlock(pos, false);
                Files.deleteIfExists(directory.resolve("definitions.json")); Files.deleteIfExists(pack.resolve("pack.mcmeta"));
                Files.deleteIfExists(directory); Files.deleteIfExists(directory.getParent()); Files.deleteIfExists(pack.resolve("data")); Files.deleteIfExists(pack);
                server.getPackRepository().reload();
            }
        } catch (java.io.IOException | com.mojang.brigadier.exceptions.CommandSyntaxException error) { throw new IllegalStateException(error); }
    }
}
