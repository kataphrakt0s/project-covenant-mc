package dev.covenant.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.covenant.rpg.*;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.progression.*;
import dev.covenant.rpg.skills.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;

/** Survival-accurate shared-foundation tests, independent of unfinished gathering abilities. */
final class ProfessionGameTests {
    static final Identifier LOGGING = ProfessionData.id("logging");
    static final Identifier OFFENSIVE = ProfessionData.OFFENSIVE_MASTERY;
    private static long revision(ServerPlayer player) { return ProfessionData.snapshot(player.level().getServer()).revision(); }
    private static ProfessionState.Track logging(ServerPlayer player) { return player.getData(RpgData.PROFESSIONS).tracks().get(LOGGING); }
    private static void level(ServerPlayer player, Identifier id, int value) {
        var state = player.getData(RpgData.PROFESSIONS);
        player.setData(RpgData.PROFESSIONS, state.with(id, state.tracks().get(id).withProgression(value, 0)));
        Progression.refreshView(player);
    }
    static void storage(GameTestHelper helper) {
        try (var f = new ProgressionGameTests.Fixture(helper)) {
            var player = f.player(null); var mining = ProfessionData.id("mining");
            var oldJson = new com.google.gson.JsonObject();
            for (var id : ProgressionData.PROFESSIONS) { oldJson.add(id.toString(), JsonParser.parseString("{\"level\":0,\"experience\":0}")); }
            check(ProfessionState.CODEC.parse(JsonOps.INSTANCE, oldJson).getOrThrow().equals(ProfessionState.INITIAL), "Old foundation tracks decode without allocations");
            var reservation = Map.of(ProfessionData.id("mining/removed_ability"), new ProfessionState.Allocation(2, 3));
            var state = player.getData(RpgData.PROFESSIONS).with(mining, new ProfessionState.Track(7, 41, reservation));
            player.setData(RpgData.PROFESSIONS, state);
            check(state.tracks().get(mining).points() == 4, "Paid historical costs reserve earned points");
            var encoded = ProfessionState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
            check(ProfessionState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(state), "Unknown allocation IDs survive serialization");
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try { ProfessionState.STREAM_CODEC.encode(buffer, state); check(ProfessionState.STREAM_CODEC.decode(buffer).equals(state), "Owner state round trip preserves paid costs"); }
            finally { buffer.release(); }
            long needed = ProgressionData.get(helper.getLevel().getServer()).required(mining, 7) - 41;
            check(Progression.awardProfession(player, helper.getLevel(), mining, needed), "Classless award reaches next level");
            var advanced = player.getData(RpgData.PROFESSIONS);
            check(advanced.tracks().get(mining).level() == 8 && advanced.tracks().get(mining).allocations().equals(reservation), "Level gains retain allocations and historical costs");
            check(!PlayerProfiles.get(player).created() && advanced.tracks().get(LOGGING).level() == 0, "Awards neither select a class nor affect another profession");
            var clone = f.player(null); clone.restoreFrom(player, false);
            check(clone.getData(RpgData.PROFESSIONS).equals(advanced), "Native death copy retains complete profession state");
            helper.getLevel().getServer().getPlayerList().saveAll();
            var disk = helper.getLevel().getServer().getPlayerList().loadPlayerData(player.nameAndId()).orElseThrow(); disk.remove("UUID");
            var loaded = f.player(null); var reporter = new ProblemReporter.Collector();
            loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
            check(reporter.isEmpty() && loaded.getData(RpgData.PROFESSIONS).equals(advanced), "Native save/reconnect load retains independent allocations");
            check(Progression.awardProfession(player, helper.getLevel(), mining, Long.MAX_VALUE), "Large award clamps at configured cap");
            check(player.getData(RpgData.PROFESSIONS).tracks().get(mining).level() == 20
                    && !Progression.awardProfession(player, helper.getLevel(), mining, 1), "At-cap awards neither bank XP nor erase paid allocations");
            helper.succeed();
        }
    }
    static void validation(GameTestHelper helper) {
        try (var f = new ProgressionGameTests.Fixture(helper)) {
            var player = f.player(null); long revision = revision(player);
            level(player, LOGGING, 4);
            ProfessionAllocations.apply(player, LOGGING, revision, Map.of(), Map.of(OFFENSIVE, 1));
            check(logging(player).allocations().isEmpty(), "Every purchased rank observes its level gate");
            level(player, LOGGING, 20); var empty = player.getData(RpgData.PROFESSIONS);
            ProfessionAllocations.apply(player, LOGGING, revision, Map.of(), Map.of(OFFENSIVE, 1, ProfessionData.id("logging/tree_felling"), 1));
            check(player.getData(RpgData.PROFESSIONS).equals(empty), "Mixed available/planned purchase fails atomically");
            ProfessionAllocations.apply(player, ProfessionData.id("mining"), revision, Map.of(), Map.of(OFFENSIVE, 1));
            ProfessionAllocations.apply(player, LOGGING, revision, Map.of(), Map.of(OFFENSIVE, 6));
            ProfessionAllocations.apply(player, LOGGING, revision + 1, Map.of(), Map.of(OFFENSIVE, 1));
            check(player.getData(RpgData.PROFESSIONS).equals(empty), "Cross-track, unavailable ranks and stale catalogues cannot spend points");
            ClassResources.markCombat(player);
            ProfessionAllocations.apply(player, LOGGING, revision, Map.of(), Map.of(OFFENSIVE, 1));
            check(player.getData(RpgData.PROFESSIONS).equals(empty), "Classless combat blocks Apply without spending");
            player.removeData(RpgData.SKILL_RUNTIME);
            ProfessionAllocations.apply(player, LOGGING, revision, Map.of(), Map.of(OFFENSIVE, 1));
            var paid = player.getData(RpgData.PROFESSIONS);
            check(logging(player).rank(OFFENSIVE) == 1 && logging(player).spentPoints() == 1, "Classless point purchase succeeds");
            ProfessionAllocations.apply(player, LOGGING, revision, Map.of(), Map.of(OFFENSIVE, 2));
            ProfessionAllocations.apply(player, LOGGING, revision, Map.of(OFFENSIVE, new ProfessionState.Allocation(1, 2)), Map.of(OFFENSIVE, 2));
            ProfessionAllocations.apply(player, LOGGING, revision, logging(player).allocations(), Map.of());
            check(player.getData(RpgData.PROFESSIONS).equals(paid), "Replays, forged costs and committed refunds are rejected");
            ProfessionAllocations.apply(player, LOGGING, revision, logging(player).allocations(), Map.of(OFFENSIVE, 2));
            check(logging(player).rank(OFFENSIVE) == 2 && logging(player).spentPoints() == 2, "Next rank pays only its incremental cost");
            var unknown = ProfessionData.id("logging/removed_ability");
            var reserved = new ProfessionState.Track(5, 0, Map.of(unknown, new ProfessionState.Allocation(1, 5)));
            player.setData(RpgData.PROFESSIONS, empty.with(LOGGING, reserved));
            ProfessionAllocations.apply(player, LOGGING, revision, reserved.allocations(), Map.of(unknown, 1, OFFENSIVE, 1));
            check(logging(player).equals(reserved), "Removed content's historical reservation cannot be spent twice");
            helper.succeed();
        }
    }
    static void packets(GameTestHelper helper) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var request = new ProfessionNetworking.Apply(LOGGING, 1, Map.of(OFFENSIVE, new ProfessionState.Allocation(1, 1)), Map.of(OFFENSIVE, 2));
            ProfessionNetworking.Apply.STREAM_CODEC.encode(buffer, request);
            check(ProfessionNetworking.Apply.STREAM_CODEC.decode(buffer).equals(request), "Purchase intent round trips");
            buffer.clear(); buffer.writeIdentifier(LOGGING); buffer.writeVarLong(1); buffer.writeVarInt(129);
            rejects(() -> ProfessionNetworking.Apply.STREAM_CODEC.decode(buffer), "Oversized expected maps rejected before allocation");
            buffer.clear(); buffer.writeIdentifier(LOGGING); buffer.writeVarLong(1); buffer.writeVarInt(0); buffer.writeVarInt(1); buffer.writeIdentifier(OFFENSIVE); buffer.writeVarInt(65);
            rejects(() -> ProfessionNetworking.Apply.STREAM_CODEC.decode(buffer), "Out-of-range wire ranks rejected");
            buffer.clear(); buffer.writeIdentifier(LOGGING); buffer.writeVarLong(1); buffer.writeVarInt(2);
            for (int i = 0; i < 2; i++) { buffer.writeIdentifier(OFFENSIVE); buffer.writeVarInt(1); buffer.writeVarInt(1); }
            rejects(() -> ProfessionNetworking.Apply.STREAM_CODEC.decode(buffer), "Duplicate wire keys rejected");
            var runtime = new SkillRuntime();
            check(runtime.acceptRequest(100, request) && !runtime.acceptRequest(100, request), "Shared intent budget suppresses duplicate profession packets");
            helper.succeed();
        } finally { buffer.release(); }
    }
    static void damage(GameTestHelper helper) {
        try (var f = new ProgressionGameTests.Fixture(helper)) {
            var player = f.player(null); level(player, LOGGING, 20);
            ProfessionAllocations.apply(player, LOGGING, revision(player), Map.of(), Map.of(OFFENSIVE, 5));
            var melee = f.mob(EntityType.COW); float before = melee.getHealth(); f.damage(melee, player, 4);
            near(before - melee.getHealth(), 4.4, "Native player damage uses current purchased bonus once");
            var arrow = EntityType.ARROW.create(helper.getLevel(), EntitySpawnReason.COMMAND); arrow.setOwner(player);
            var ranged = f.mob(EntityType.COW); before = ranged.getHealth();
            ranged.hurtServer(helper.getLevel(), ranged.damageSources().arrow(arrow, player), 4);
            near(before - ranged.getHealth(), 4.4, "Native attributed projectile damage receives the bonus");
            var spell = f.mob(EntityType.COW); before = spell.getHealth();
            var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
            var spellType = net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, ProfessionData.id("fire_bolt"));
            spell.hurtServer(helper.getLevel(), new DamageSource(registry.getOrThrow(spellType), player), 4);
            near(before - spell.getHealth(), 4.4, "Player-attributed skill damage receives the bonus");
            var excluded = f.mob(EntityType.COW); before = excluded.getHealth();
            var excludedType = net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("covenant_test", "unscaled_profession"));
            excluded.hurtServer(helper.getLevel(), new DamageSource(registry.getOrThrow(excludedType), player), 4);
            near(before - excluded.getHealth(), 4, "Data-tagged sources can exclude future summon damage");
            var pet = f.mob(EntityType.WOLF); var target = f.mob(EntityType.COW); before = target.getHealth();
            var holder = helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MOB_ATTACK);
            target.hurtServer(helper.getLevel(), new DamageSource(holder, pet, player), 4);
            near(before - target.getHealth(), 4, "Attributed owned-creature sources do not inherit the bonus");
            player.removeData(RpgData.SKILL_RUNTIME);
            check(ProfessionAllocations.respec(player), "Valid respec removes mastery");
            var after = f.mob(EntityType.COW); before = after.getHealth(); f.damage(after, player, 4);
            near(before - after.getHealth(), 4, "Respec removes damage bonus without cached stats");
            arrow.discard(); helper.succeed();
        }
    }
    static void respec(GameTestHelper helper) {
        var f = new ProgressionGameTests.Fixture(helper);
        try {
            var player = f.player(PlayerClass.STALKER);
            SkillLoadouts.apply(player, Map.of(), Map.of(SkillDefinition.PIERCING_SHOT.id(), 1));
            var combat = player.getData(RpgData.SKILL_LOADOUT); var profile = PlayerProfiles.get(player);
            var tracks = new HashMap<Identifier, ProfessionState.Track>();
            for (var id : ProgressionData.PROFESSIONS) {
                tracks.put(id, new ProfessionState.Track(10, 37, Map.of(ProfessionData.id(id.getPath() + "/saved_ability"), new ProfessionState.Allocation(1, 2))));
            }
            var original = new ProfessionState(tracks); player.setData(RpgData.PROFESSIONS, original);
            var stack = new ItemStack(ProfessionRespecItem.ITEM.get(), 2); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND); player.stopUsingItem();
            check(stack.getCount() == 2 && player.getData(RpgData.PROFESSIONS).equals(original), "Early release spends nothing");
            stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND); ClassResources.markCombat(player);
            stack.getItem().onUseTick(player.level(), player, stack, 30);
            stack.getItem().finishUsingItem(stack, player.level(), player);
            check(!player.isUsingItem() && stack.getCount() == 2 && player.getData(RpgData.PROFESSIONS).equals(original), "Combat interrupts and invalid completion cannot consume/refund");
            player.removeData(RpgData.SKILL_RUNTIME); stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            helper.onEachTick(player::doTick);
            helper.runAfterDelay(42, () -> {
                try {
                    check(stack.getCount() == 1, "Native two-second held use consumes exactly one item");
                    for (var track : player.getData(RpgData.PROFESSIONS).tracks().values()) {
                        check(track.level() == 10 && track.experience() == 37 && track.allocations().isEmpty() && track.points() == 10, "All four tracks refund paid points and retain level/XP");
                    }
                    check(player.getData(RpgData.SKILL_LOADOUT).equals(combat) && PlayerProfiles.get(player).equals(profile), "Combat progression/loadout unaffected");
                    stack.getItem().finishUsingItem(stack, player.level(), player);
                    check(stack.getCount() == 1, "Empty/reset replay cannot consume another item");
                    helper.succeed();
                } finally { f.close(); }
            });
        } catch (RuntimeException | Error error) { f.close(); throw error; }
    }
    static void catalogue(GameTestHelper helper) {
        try (var reader = helper.getLevel().getServer().getResourceManager().getResourceOrThrow(ProfessionData.FILE).openAsReader()) {
            var original = JsonParser.parseReader(reader).getAsJsonObject();
            var data = ProfessionData.parse(original);
            check(data.abilities().size() == 17 && data.abilities().values().stream().filter(ProfessionData.Node::implemented).count() == 5, "Seventeen authored nodes; Logging mastery and four wired Crafting nodes can be purchased");
            check(data.activityRewards().get(ProfessionData.id("farming/breeding")) == 100
                    && data.activityRewards().get(ProfessionData.id("crafting/enchant_option_3")) == 100,
                    "Agreed gathering and enchanting rewards are authored");
            check(data.harvesting().ores().size() == 10 && data.harvesting().crops().size() == 5
                    && data.harvesting().trees().size() == 9, "Initial harvest mappings are loaded with the catalogue");
            var badCrop = original.deepCopy();
            badCrop.getAsJsonObject("harvesting").getAsJsonArray("crops").get(0).getAsJsonObject().addProperty("mature_age", 8);
            rejects(() -> ProfessionData.parse(badCrop), "Invalid crop maturity rejects the complete catalogue");
            var missingReward = original.deepCopy();
            missingReward.getAsJsonObject("activity_rewards").remove("covenant:mining/iron");
            rejects(() -> ProfessionData.parse(missingReward), "Ore mappings cannot silently reference missing activity rewards");
            var invalid = original.deepCopy(); invalid.getAsJsonArray("abilities").get(0).getAsJsonObject().getAsJsonArray("ranks").get(0).getAsJsonObject().addProperty("cost", 0);
            final var zero = invalid; rejects(() -> ProfessionData.parse(zero), "Free/negative rank costs rejected");
            invalid = original.deepCopy(); invalid.getAsJsonArray("abilities").get(0).getAsJsonObject().getAsJsonArray("ranks").get(0).getAsJsonObject().addProperty("cost", 1.5);
            final var fractional = invalid; rejects(() -> ProfessionData.parse(fractional), "Fractional rank costs rejected without truncation");
            invalid = original.deepCopy(); invalid.getAsJsonArray("abilities").add(invalid.getAsJsonArray("abilities").get(0).deepCopy());
            final var duplicate = invalid; rejects(() -> ProfessionData.parse(duplicate), "Duplicate ability IDs rejected");
            invalid = original.deepCopy(); invalid.getAsJsonArray("abilities").get(0).getAsJsonObject().addProperty("prerequisite", "covenant:mining/prospecting");
            final var cycle = invalid; rejects(() -> ProfessionData.parse(cycle), "Cyclic prerequisites rejected");
            invalid = original.deepCopy(); invalid.getAsJsonArray("abilities").get(0).getAsJsonObject().addProperty("prerequisite", "covenant:logging/tree_felling");
            final var cross = invalid; rejects(() -> ProfessionData.parse(cross), "Cross-profession prerequisites rejected");
            helper.succeed();
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
    static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
    private static void near(double actual, double expected, String message) { check(Math.abs(actual - expected) < .0001, message + ": " + actual + " != " + expected); }
    private static void rejects(Runnable action, String message) { try { action.run(); } catch (RuntimeException expected) { return; } throw new IllegalStateException(message); }
}
