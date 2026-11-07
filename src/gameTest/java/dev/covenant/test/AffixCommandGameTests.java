package dev.covenant.test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.covenant.rpg.CharacterProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.equipment.*;
import dev.covenant.rpg.stats.RpgStats;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

/** Real dispatcher, Survival inventories, equipment updates, and native persistence on a dedicated server. */
final class AffixCommandGameTests {
    private AffixCommandGameTests() {}

    static void run(GameTestHelper helper) {
        try (var fixture = new ProgressionGameTests.Fixture(helper)) {
            var player = fixture.player(null);
            var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
            editing(helper, player, source);
            workbench(helper, player, source);
            transfers(helper, player, source);
            components(helper, player, source);
            helper.succeed();
        } catch (CommandSyntaxException exception) {
            throw new IllegalStateException("Valid affix command failed", exception);
        }
    }

    private static void editing(GameTestHelper h, ServerPlayer player, CommandSourceStack source) throws CommandSyntaxException {
        var server = h.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        var data = ItemizationData.get(server);
        var profession = player.getData(RpgData.PROFESSIONS);
        for (String suffix : List.of("set covenant:health covenant:legendary 5 max", "rarity covenant:rare", "potential 0",
                "potential full", "remove covenant:health", "clear", "reset", "component covenant:health covenant:rare")) {
            reject(dispatcher, source.withPermission(PermissionSet.NO_PERMISSIONS), suffix);
            reject(dispatcher, server.createCommandSourceStack(), suffix);
            if (!suffix.startsWith("component")) { reject(dispatcher, source, suffix); }
        }
        var root = dispatcher.getRoot().getChild("covenant").getChild("affix");
        check(root.getChild("inspect") == null && root.getChild("apply") == null, "Retired commands are removed");
        check(suggestions(dispatcher, source, "set ").size() == data.families().size(), "Family completion uses live definitions");
        check(suggestions(dispatcher, source, "set covenant:health ").size() == data.rarities().size(), "Rarity completion uses live definitions");
        check(suggestions(dispatcher, source, "set covenant:health covenant:common 1 ").containsAll(List.of("min", "max", "random")), "Roll modes complete");
        check(!suggestions(dispatcher, source, "component covenant:health ").contains("covenant:common"), "No Common component completion");
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIRT));
        rejectUnchanged(dispatcher, source, player, "set covenant:health covenant:common 1 max");
        rejectUnchanged(dispatcher, source, player, "rarity covenant:rare");
        var stacked = new ItemStack(Items.IRON_SWORD, 2);
        player.setItemSlot(EquipmentSlot.MAINHAND, stacked);
        rejectUnchanged(dispatcher, source, player, "set covenant:health covenant:common 1 max");
        check(stacked.getCount() == 2, "Stacked rejection consumes nothing");
        var sword = new ItemStack(Items.IRON_SWORD);
        sword.setDamageValue(7);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Affix command test"));
        sword.enchant(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 2);
        var underlying = sword.copy();
        player.setItemSlot(EquipmentSlot.MAINHAND, sword);
        player.getInventory().setItem(9, new ItemStack(Items.LAPIS_LAZULI, 9));
        check(execute(dispatcher, source, "clear") == 0 && execute(dispatcher, source, "reset") == 0, "Uninitialized cleanup is harmless");
        for (String suffix : List.of("set covenant:missing covenant:common 1 max", "set covenant:health covenant:missing 1 max",
                "set covenant:health covenant:common 2 max", "set covenant:health covenant:common 0 max",
                "set covenant:health covenant:common 1 nope", "set covenant:health covenant:common 2147483647 max",
                "rarity covenant:missing", "potential 1", "potential full", "remove covenant:health")) {
            rejectUnchanged(dispatcher, source, player, suffix);
        }
        execute(dispatcher, source, "set covenant:health covenant:legendary 5 max");
        var identity = AffixPages.get(sword).orElseThrow().identity();
        var legendary = data.rarity(id("legendary"));
        var health = data.family(id("health"));
        near(bonus(sword, "health"), health.ranges().get(4).max() * legendary.magnitude(), "Exact maximum includes rarity multiplier");
        check(sword.get(Workbench.QUALITY.get()).equals(new EquipmentQuality(legendary.id(), legendary.potential())), "Set initializes full Potential and equipment rarity atomically");
        check(AffixPages.get(sword).orElseThrow().category().equals(EquipmentCategory.MAIN_HAND), "Category inferred from participation");
        execute(dispatcher, source, "potential 0");
        execute(dispatcher, source, "set covenant:health covenant:common 1 min");
        near(bonus(sword, "health"), health.ranges().getFirst().min(), "Upsert replaces roll with exact minimum");
        execute(dispatcher, source, "set covenant:attack_damage covenant:common 1 min");
        var other = AffixPages.get(sword).orElseThrow().affixes().get(1);
        player.getRandom().setSeed(426);
        var expected = AffixedLoot.roll(health, legendary, 5, RandomSource.create(426));
        execute(dispatcher, source, "set covenant:health covenant:legendary 5 random");
        var page = AffixPages.get(sword).orElseThrow();
        check(page.identity().equals(identity) && page.affixes().size() == 2 && page.affixes().getFirst().equals(expected)
                && page.affixes().get(1).equals(other), "Upsert preserves identity/order/other affixes and uses normal random sampling");
        check(sword.get(Workbench.QUALITY.get()).equals(new EquipmentQuality(legendary.id(), 0)), "Free edits work at zero Potential without refill");
        execute(dispatcher, source, "set covenant:armor covenant:common 1 max");
        rejectUnchanged(dispatcher, source, player, "set covenant:attack_speed covenant:common 1 max");
        execute(dispatcher, source, "set covenant:movement_speed covenant:common 1 min");
        execute(dispatcher, source, "set covenant:resource_cost covenant:legendary 5 max");
        var reduction = data.family(id("resource_cost"));
        near(bonus(sword, "resource_cost"), -reduction.ranges().get(4).max() * legendary.magnitude(), "Max reduction is strongest, with negative sign");
        execute(dispatcher, source, "set covenant:resource_cost covenant:legendary 5 min");
        near(bonus(sword, "resource_cost"), -reduction.ranges().get(4).min() * legendary.magnitude(), "Min reduction is weakest");
        execute(dispatcher, source, "set covenant:cooldown covenant:common 1 min");
        rejectUnchanged(dispatcher, source, player, "set covenant:cast_time covenant:common 1 min");
        check(suggestions(dispatcher, source, "remove ").size() == 6, "Removal completion contains held families only");
        execute(dispatcher, source, "remove covenant:armor");
        check(AffixPages.get(sword).orElseThrow().affixes().size() == 5 && sword.get(Workbench.QUALITY.get()).potential() == 0,
                "Removal is free and preserves other affixes and Potential");
        rejectUnchanged(dispatcher, source, player, "remove covenant:armor");
        rejectUnchanged(dispatcher, source, player, "rarity covenant:rare"); // Affix rarity exceeds Rare.
        execute(dispatcher, source, "set covenant:health covenant:common 5 max");
        execute(dispatcher, source, "remove covenant:resource_cost");
        rejectUnchanged(dispatcher, source, player, "rarity covenant:rare"); // Numerical tier exceeds Rare.
        execute(dispatcher, source, "set covenant:health covenant:common 1 min");
        rejectUnchanged(dispatcher, source, player, "rarity covenant:common"); // Two prefixes and two suffixes.
        execute(dispatcher, source, "clear");
        check(AffixPages.get(sword).orElseThrow().identity().equals(identity) && AffixPages.get(sword).orElseThrow().affixes().isEmpty()
                && sword.get(Workbench.QUALITY.get()).potential() == 0, "Clear preserves page identity, equipment rarity and Potential");
        execute(dispatcher, source, "potential full");
        rejectUnchanged(dispatcher, source, player, "potential -1");
        rejectUnchanged(dispatcher, source, player, "potential " + (legendary.potential() + 1));
        rejectUnchanged(dispatcher, source, player, "rarity covenant:common"); // Remaining Potential exceeds budget.
        execute(dispatcher, source, "potential 10");
        execute(dispatcher, source, "rarity covenant:common");
        execute(dispatcher, source, "rarity covenant:legendary");
        check(sword.get(Workbench.QUALITY.get()).potential() == 10, "Rarity increase never refills Potential");
        execute(dispatcher, source, "reset");
        check(AffixPages.get(sword).isEmpty() && !sword.has(Workbench.QUALITY.get()) && ItemStack.isSameItemSameComponents(sword, underlying),
                "Reset preserves native item components exactly");
        execute(dispatcher, source, "rarity covenant:rare");
        check(AffixPages.get(sword).orElseThrow().affixes().isEmpty()
                && sword.get(Workbench.QUALITY.get()).potential() == data.rarity(id("rare")).potential(), "Rarity initializes empty equipment with full budget");
        rejectUnchanged(dispatcher, source, player, "set covenant:health covenant:legendary 1 max");
        execute(dispatcher, source, "set covenant:health covenant:common 3 max");
        check(sword.get(Workbench.QUALITY.get()).rarity().equals(id("rare")), "Set never promotes initialized equipment");
        check(player.getData(RpgData.PROFESSIONS).equals(profession) && player.getInventory().getItem(9).getCount() == 9
                && PlayerProfiles.get(player).equals(CharacterProfile.UNINITIALIZED), "No costs, profession requirements, XP, or character edits");
        // Cleanup remains usable for unknown definitions and legacy raw debug modifiers.
        AffixPages.update(h.getLevel(), sword, List.of(new RpgModifier(id("resource_cost"), RpgModifier.Operation.ADD, -0.2)),
                List.of(new Affix(id("removed_family"), id("removed_rarity"), 1, List.of())), new PageProgression(2, 17));
        check(suggestions(dispatcher, source, "remove ").contains("covenant:removed_family"), "Unknown saved family remains removable");
        execute(dispatcher, source, "remove covenant:removed_family");
        execute(dispatcher, source, "clear");
        check(AffixPages.get(sword).orElseThrow().modifiers().isEmpty(), "Clear also removes legacy raw bonuses");
        player.getInventory().clearContent();
    }

    private static void workbench(GameTestHelper h, ServerPlayer player, CommandSourceStack source) throws CommandSyntaxException {
        var dispatcher = h.getLevel().getServer().getCommands().getDispatcher();
        var sword = new ItemStack(Items.IRON_SWORD);
        player.setItemSlot(EquipmentSlot.MAINHAND, sword);
        execute(dispatcher, source, "set covenant:health covenant:common 1 max");
        var identity = AffixPages.get(sword).orElseThrow().identity();
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        var pos = h.absolutePos(new BlockPos(3, 100, 2));
        h.getLevel().setBlock(pos, Workbench.BLOCK.get().defaultBlockState(), 3);
        var station = (WorkbenchBlockEntity) h.getLevel().getBlockEntity(pos);
        station.setItem(0, sword);
        try {
            check(!WorkbenchOperations.craft(player, station, WorkbenchOperations.Operation.REROLL, 0, 0, 0),
                    "Debug equipment does not bypass normal Workbench profession requirements");
            var state = player.getData(RpgData.PROFESSIONS);
            player.setData(RpgData.PROFESSIONS, state.with(WorkbenchOperations.CRAFTING,
                    new dev.covenant.rpg.progression.ProfessionState.Track(1, 0)));
            dev.covenant.rpg.progression.ProfessionAllocations.apply(player, WorkbenchOperations.CRAFTING,
                    dev.covenant.rpg.progression.ProfessionData.snapshot(h.getLevel().getServer()).revision(),
                    java.util.Map.of(), java.util.Map.of(WorkbenchOperations.BASIC, 1));
            check(!WorkbenchOperations.craft(player, station, WorkbenchOperations.Operation.REROLL, 0, 0, 0),
                    "Debug equipment still requires ingredients in normal crafting");
            player.getInventory().setItem(1, new ItemStack(Items.LAPIS_LAZULI, 3));
            check(WorkbenchOperations.craft(player, station, WorkbenchOperations.Operation.REROLL, 0, 0, 0),
                    "Command-created equipment is immediately usable at the Workbench");
            var result = station.getItem(0);
            check(result.get(Workbench.QUALITY.get()).potential() == 15 && player.getInventory().getItem(1).getCount() == 2
                    && WorkbenchOperations.track(player).experience() == 20 && AffixPages.get(result).orElseThrow().identity().equals(identity),
                    "Normal crafting spends costs, awards XP and preserves command page identity");
        } finally {
            station.clearContent(); h.getLevel().removeBlock(pos, false); player.getInventory().clearContent();
        }
    }

    private static void transfers(GameTestHelper h, ServerPlayer player, CommandSourceStack source) throws CommandSyntaxException {
        var dispatcher = h.getLevel().getServer().getCommands().getDispatcher();
        var data = ItemizationData.get(h.getLevel().getServer());
        var first = new ItemStack(Items.IRON_SWORD);
        first.setDamageValue(11);
        player.setItemSlot(EquipmentSlot.MAINHAND, first);
        execute(dispatcher, source, "set covenant:health covenant:legendary 5 max");
        execute(dispatcher, source, "potential 17");
        var page = AffixPages.get(first).orElseThrow();
        var quality = first.get(Workbench.QUALITY.get());
        double bonus = bonus(first, "health");
        player.doTick();
        near(player.getAttributeValue(RpgStats.HEALTH), 20 + bonus, "Command gear applies native equipped bonuses");
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.getInventory().setItem(5, first);
        var second = new ItemStack(Items.IRON_SWORD);
        player.setItemSlot(EquipmentSlot.MAINHAND, second);
        execute(dispatcher, source, "set covenant:health covenant:common 1 min");
        var otherPage = AffixPages.get(second).orElseThrow();
        check(!page.identity().equals(otherPage.identity()), "Same item types get independent identities");
        player.doTick();
        near(player.getAttributeValue(RpgStats.HEALTH), 20 + data.family(id("health")).ranges().getFirst().min(), "Only equipped stack contributes");
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.getInventory().setItem(6, second);
        player.doTick();
        near(player.getAttributeValue(RpgStats.HEALTH), 20, "Inventory gear grants no bonus");
        player.getInventory().setItem(17, player.getInventory().removeItem(5, 1));
        check(player.getInventory().getItem(5).isEmpty(), "Native inventory transfer consumes source");
        var chestPos = new BlockPos(1, 1, 1);
        h.setBlock(chestPos, Blocks.CHEST);
        var chest = h.getBlockEntity(chestPos, ChestBlockEntity.class);
        chest.setItem(3, player.getInventory().removeItem(17, 1));
        var saved = chest.saveWithFullMetadata(h.getLevel().registryAccess());
        chest.clearContent();
        h.setBlock(chestPos, Blocks.AIR);
        h.setBlock(chestPos, Blocks.CHEST);
        chest = h.getBlockEntity(chestPos, ChestBlockEntity.class);
        var reporter = new ProblemReporter.Collector();
        chest.loadWithComponents(TagValueInput.create(reporter, h.getLevel().registryAccess(), saved));
        var retrieved = chest.removeItem(3, 1);
        check(chest.getItem(3).isEmpty() && reporter.isEmpty(), "Native chest round trip decodes and consumes source");
        sameGear(retrieved, page, quality);
        var dropped = player.drop(retrieved, false);
        check(dropped != null, "Native drop creates item entity");
        sameGear(dropped.getItem(), page, quality);
        dropped.setNoPickUpDelay();
        dropped.playerTouch(player);
        check(dropped.isRemoved(), "Pickup consumes entity");
        first = player.getMainHandItem();
        sameGear(first, page, quality);
        for (int i = 0; i < 10; i++) {
            first.getAttributeModifiers(); player.doTick();
            near(player.getAttributeValue(RpgStats.HEALTH), 20 + bonus, "Recalculation never compounds");
        }
        execute(dispatcher, source, "set covenant:health covenant:common 1 min");
        player.doTick();
        near(player.getAttributeValue(RpgStats.HEALTH), 20 + data.family(id("health")).ranges().getFirst().min(), "In-place edits refresh attributes");
        execute(dispatcher, source, "clear"); player.doTick();
        near(player.getAttributeValue(RpgStats.HEALTH), 20, "Clear removes equipped bonuses");
        check(player.getAttributeValue(RpgStats.DAMAGE) > 1 && first.getDamageValue() == 11, "Native sword stats/damage survive");
        sameGear(second, otherPage, second.get(Workbench.QUALITY.get()));
        check(!new ItemStack(Items.IRON_SWORD).has(RpgData.AFFIX_PAGE.get()), "Item defaults are unchanged");
        player.getInventory().clearContent();
    }

    private static void components(GameTestHelper h, ServerPlayer player, CommandSourceStack source) throws CommandSyntaxException {
        var dispatcher = h.getLevel().getServer().getCommands().getDispatcher();
        var professions = player.getData(RpgData.PROFESSIONS);
        var identity = new AffixMaterial(id("health"), id("rare"));
        execute(dispatcher, source, "component covenant:health covenant:rare");
        execute(dispatcher, source, "component covenant:health covenant:rare 7");
        execute(dispatcher, source, "component covenant:health covenant:epic 2");
        check(player.getInventory().getItem(0).getCount() == 8 && identity.equals(player.getInventory().getItem(0).get(Workbench.MATERIAL_ID.get())), "Default count and exact identity merge");
        check(player.getInventory().getItem(1).getCount() == 2 && player.getInventory().getItem(1).get(Workbench.MATERIAL_ID.get()).rarity().equals(id("epic")), "Different rarities remain separate");
        for (String suffix : List.of("component covenant:health covenant:common", "component covenant:missing covenant:rare",
                "component covenant:health covenant:missing", "component covenant:health covenant:rare 0", "component covenant:health covenant:rare 65")) {
            reject(dispatcher, source, suffix);
        }
        check(player.getInventory().getItem(0).getCount() == 8 && player.getInventory().getItem(1).getCount() == 2, "Rejected grants do not alter inventory");
        for (int i = 0; i < 36; i++) { player.getInventory().setItem(i, new ItemStack(Items.STONE, 64)); }
        execute(dispatcher, source, "component covenant:health covenant:rare 3");
        var overflow = h.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4),
                e -> identity.equals(e.getItem().get(Workbench.MATERIAL_ID.get())));
        check(overflow.stream().mapToInt(e -> e.getItem().getCount()).sum() == 3, "Full inventory drops exact overflow using native insertion");
        check(player.getData(RpgData.PROFESSIONS).equals(professions), "Component supply grants no profession XP");
        overflow.forEach(ItemEntity::discard);
    }

    /** Native player file retained outside the disposable GameTest world for a second server process. */
    @SuppressWarnings("removal")
    static void restart(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var player = helper.makeMockServerPlayerInLevel();
        var checkpoint = Path.of("affix-command-restart.properties");
        var retainedPlayerFile = Path.of("affix-command-restart-player.dat");
        var expected = new Properties();
        try {
            if ("verify".equals(System.getenv("COVENANT_AFFIX_RESTART_CHECK"))) {
                try (var reader = Files.newBufferedReader(checkpoint)) { expected.load(reader); }
                check(!Long.toString(ProcessHandle.current().pid()).equals(expected.getProperty("process")), "Verification requires a fresh process");
                var savedPlayer = new NameAndId(UUID.fromString(expected.getProperty("player")), "test-mock-player");
                var playerFile = server.getPlayerList().getPlayerIo().getPlayerDir().toPath().resolve(savedPlayer.id() + ".dat");
                Files.copy(retainedPlayerFile, playerFile, StandardCopyOption.REPLACE_EXISTING);
                var disk = server.getPlayerList().loadPlayerData(savedPlayer).orElseThrow();
                disk.remove("UUID");
                var reporter = new ProblemReporter.Collector();
                player.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
                player.doTick();
                var stack = player.getMainHandItem();
                var page = AffixPages.get(stack).orElseThrow();
                check(page.identity().toString().equals(expected.getProperty("page")) && page.category().equals(EquipmentCategory.MAIN_HAND)
                        && page.modifiers().isEmpty() && page.affixes().size() == 1 && page.affixes().getFirst().id().equals(id("health"))
                        && page.affixes().getFirst().rarity().equals(id("legendary")) && page.affixes().getFirst().tier() == 5,
                        "New command page survives restart unchanged");
                near(bonus(stack, "health"), Double.parseDouble(expected.getProperty("bonus")), "Saved rolled amount survives restart");
                check(stack.get(Workbench.QUALITY.get()).equals(new EquipmentQuality(id("legendary"), 17)), "Rarity and spent Potential survive restart");
                check(stack.is(Items.IRON_SWORD) && stack.getDamageValue() == 11 && reporter.isEmpty(), "Native item file decodes cleanly");
                near(player.getAttributeValue(RpgStats.HEALTH), 20 + bonus(stack, "health"), "Restart reconstructs bonus once");
                check(PlayerProfiles.get(player).equals(CharacterProfile.UNINITIALIZED), "No character progression created");
            } else {
                var sword = new ItemStack(Items.IRON_SWORD);
                sword.setDamageValue(11);
                player.setItemSlot(EquipmentSlot.MAINHAND, sword);
                var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
                execute(server.getCommands().getDispatcher(), source, "set covenant:health covenant:legendary 5 max");
                execute(server.getCommands().getDispatcher(), source, "potential 17");
                player.doTick();
                server.getPlayerList().saveAll();
                var playerFile = server.getPlayerList().getPlayerIo().getPlayerDir().toPath().resolve(player.getUUID() + ".dat");
                Files.copy(playerFile, retainedPlayerFile, StandardCopyOption.REPLACE_EXISTING);
                expected.setProperty("player", player.getUUID().toString());
                expected.setProperty("page", AffixPages.get(sword).orElseThrow().identity().toString());
                expected.setProperty("bonus", Double.toString(bonus(sword, "health")));
                expected.setProperty("process", Long.toString(ProcessHandle.current().pid()));
                try (var writer = Files.newBufferedWriter(checkpoint)) { expected.store(writer, "Affixed equipment restart expectations"); }
            }
            helper.succeed();
        } catch (IOException | CommandSyntaxException exception) {
            throw new IllegalStateException("Affix restart fixture failed", exception);
        } finally { server.getPlayerList().remove(player); }
    }

    private static Identifier id(String path) { return ItemizationData.id(path); }
    private static double bonus(ItemStack stack, String family) {
        return AffixPages.get(stack).orElseThrow().affixes().stream().filter(a -> a.id().equals(id(family)))
                .findFirst().orElseThrow().modifiers().getFirst().amount();
    }
    private static void sameGear(ItemStack stack, AffixPage page, EquipmentQuality quality) {
        check(!stack.isEmpty() && AffixPages.get(stack).orElseThrow().equals(page) && quality.equals(stack.get(Workbench.QUALITY.get())), "Exact page/quality stay with stack");
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.00001, message + ": expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
    private static int execute(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, String suffix) throws CommandSyntaxException {
        return dispatcher.execute("covenant affix " + suffix, source);
    }
    private static List<String> suggestions(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, String suffix) {
        return dispatcher.getCompletionSuggestions(dispatcher.parse("covenant affix " + suffix, source)).join().getList().stream().map(s -> s.getText()).toList();
    }
    private static void reject(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, String suffix) {
        try { execute(dispatcher, source, suffix); } catch (CommandSyntaxException expected) { return; }
        throw new IllegalStateException("Command should be rejected: " + suffix);
    }
    private static void rejectUnchanged(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, ServerPlayer player, String suffix) {
        var before = player.getMainHandItem().copy();
        reject(dispatcher, source, suffix);
        check(ItemStack.matches(before, player.getMainHandItem()), "Rejected command changes nothing: " + suffix);
    }
}
