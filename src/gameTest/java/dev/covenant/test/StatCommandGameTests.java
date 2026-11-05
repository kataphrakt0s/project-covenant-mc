package dev.covenant.test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.covenant.rpg.CharacterProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.equipment.Affix;
import dev.covenant.rpg.equipment.AffixPages;
import dev.covenant.rpg.equipment.EquipmentCategory;
import dev.covenant.rpg.equipment.PageProgression;
import dev.covenant.rpg.equipment.RpgModifier;
import dev.covenant.rpg.stats.RpgStats;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;

/** Registered command behavior, native modifier coexistence, and actual player-file isolation. */
final class StatCommandGameTests {
    private StatCommandGameTests() {}

    @SuppressWarnings("removal")
    static void run(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        List<ServerPlayer> players = new ArrayList<>();
        try {
            var player = helper.makeMockServerPlayerInLevel();
            players.add(player);
            var feedback = new Feedback();
            var source = player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSource(feedback);
            var ordinary = source.withPermission(PermissionSet.NO_PERMISSIONS);
            reject(dispatcher, ordinary, "covenant stats");
            reject(dispatcher, ordinary, "covenant stats set covenant:dodge 1");
            reject(dispatcher, ordinary, "covenant stats reset");
            reject(dispatcher, server.createCommandSourceStack(), "covenant stats");
            reject(dispatcher, server.createCommandSourceStack(), "covenant stats get covenant:dodge");
            reject(dispatcher, server.createCommandSourceStack(), "covenant stats set covenant:dodge 1");
            reject(dispatcher, server.createCommandSourceStack(), "covenant stats reset");
            for (String verb : List.of("get", "set")) {
                var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("covenant stats " + verb + " ", source))
                        .join().getList().stream().map(suggestion -> suggestion.getText()).toList();
                check(suggestions.size() == RpgStats.SUPPORTED.size(), "Complete supported stat list");
                for (var attribute : RpgStats.SUPPORTED) {
                    check(suggestions.contains(BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()).toString()), "Stat ID completion");
                }
            }
            check(dispatcher.execute("covenant stats", source) == RpgStats.SUPPORTED.size(), "List every supported effective value");
            check(feedback.messages.size() == RpgStats.SUPPORTED.size() + 1, "Header and supported stat entries");
            dispatcher.execute("covenant stats get covenant:dodge", source);
            check(feedback.has("commands.covenant.stats.no_modifiers"), "Empty modifier feedback");
            for (String command : List.of("get covenant:missing", "set minecraft:luck 1", "get covenant:INVALID",
                    "set covenant:dodge NaN", "set covenant:dodge Infinity", "set covenant:dodge 1e309",
                    "set covenant:dodge -0.1", "set covenant:dodge 1.1", "set minecraft:max_health 0",
                    "set covenant:cooldown 1025", "set covenant:dodge nope")) {
                reject(dispatcher, source, "covenant stats " + command);
            }
            check(PlayerProfiles.get(player).equals(CharacterProfile.UNINITIALIZED), "Queries/invalid commands do not create profiles");

            // Exercise every supported attribute, including zero, fractional, and integer bases.
            for (var attribute : RpgStats.SUPPORTED) {
                var instance = player.getAttribute(attribute);
                double base = instance.getBaseValue();
                String id = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()).toString();
                double testValue = attribute.equals(RpgStats.DODGE) ? 0.5
                        : attribute.equals(RpgStats.FOCUS_REGENERATION) ? 1.25 : base + 1;
                dispatcher.execute("covenant stats set " + id + " " + testValue, source);
                near(instance.getBaseValue(), base, "Native base remains unchanged");
                near(instance.getValue(), testValue, "Immediate testing base");
                check(instance.getPermanentModifiers().isEmpty(), "Debug modifier must be transient");
                dispatcher.execute("covenant stats get " + id, source);
            }
            check(dispatcher.execute("covenant stats reset", source) == RpgStats.SUPPORTED.size(), "Reset all attributes");
            check(dispatcher.execute("covenant stats reset", source) == 0, "Repeated reset is harmless");

            PlayerProfiles.initialize(player, id("stalker"), Optional.empty(), 0);
            var profile = new CharacterProfile(true, 7, 321, Optional.of(id("stalker")),
                    Optional.empty(), Optional.empty(), 0, 0);
            PlayerProfiles.update(player, ignored -> profile);
            var gear = new ItemStack(Items.IRON_CHESTPLATE);
            var initialModifiers = List.of(new RpgModifier(id("dodge"), RpgModifier.Operation.ADD, 0.1),
                    new RpgModifier(id("dodge"), RpgModifier.Operation.MULTIPLY_BASE, 0.25));
            var affixes = List.of(new Affix(id("test"), id("common"), 1,
                    List.of(new RpgModifier(id("dodge"), RpgModifier.Operation.MULTIPLY_TOTAL, 0.2))));
            var page = AffixPages.attach(helper.getLevel(), gear, EquipmentCategory.BODY_ARMOR, initialModifiers, affixes);
            player.setItemSlot(EquipmentSlot.CHEST, gear);
            player.doTick();
            var dodge = player.getAttribute(RpgStats.DODGE);
            var external = new AttributeModifier(id("test_external"), 0.05, AttributeModifier.Operation.ADD_VALUE);
            dodge.addPermanentModifier(external);
            var normalModifiers = dodge.getModifiers();
            near(dodge.getValue(), 0.225, "Normal gear plus permanent external modifier");
            var packedBefore = dodge.pack();

            dispatcher.execute("covenant stats set covenant:dodge 0.2", source);
            near(dodge.getValue(), 0.525, "Testing base plus flat, base-percent, total-percent modifiers");
            dispatcher.execute("covenant stats set covenant:dodge 0.3", source);
            near(dodge.getValue(), 0.675, "Repeated set replaces the debug modifier");
            check(dodge.getModifiers().size() == normalModifiers.size() + 1, "Exactly one debug modifier");
            for (int i = 0; i < 5; i++) { player.doTick(); }
            near(dodge.getValue(), 0.675, "Ticks do not compound overrides or equipment");
            feedback.messages.clear();
            dispatcher.execute("covenant stats get covenant:dodge", source);
            check(feedback.has("commands.covenant.stats.base") && feedback.has("commands.covenant.stats.override")
                    && feedback.has("commands.covenant.stats.modifier") && feedback.has("commands.covenant.stats.effective"),
                    "Inspection includes normal base, debug base, modifier IDs/operations/amounts, and effective value");
            check(dodge.pack().equals(packedBefore), "Native saved base and permanent modifiers remain identical");
            check(PlayerProfiles.get(player).equals(profile), "Character progression remains identical");
            check(AffixPages.get(gear).orElseThrow().equals(page), "Page remains identical");
            reject(dispatcher, source, "covenant stats set covenant:dodge 2");
            near(dodge.getValue(), 0.675, "Invalid input preserves active override");

            // Save while override is active, then load the actual file into a fresh player.
            server.getPlayerList().saveAll();
            var disk = server.getPlayerList().loadPlayerData(player.nameAndId()).orElseThrow();
            disk.remove("UUID");
            var loaded = helper.makeMockServerPlayerInLevel();
            players.add(loaded);
            var reporter = new ProblemReporter.Collector();
            loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
            loaded.doTick();
            check(reporter.isEmpty(), "Saved player file decodes cleanly");
            near(loaded.getAttributeValue(RpgStats.DODGE), 0.225, "Reload restores normal calculation, excluding debug override");
            check(loaded.getAttribute(RpgStats.DODGE).getModifiers().equals(normalModifiers), "Saved modifiers exclude debug adjustment");
            check(PlayerProfiles.get(loaded).equals(profile), "Saved profile unchanged");
            check(AffixPages.get(loaded.getItemBySlot(EquipmentSlot.CHEST)).orElseThrow().equals(page), "Saved page unchanged");
            for (boolean endReturn : List.of(false, true)) {
                var clone = helper.makeMockServerPlayerInLevel();
                players.add(clone);
                clone.restoreFrom(player, endReturn);
                check(clone.getAttribute(RpgStats.DODGE).getModifiers().stream()
                        .noneMatch(modifier -> modifier.id().getPath().equals("debug/base_override")), "Clones do not inherit debug overrides");
            }
            dispatcher.execute("covenant stats reset", source);
            near(dodge.getValue(), 0.225, "Reset immediately restores normal calculation");
            check(dodge.getModifiers().equals(normalModifiers), "Reset preserves all non-debug modifiers");
            dispatcher.execute("covenant stats set covenant:dodge 0.3", source);
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            player.doTick();
            near(dodge.getValue(), 0.35, "Unequip during override preserves external and debug modifiers");
            player.setItemSlot(EquipmentSlot.CHEST, gear);
            player.doTick();
            near(dodge.getValue(), 0.675, "Reequip during override applies page once");
            AffixPages.update(helper.getLevel(), gear,
                    List.of(new RpgModifier(id("dodge"), RpgModifier.Operation.ADD, 0.2)), List.of(), PageProgression.NONE);
            player.doTick();
            near(dodge.getValue(), 0.55, "In-place page update works during override");
            dispatcher.execute("covenant stats reset", source);
            near(dodge.getValue(), 0.25, "Reset uses current page, not a stale snapshot");
            check(PlayerProfiles.get(player).equals(profile), "Final profile unchanged");
            helper.succeed();
        } catch (CommandSyntaxException exception) {
            throw new IllegalStateException("Valid stat command failed", exception);
        } finally {
            players.forEach(server.getPlayerList()::remove);
        }
    }

    private static void reject(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, String command) {
        try { dispatcher.execute(command, source); }
        catch (CommandSyntaxException expected) { return; }
        throw new IllegalStateException("Expected command rejection: " + command);
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.00001, message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("covenant", path); }

    private static final class Feedback implements CommandSource {
        final List<Component> messages = new ArrayList<>();
        @Override public void sendSystemMessage(Component message) { messages.add(message); }
        @Override public boolean acceptsSuccess() { return true; }
        @Override public boolean acceptsFailure() { return true; }
        @Override public boolean shouldInformAdmins() { return false; }
        boolean has(String key) {
            return messages.stream().anyMatch(message -> message.getContents() instanceof TranslatableContents text
                    && text.getKey().equals(key));
        }
    }
}
