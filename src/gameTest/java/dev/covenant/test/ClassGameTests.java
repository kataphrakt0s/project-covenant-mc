package dev.covenant.test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.covenant.rpg.CharacterProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.stats.RpgStats;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;

/** Exercises the registered player command and existing profile lifecycles on a dedicated server. */
final class ClassGameTests {
    private ClassGameTests() {}

    @SuppressWarnings("removal") // Pinned helper supplies an embedded connection for player lifecycles.
    static void run(GameTestHelper helper) {
        List<ServerPlayer> players = new ArrayList<>();
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        try {
            check(PlayerClass.all().stream().map(c -> c.id().toString()).toList().equals(
                    List.of("covenant:stalker", "covenant:reaver", "covenant:weaver")), "Stable class IDs");
            rejectCommand(dispatcher, server.createCommandSourceStack(), "covenant class select covenant:weaver");
            for (var definition : PlayerClass.all()) {
                var original = helper.makeMockServerPlayerInLevel();
                players.add(original);
                var source = original.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
                check(PlayerProfiles.getSelectedClass(original).isEmpty(), "No class before selection");
                dispatcher.execute("covenant class", source);
                rejectCommand(dispatcher, source, "covenant class select covenant:missing");
                rejectCommand(dispatcher, source, "covenant class select other:stalker");
                rejectCommand(dispatcher, source, "covenant class select covenant:INVALID");
                check(PlayerProfiles.get(original).equals(CharacterProfile.UNINITIALIZED), "Queries/invalid input cannot create a character");
                var suggestions = dispatcher.getCompletionSuggestions(
                        dispatcher.parse("covenant class select ", source)).join().getList();
                check(suggestions.size() == 3 && suggestions.stream().anyMatch(s -> s.getText().equals(definition.id().toString())),
                        "Every defined class is suggested to non-operators");

                check(dispatcher.execute("covenant class select " + definition.id(), source) == 1, "Non-operator selection succeeds");
                var selected = PlayerProfiles.get(original);
                check(PlayerProfiles.getSelectedClass(original).orElseThrow() == definition, "Definition lookup after selection");
                check(selected.equals(CharacterProfile.create(definition.id(), Optional.empty(), 0)), "Existing profile owns selection; no resources");
                rejectCommand(dispatcher, source, "covenant class select covenant:reaver");
                dispatcher.execute("covenant class", source);
                check(PlayerProfiles.get(original).equals(selected), "Repeated selection/query leaves the profile intact");

                for (var item : List.of(Items.BOW, Items.IRON_SWORD, Items.IRON_AXE)) {
                    original.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
                    original.doTick();
                    check(original.getMainHandItem().is(item), "Classes do not restrict native equipment");
                }
                check(original.getAttributeValue(RpgStats.HEALTH) == 20, "No implicit class stat bonuses");
                check(PlayerProfiles.get(original).equals(selected), "Equipping does not change class state");
                var buffer = Unpooled.buffer();
                try {
                    CharacterProfile.STREAM_CODEC.encode(buffer, selected);
                    check(CharacterProfile.STREAM_CODEC.decode(buffer).equals(selected), "Selected class network codec");
                } finally {
                    buffer.release();
                }

                server.getPlayerList().saveAll();
                var disk = server.getPlayerList().loadPlayerData(original.nameAndId()).orElseThrow();
                disk.remove("UUID");
                var loaded = helper.makeMockServerPlayerInLevel();
                players.add(loaded);
                var reporter = new ProblemReporter.Collector();
                loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
                check(reporter.isEmpty(), "Selected profile file decodes cleanly");
                check(PlayerProfiles.get(loaded).equals(selected), "Actual player-file reload retains class");
                check(PlayerProfiles.getSelectedClass(loaded).orElseThrow() == definition, "Reload resolves class definition");

                var respawned = helper.makeMockServerPlayerInLevel();
                players.add(respawned);
                respawned.restoreFrom(loaded, false);
                check(PlayerProfiles.get(respawned).equals(selected), "Death clone retains selected class");
                var endReturn = helper.makeMockServerPlayerInLevel();
                players.add(endReturn);
                endReturn.restoreFrom(respawned, true);
                check(PlayerProfiles.get(endReturn).equals(selected), "End return retains selected class");
            }

            var legacy = players.getFirst();
            var unknownId = Identifier.fromNamespaceAndPath("covenant", "legacy_class");
            PlayerProfiles.update(legacy, p -> new CharacterProfile(true, 7, 23, Optional.of(unknownId),
                    Optional.empty(), Optional.empty(), 0, 0));
            var unknown = PlayerProfiles.get(legacy);
            dispatcher.execute("covenant class", legacy.createCommandSourceStack());
            rejectCommand(dispatcher, legacy.createCommandSourceStack(), "covenant class select covenant:stalker");
            check(PlayerProfiles.getSelectedClass(legacy).isEmpty() && PlayerProfiles.get(legacy).equals(unknown),
                    "Unknown saved IDs remain intact without fallback or accidental reinitialization");
            helper.succeed();
        } catch (CommandSyntaxException exception) {
            throw new IllegalStateException("Valid class command failed", exception);
        } finally {
            players.forEach(server.getPlayerList()::remove);
        }
    }

    private static void rejectCommand(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack source, String command) {
        try {
            dispatcher.execute(command, source);
        } catch (CommandSyntaxException expected) {
            return;
        }
        throw new IllegalStateException("Command should be rejected: " + command);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
