package dev.covenant.test;

import com.mojang.serialization.Codec;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.covenant.rpg.CharacterProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.equipment.Affix;
import dev.covenant.rpg.equipment.AffixPage;
import dev.covenant.rpg.equipment.AffixPages;
import dev.covenant.rpg.equipment.EquipmentCategory;
import dev.covenant.rpg.equipment.PageProgression;
import dev.covenant.rpg.equipment.RpgModifier;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Runs only in runGameTestServer; no test classes, fixtures, or test mod ship in the production JAR. */
@Mod("covenant_test")
public final class RpgGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, "covenant_test");

    static {
        FUNCTIONS.register("profile_lifecycle", () -> RpgGameTests::profileLifecycle);
        FUNCTIONS.register("item_lifecycle", () -> RpgGameTests::itemLifecycle);
        FUNCTIONS.register("stack_safety", () -> RpgGameTests::stackSafety);
        FUNCTIONS.register("data_validation", () -> RpgGameTests::dataValidation);
    }

    public RpgGameTests(IEventBus modBus) { FUNCTIONS.register(modBus); }

    @SuppressWarnings("removal") // Pinned vanilla helper supplies an embedded connection for server-player lifecycles.
    private static void profileLifecycle(GameTestHelper helper) {
        List<ServerPlayer> players = new ArrayList<>();
        try {
            ServerPlayer original = helper.makeMockServerPlayerInLevel();
            players.add(original);
            check(PlayerProfiles.get(original).equals(CharacterProfile.UNINITIALIZED), "New player must be uninitialized");
            expectFailure(() -> PlayerProfiles.update(original, p -> p));
            ServerPlayer loaded = helper.makeMockServerPlayerInLevel();
            players.add(loaded);
            restore(helper, original, loaded);
            check(PlayerProfiles.get(loaded).equals(CharacterProfile.UNINITIALIZED), "Uninitialized save/load");

            PlayerProfiles.initialize(original, id("mage"), Optional.of(id("mana")), 100);
            expectFailure(() -> PlayerProfiles.initialize(original, id("mage"), Optional.empty(), 0));
            CharacterProfile progressed = new CharacterProfile(true, 7, 123456789012L,
                    Optional.of(id("mage")), Optional.of(id("frost")), Optional.of(id("mana")), 42.5, 140);
            PlayerProfiles.update(original, p -> progressed);
            restore(helper, original, loaded);
            check(PlayerProfiles.get(loaded).equals(progressed), "Player entity persistence must include every profile field");
            ItemStack gear = new ItemStack(Items.IRON_CHESTPLATE);
            AffixPage gearPage = AffixPages.attach(helper.getLevel(), gear,
                    EquipmentCategory.BODY_ARMOR, modifiers(10), affixes());
            original.getInventory().setItem(0, gear);
            var playerList = helper.getLevel().getServer().getPlayerList();
            playerList.saveAll();
            var disk = playerList.loadPlayerData(original.nameAndId()).orElseThrow();
            disk.remove("UUID"); // Keep the registered test recipient's entity identity intact.
            var reporter = new ProblemReporter.Collector();
            loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
            check(reporter.isEmpty(), "Disk player data must decode without errors");
            check(PlayerProfiles.get(loaded).equals(progressed), "Profile must survive actual player-file persistence");
            checkPage(loaded.getInventory().getItem(0), gearPage);
            check(roundTrip(CharacterProfile.CODEC, progressed).equals(progressed), "Profile codec");
            check(network(helper, CharacterProfile.STREAM_CODEC, progressed).equals(progressed), "Profile network codec");
            check(network(helper, CharacterProfile.STREAM_CODEC, CharacterProfile.UNINITIALIZED)
                    .equals(CharacterProfile.UNINITIALIZED), "Uninitialized profile network codec");

            ServerPlayer respawned = helper.makeMockServerPlayerInLevel();
            players.add(respawned);
            respawned.restoreFrom(original, false);
            check(PlayerProfiles.get(respawned).equals(progressed), "Death clone must preserve progression exactly once");
            ServerPlayer endReturn = helper.makeMockServerPlayerInLevel();
            players.add(endReturn);
            endReturn.restoreFrom(respawned, true);
            check(PlayerProfiles.get(endReturn).equals(progressed), "End return clone must not add or reset progression");
            PlayerProfiles.update(endReturn, p -> new CharacterProfile(true, 8, 0, p.selectedClass(),
                    p.specialization(), p.resourceType(), 1, p.maximumResource()));
            check(PlayerProfiles.get(original).equals(progressed), "Clone updates must not mutate original state");
            helper.succeed();
        } finally {
            players.forEach(player -> helper.getLevel().getServer().getPlayerList().remove(player));
        }
    }

    private static void itemLifecycle(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStack first = new ItemStack(Items.IRON_CHESTPLATE);
        ItemStack second = new ItemStack(Items.IRON_CHESTPLATE);
        AffixPage page = AffixPages.attach(level, first, EquipmentCategory.BODY_ARMOR, modifiers(8), affixes());
        AffixPage other = AffixPages.attach(level, second, EquipmentCategory.BODY_ARMOR, modifiers(27), List.of());
        check(!page.identity().equals(other.identity()), "Identical items need independent page identities");
        check(!ItemStack.isSameItemSameComponents(first, second), "Different pages must not merge");

        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        var recipient = helper.makeMockPlayer(GameType.SURVIVAL);
        owner.getInventory().setItem(0, first);
        owner.getInventory().setItem(5, owner.getInventory().removeItem(0, 1));
        checkPage(owner.getInventory().getItem(5), page);
        owner.setItemSlot(EquipmentSlot.CHEST, owner.getInventory().removeItem(5, 1));
        checkPage(owner.getItemBySlot(EquipmentSlot.CHEST), page);
        check(PlayerProfiles.get(owner).equals(CharacterProfile.UNINITIALIZED), "Equipping must not write equipment data to profile");
        ItemStack unequipped = owner.getItemBySlot(EquipmentSlot.CHEST);
        owner.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        checkPage(unequipped, page);

        SimpleContainer chest = new SimpleContainer(27);
        chest.setItem(3, unequipped);
        ItemStack fromChest = chest.removeItem(3, 1);
        check(chest.getItem(3).isEmpty(), "Container transfer must consume source");
        checkPage(fromChest, page);
        ItemEntity dropped = new ItemEntity(level, 0, 100, 0, fromChest);
        var reporter = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(reporter, level.registryAccess());
        dropped.saveWithoutId(output);
        ItemEntity restored = new ItemEntity(level, 0, 100, 0, ItemStack.EMPTY);
        restored.load(TagValueInput.create(reporter, level.registryAccess(), output.buildResult()));
        checkPage(restored.getItem(), page);
        restored.setNoPickUpDelay();
        restored.playerTouch(recipient);
        check(restored.isRemoved(), "Pickup must consume dropped entity");
        checkPage(recipient.getInventory().getItem(0), page);
        check(PlayerProfiles.get(recipient).equals(CharacterProfile.UNINITIALIZED), "Transfer must not alter permanent progression");
        checkPage(second, other);
        check(reporter.isEmpty(), "Entity save/load reported serialization problems: " + reporter.getReport());

        ItemStack saved = roundTripStack(helper, recipient.getInventory().getItem(0));
        checkPage(saved, page);
        checkPage(network(helper, ItemStack.STREAM_CODEC, saved), page);
        check(saved.getMaxStackSize() == 1, "Save/network must retain singleton behavior");
        helper.succeed();
    }

    private static void stackSafety(GameTestHelper helper) {
        var level = helper.getLevel();
        // Test-only tag opts a naturally stackable item into a custom category, like a modded ring.
        var custom = new EquipmentCategory(Identifier.fromNamespaceAndPath("covenant_test", "charm"));
        check(custom.supports(new ItemStack(Items.PAPER)), "Custom tag category must be extensible");
        ItemStack stacked = new ItemStack(Items.PAPER, 2);
        expectFailure(() -> AffixPages.attach(level, stacked, custom, List.of(), List.of()));
        check(stacked.getCount() == 2 && stacked.getMaxStackSize() == 64 && AffixPages.get(stacked).isEmpty(),
                "Rejected attachment must leave stack intact");
        ItemStack item = new ItemStack(Items.PAPER);
        AffixPage page = AffixPages.attach(level, item, custom, modifiers(2), affixes());
        check(item.getMaxStackSize() == 1 && !item.isStackable(), "Paged equipment must be unstackable");
        expectFailure(() -> AffixPages.attach(level, item, custom, List.of(), List.of()));
        ItemStack snapshot = item.copy();
        checkPage(snapshot, page);
        ItemStack zeroSplit = item.split(0);
        check(zeroSplit.isEmpty(), "Zero split must produce no live page item");
        checkPage(item, page);
        ItemStack split = item.split(64);
        check(item.isEmpty() && split.getCount() == 1, "Split must move the sole item, never create two live items");
        checkPage(split, page);
        AffixPages.update(level, split, modifiers(15), List.of(), new PageProgression(3, 50));
        AffixPage changed = AffixPages.get(split).orElseThrow();
        check(changed.identity().equals(page.identity()) && changed.category().equals(custom), "Updates must retain identity/category");
        checkPage(snapshot, page);
        checkPage(roundTripStack(helper, split), changed);
        check(roundTripStack(helper, split).getMaxStackSize() == 1, "Stackable item's max-size override must persist");
        check(!new ItemStack(Items.PAPER).has(DataComponents.MAX_STACK_SIZE)
                        || new ItemStack(Items.PAPER).getMaxStackSize() == 64, "Item defaults must not change");
        expectFailure(() -> AffixPages.attach(level, new ItemStack(Items.DIRT), EquipmentCategory.BODY_ARMOR, List.of(), List.of()));
        expectFailure(() -> AffixPages.attach(level, ItemStack.EMPTY, custom, List.of(), List.of()));
        helper.succeed();
    }

    private static void dataValidation(GameTestHelper helper) {
        expectFailure(() -> new CharacterProfile(true, 0, 0, Optional.of(id("mage")), Optional.empty(), Optional.empty(), 0, 0));
        expectFailure(() -> new CharacterProfile(false, 1, 0, Optional.empty(), Optional.empty(), Optional.empty(), 0, 0));
        expectFailure(() -> CharacterProfile.create(id("mage"), Optional.of(id("mana")), Double.NaN));
        expectFailure(() -> new CharacterProfile(true, 1, -1, Optional.of(id("mage")), Optional.empty(), Optional.empty(), 0, 0));
        expectFailure(() -> new CharacterProfile(true, 1, 0, Optional.of(id("mage")), Optional.empty(), Optional.of(id("mana")), 101, 100));
        expectFailure(() -> new RpgModifier(id("power"), RpgModifier.Operation.ADD, Double.POSITIVE_INFINITY));
        expectFailure(() -> new PageProgression(-1, 0));
        var mutable = new ArrayList<>(modifiers(5));
        Affix affix = new Affix(id("strong"), id("rare"), 2, mutable);
        mutable.clear();
        check(affix.modifiers().size() == 2, "Nested modifier lists must be immutable snapshots");
        expectFailure(() -> affix.modifiers().clear());
        check(roundTrip(Affix.CODEC, affix).equals(affix), "Affix codec must preserve multiple independent modifiers");
        check(CharacterProfile.create(id("warrior"), Optional.empty(), 0).created(), "Resource-less classes are valid");
        var invalidProfile = JsonParser.parseString("""
                {"created":true,"level":0,"experience":0,"class":"covenant:mage",
                 "current_resource":0,"maximum_resource":0}
                """);
        check(CharacterProfile.CODEC.parse(JsonOps.INSTANCE, invalidProfile).isError(),
                "Malformed creation state must return a codec error");
        helper.succeed();
    }

    private static List<RpgModifier> modifiers(double amount) {
        return List.of(new RpgModifier(id("power"), RpgModifier.Operation.ADD, amount),
                new RpgModifier(id("vitality"), RpgModifier.Operation.MULTIPLY_BASE, 0.1));
    }

    private static List<Affix> affixes() {
        return List.of(new Affix(id("strong"), id("rare"), 2, modifiers(3)),
                new Affix(id("sturdy"), id("common"), 1, modifiers(7)));
    }

    private static void restore(GameTestHelper helper, ServerPlayer source, ServerPlayer target) {
        var reporter = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(reporter, helper.getLevel().registryAccess());
        source.saveWithoutId(output);
        var saved = output.buildResult();
        saved.remove("UUID"); // Both test players are already registered under different UUIDs.
        target.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), saved));
        check(reporter.isEmpty(), "Player save/load reported problems: " + reporter.getReport());
    }

    private static ItemStack roundTripStack(GameTestHelper helper, ItemStack stack) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var encoded = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        return ItemStack.CODEC.parse(ops, encoded).getOrThrow();
    }

    private static <T> T roundTrip(Codec<T> codec, T value) {
        return codec.parse(NbtOps.INSTANCE, codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow()).getOrThrow();
    }

    private static <T> T network(GameTestHelper helper, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, T value) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            codec.encode(buffer, value);
            T decoded = codec.decode(buffer);
            check(!buffer.isReadable(), "Network decoder must consume all bytes");
            return decoded;
        } finally {
            buffer.release();
        }
    }

    private static void checkPage(ItemStack stack, AffixPage expected) {
        check(!stack.isEmpty() && AffixPages.get(stack).orElseThrow().equals(expected), "Item must retain its exact Affix Page");
    }

    private static void expectFailure(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException expected) { return; }
        throw new AssertionError("Expected operation to be rejected");
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("covenant", path); }
}
