package dev.covenant.test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.covenant.rpg.CharacterProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.equipment.Affix;
import dev.covenant.rpg.equipment.AffixPage;
import dev.covenant.rpg.equipment.AffixPages;
import dev.covenant.rpg.equipment.EquipmentCategory;
import dev.covenant.rpg.equipment.PageProgression;
import dev.covenant.rpg.equipment.RpgModifier;
import dev.covenant.rpg.stats.RpgStats;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

/** Exercises real commands and native stack/container/equipment lifecycles on the dedicated server. */
final class AffixCommandGameTests {
    private AffixCommandGameTests() {}

    @SuppressWarnings("removal")
    static void run(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        var player = helper.makeMockServerPlayerInLevel();
        var feedback = new Feedback();
        var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSource(feedback);
        try {
            for (String suffix : List.of("inspect", "remove", "clear", "apply covenant:main_hand minecraft:max_health 4")) {
                reject(dispatcher, source.withPermission(PermissionSet.NO_PERMISSIONS), "covenant affix " + suffix);
                reject(dispatcher, server.createCommandSourceStack(), "covenant affix " + suffix);
                reject(dispatcher, source, "covenant affix " + suffix); // Empty main hand.
            }
            var first = new ItemStack(Items.IRON_SWORD);
            first.setDamageValue(7);
            first.set(DataComponents.CUSTOM_NAME, Component.literal("Affix command test"));
            var underlying = first.copy();
            player.setItemSlot(EquipmentSlot.MAINHAND, first);
            check(dispatcher.execute("covenant affix inspect", source) == 0 && feedback.has("no_page"), "Clear no-page feedback");
            check(dispatcher.execute("covenant affix clear", source) == 0, "Clearing unpaged item is harmless");
            var categories = dispatcher.getCompletionSuggestions(dispatcher.parse("covenant affix apply ", source))
                    .join().getList().stream().map(suggestion -> suggestion.getText()).toList();
            check(categories.contains("covenant:main_hand") && categories.contains("covenant_test:charm"),
                    "Category completion reuses loaded tags, including custom categories");
            var stats = dispatcher.getCompletionSuggestions(dispatcher.parse("covenant affix apply covenant:main_hand ", source))
                    .join().getList();
            check(stats.size() == RpgStats.SUPPORTED.size(), "Stat completion uses supported stats");
            for (String arguments : List.of("covenant:missing minecraft:max_health 4", "covenant:body_armor minecraft:max_health 4",
                    "covenant:main_hand covenant:missing 4", "covenant:main_hand minecraft:luck 4",
                    "covenant:main_hand minecraft:max_health NaN", "covenant:main_hand minecraft:max_health Infinity",
                    "covenant:main_hand minecraft:max_health 1e309", "covenant:main_hand minecraft:max_health nope",
                    "covenant:INVALID minecraft:max_health 4")) {
                reject(dispatcher, source, "covenant affix apply " + arguments);
                check(ItemStack.isSameItemSameComponents(first, underlying), "Invalid apply leaves all item data intact");
            }
            check(dispatcher.execute("covenant affix apply covenant:main_hand minecraft:max_health 4", source) == 1,
                    "Vanilla item receives a normal page");
            var page = AffixPages.get(first).orElseThrow();
            check(page.category().equals(EquipmentCategory.MAIN_HAND) && page.modifiers().equals(List.of(health(4)))
                    && page.affixes().isEmpty() && page.progression().equals(PageProgression.NONE), "Simple test page format");
            reject(dispatcher, source, "covenant affix apply covenant:main_hand minecraft:max_health 8");
            samePage(first, page);
            feedback.messages.clear();
            dispatcher.execute("covenant affix inspect", source);
            check(feedback.has("page") && feedback.modifier("minecraft:max_health", "add", 4), "Inspect category and modifier details");
            player.doTick();
            near(player.getAttributeValue(RpgStats.HEALTH), 24, "Main-hand page applies on native update");

            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            player.getInventory().setItem(5, first);
            var second = new ItemStack(Items.IRON_SWORD);
            player.setItemSlot(EquipmentSlot.MAINHAND, second);
            dispatcher.execute("covenant affix apply covenant:main_hand minecraft:max_health 8", source);
            var otherPage = AffixPages.get(second).orElseThrow();
            check(!page.identity().equals(otherPage.identity()), "Identical item types have independent identities and values");
            samePage(first, page);
            player.doTick();
            near(player.getAttributeValue(RpgStats.HEALTH), 28, "Only the equipped sword contributes");
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            player.getInventory().setItem(6, second);
            player.doTick();
            near(player.getAttributeValue(RpgStats.HEALTH), 20, "Unequip removes page contribution");
            player.getInventory().setItem(17, player.getInventory().removeItem(5, 1));
            check(player.getInventory().getItem(5).isEmpty(), "Inventory move consumes source");
            samePage(player.getInventory().getItem(17), page);

            // A real chest block entity, including native block-entity serialization/reconstruction.
            var chestPos = new BlockPos(1, 1, 1);
            helper.setBlock(chestPos, Blocks.CHEST);
            var chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
            chest.setItem(3, player.getInventory().removeItem(17, 1));
            var savedChest = chest.saveWithFullMetadata(helper.getLevel().registryAccess());
            chest.clearContent(); // The saved snapshot is about to replace this live container.
            helper.setBlock(chestPos, Blocks.AIR);
            helper.setBlock(chestPos, Blocks.CHEST);
            chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
            var reporter = new ProblemReporter.Collector();
            chest.loadWithComponents(TagValueInput.create(reporter, helper.getLevel().registryAccess(), savedChest));
            var retrieved = chest.removeItem(3, 1);
            check(chest.getItem(3).isEmpty(), "Chest retrieval consumes source");
            samePage(retrieved, page);
            check(reporter.isEmpty(), "Chest persistence decodes cleanly");
            var dropped = player.drop(retrieved, false);
            check(dropped != null, "Native player drop creates an item entity");
            samePage(dropped.getItem(), page);
            dropped.setNoPickUpDelay();
            dropped.playerTouch(player);
            check(dropped.isRemoved(), "Pickup consumes dropped entity");
            first = player.getMainHandItem();
            samePage(first, page);
            player.doTick();
            near(player.getAttributeValue(RpgStats.HEALTH), 24, "Pickup/equip restores the same page once");
            for (int i = 0; i < 10; i++) {
                first.getAttributeModifiers();
                player.doTick();
                near(player.getAttributeValue(RpgStats.HEALTH), 24, "Repeated native recalculation cannot compound");
            }
            check(dispatcher.execute("covenant affix remove", source) == 1, "Remove existing page");
            check(player.getMainHandItem() == first && first.getCount() == 1
                    && ItemStack.isSameItemSameComponents(first, underlying), "Remove preserves exact underlying item and components");
            player.doTick();
            near(player.getAttributeValue(RpgStats.HEALTH), 20, "Removing equipped page removes its modifiers");
            check(player.getAttributeValue(RpgStats.DAMAGE) > 1, "Native sword damage survives page removal");
            dispatcher.execute("covenant affix apply covenant:main_hand minecraft:max_health 4", source);
            AffixPages.update(helper.getLevel(), first, List.of(health(4), health(2)), List.of(
                    new Affix(id("test"), id("common"), 1, List.of(
                            new RpgModifier(id("dodge"), RpgModifier.Operation.ADD, 0.2),
                            new RpgModifier(id("dodge"), RpgModifier.Operation.MULTIPLY_TOTAL, 0.5)))), new PageProgression(2, 17));
            feedback.messages.clear();
            dispatcher.execute("covenant affix inspect", source);
            check(feedback.modifier("minecraft:max_health", "add", 4) && feedback.modifier("minecraft:max_health", "add", 2)
                    && feedback.modifier("covenant:dodge", "add", 0.2) && feedback.modifier("covenant:dodge", "multiply_total", 0.5),
                    "Inspection retains every independent base and nested affix modifier");
            check(dispatcher.execute("covenant affix clear", source) == 1 && AffixPages.get(first).isEmpty(),
                    "Clear removes the complete page, nested affixes, identity, and progression");
            check(ItemStack.isSameItemSameComponents(first, underlying), "Clear preserves all other components");
            player.doTick();
            near(player.getAttributeValue(RpgStats.HEALTH), 20, "Clear leaves no stale bonus");

            var paper = new ItemStack(Items.PAPER, 2);
            player.setItemSlot(EquipmentSlot.MAINHAND, paper);
            reject(dispatcher, source, "covenant affix apply covenant_test:charm covenant:resource_cost -0.2");
            check(paper.getCount() == 2 && paper.getMaxStackSize() == 64 && AffixPages.get(paper).isEmpty(), "Stack rejection is atomic");
            paper.setCount(1);
            dispatcher.execute("covenant affix apply covenant_test:charm covenant:resource_cost -0.2", source);
            check(AffixPages.get(paper).orElseThrow().modifiers().getFirst().amount() == -0.2, "Signed fractional additions are valid");
            player.doTick();
            near(player.getAttributeValue(RpgStats.RESOURCE_COST), 1, "Custom category has no implicit slot integration");
            dispatcher.execute("covenant affix clear", source);
            check(paper.getMaxStackSize() == 1 && paper.getCount() == 1 && AffixPages.get(paper).isEmpty(),
                    "Removal preserves unrelated stack-limit component rather than guessing its prior value");
            AffixPages.attach(helper.getLevel(), paper, new EquipmentCategory(Identifier.parse("covenant_test:charm")), List.of(), List.of());
            feedback.messages.clear();
            dispatcher.execute("covenant affix inspect", source);
            check(feedback.has("no_modifiers"), "Empty page is distinct from missing page");
            samePage(second, otherPage);
            check(AffixPages.get(new ItemStack(Items.IRON_SWORD)).isEmpty(), "Item type defaults were not changed");
            check(PlayerProfiles.get(player).equals(CharacterProfile.UNINITIALIZED), "Commands and transfers do not touch character data");
            helper.succeed();
        } catch (CommandSyntaxException exception) {
            throw new IllegalStateException("Valid affix command failed", exception);
        } finally {
            server.getPlayerList().remove(player);
        }
    }

    /**
     * Run once normally to write a native player file, then in another server process with
     * COVENANT_AFFIX_RESTART_CHECK=verify. The small properties file holds test expectations only;
     * the actual item is stored and reloaded by Minecraft's normal player persistence. Vanilla
     * deletes the GameTest world on launch, so retain/restore its native player file unchanged.
     */
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
                check(!Long.toString(ProcessHandle.current().pid()).equals(expected.getProperty("process")), "Verification requires a fresh server process");
                var savedPlayer = new NameAndId(UUID.fromString(expected.getProperty("player")), "test-mock-player");
                var playerFile = server.getPlayerList().getPlayerIo().getPlayerDir().toPath().resolve(savedPlayer.id() + ".dat");
                Files.copy(retainedPlayerFile, playerFile, StandardCopyOption.REPLACE_EXISTING);
                var disk = server.getPlayerList().loadPlayerData(savedPlayer)
                        .orElseThrow(() -> new IllegalStateException("Retained native player file was not loaded"));
                disk.remove("UUID");
                var reporter = new ProblemReporter.Collector();
                player.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
                player.doTick();
                var page = AffixPages.get(player.getMainHandItem())
                        .orElseThrow(() -> new IllegalStateException("Page missing from restarted player's main hand"));
                check(page.identity().toString().equals(expected.getProperty("page")) && page.category().equals(EquipmentCategory.MAIN_HAND)
                        && page.modifiers().equals(List.of(health(6))), "Command-created page survives full server stop/start unchanged");
                check(player.getMainHandItem().is(Items.IRON_SWORD) && player.getMainHandItem().getDamageValue() == 11, "Underlying item survives restart");
                near(player.getAttributeValue(RpgStats.HEALTH), 26, "Restart reconstructs page contribution once");
                check(reporter.isEmpty(), "Restart player file decodes cleanly");
                check(PlayerProfiles.get(player).equals(CharacterProfile.UNINITIALIZED), "Restart does not create character progression");
            } else {
                var sword = new ItemStack(Items.IRON_SWORD);
                sword.setDamageValue(11);
                player.setItemSlot(EquipmentSlot.MAINHAND, sword);
                server.getCommands().getDispatcher().execute("covenant affix apply covenant:main_hand minecraft:max_health 6",
                        player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS));
                player.doTick();
                server.getPlayerList().saveAll();
                var playerFile = server.getPlayerList().getPlayerIo().getPlayerDir().toPath().resolve(player.getUUID() + ".dat");
                Files.copy(playerFile, retainedPlayerFile, StandardCopyOption.REPLACE_EXISTING);
                expected.setProperty("player", player.getUUID().toString());
                expected.setProperty("page", AffixPages.get(sword).orElseThrow().identity().toString());
                expected.setProperty("process", Long.toString(ProcessHandle.current().pid()));
                try (var writer = Files.newBufferedWriter(checkpoint)) { expected.store(writer, "Affix Page restart test expectations"); }
            }
            helper.succeed();
        } catch (IOException | CommandSyntaxException exception) {
            throw new IllegalStateException("Affix restart fixture failed", exception);
        } finally {
            server.getPlayerList().remove(player);
        }
    }

    private static RpgModifier health(double amount) {
        return new RpgModifier(Identifier.parse("minecraft:max_health"), RpgModifier.Operation.ADD, amount);
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("covenant", path); }

    private static void samePage(ItemStack stack, AffixPage expected) {
        check(!stack.isEmpty() && AffixPages.get(stack).orElseThrow().equals(expected), "Exact page stays with individual stack");
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.00001, message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
    }

    private static void reject(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, String command) {
        try { dispatcher.execute(command, source); }
        catch (CommandSyntaxException expected) { return; }
        throw new IllegalStateException("Command should be rejected: " + command);
    }

    private static final class Feedback implements CommandSource {
        final List<Component> messages = new ArrayList<>();
        @Override public void sendSystemMessage(Component message) { messages.add(message); }
        @Override public boolean acceptsSuccess() { return true; }
        @Override public boolean acceptsFailure() { return true; }
        @Override public boolean shouldInformAdmins() { return false; }
        boolean has(String suffix) {
            return messages.stream().anyMatch(message -> message.getContents() instanceof TranslatableContents text
                    && text.getKey().equals("commands.covenant.affix." + suffix));
        }
        boolean modifier(String stat, String operation, double amount) {
            return messages.stream().anyMatch(message -> message.getContents() instanceof TranslatableContents text
                    && text.getKey().equals("commands.covenant.affix.modifier") && text.getArgs()[1].equals(stat)
                    && text.getArgs()[2].equals(operation) && text.getArgs()[3].equals(amount));
        }
    }
}
