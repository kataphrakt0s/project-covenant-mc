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
import dev.covenant.rpg.stats.RpgStats;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Runs only in runGameTestServer; no test classes, fixtures, or test mod ship in the production JAR. */
public final class RpgGameTests {
    @SuppressWarnings("removal") // Pinned vanilla helper supplies an embedded connection for server-player lifecycles.
    static void profileLifecycle(GameTestHelper helper) {
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

    static void itemLifecycle(GameTestHelper helper) {
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

    static void stackSafety(GameTestHelper helper) {
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

    static void dataValidation(GameTestHelper helper) {
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

    static void statEquipment(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        near(player.getAttributeValue(RpgStats.HEALTH), 20, "Base health");
        near(player.getAttributeValue(RpgStats.DAMAGE), 1, "Base attack damage");
        near(player.getAttributeValue(RpgStats.ARMOR), 0, "Base armor");
        near(player.getAttributeValue(RpgStats.MOVEMENT_SPEED), 0.1, "Base movement speed");
        near(player.getAttributeValue(RpgStats.ATTACK_SPEED), 4, "Base attack speed");
        near(player.getAttributeValue(RpgStats.DODGE), 0, "Base dodge");
        check(RpgStats.DAMAGE.value().isClientSyncable(), "Attack damage must sync too");

        ItemStack first = new ItemStack(Items.IRON_CHESTPLATE);
        var page = AffixPages.attach(helper.getLevel(), first, EquipmentCategory.BODY_ARMOR,
                List.of(stat(RpgStats.HEALTH, RpgModifier.Operation.ADD, 4),
                        stat(RpgStats.HEALTH, RpgModifier.Operation.ADD, 4),
                        stat(RpgStats.HEALTH, RpgModifier.Operation.MULTIPLY_BASE, 0.25),
                        stat(RpgStats.HEALTH, RpgModifier.Operation.MULTIPLY_TOTAL, 0.1),
                        stat(RpgStats.ARMOR, RpgModifier.Operation.ADD, 3),
                        stat(RpgStats.MOVEMENT_SPEED, RpgModifier.Operation.MULTIPLY_BASE, 0.2),
                        new RpgModifier(id("unknown_future_stat"), RpgModifier.Operation.ADD, 99)),
                List.of(new Affix(id("healthy"), id("common"), 1,
                        List.of(stat(RpgStats.HEALTH, RpgModifier.Operation.MULTIPLY_TOTAL, 0.2)))));
        player.getInventory().setItem(5, first);
        player.tick();
        near(player.getAttributeValue(RpgStats.HEALTH), 20, "Inventory gear must not contribute");
        first = player.getInventory().removeItem(5, 1);
        player.setItemSlot(EquipmentSlot.MAINHAND, first);
        player.tick();
        near(player.getAttributeValue(RpgStats.HEALTH), 20, "Chest page must not work in hand");
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.CHEST, first);
        player.tick();
        near(player.getAttributeValue(RpgStats.HEALTH), 46.2, "All three operations and independent affix modifiers");
        near(player.getAttributeValue(RpgStats.ARMOR), 9, "Vanilla chest armor plus page armor");
        near(player.getAttributeValue(RpgStats.MOVEMENT_SPEED), 0.12, "Movement percent modifier");
        for (int i = 0; i < 10; i++) {
            first.getAttributeModifiers();
            player.tick();
            near(player.getAttributeValue(RpgStats.HEALTH), 46.2, "Repeated queries/ticks must not compound");
        }

        ItemStack weapon = new ItemStack(Items.IRON_SWORD);
        player.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        player.tick();
        double weaponDamage = player.getAttributeValue(RpgStats.DAMAGE);
        double weaponSpeed = player.getAttributeValue(RpgStats.ATTACK_SPEED);
        AffixPages.attach(helper.getLevel(), weapon, EquipmentCategory.MAIN_HAND,
                List.of(stat(RpgStats.DAMAGE, RpgModifier.Operation.ADD, 2),
                        stat(RpgStats.ATTACK_SPEED, RpgModifier.Operation.ADD, 0.5)), List.of());
        player.tick();
        near(player.getAttributeValue(RpgStats.DAMAGE), weaponDamage + 2, "Page adds to native weapon damage");
        near(player.getAttributeValue(RpgStats.ATTACK_SPEED), weaponSpeed + 0.5, "Native attack speed");
        near(player.getCurrentItemAttackStrengthDelay(), 20 / (weaponSpeed + 0.5), "Native attack recharge uses stat");

        ItemStack second = new ItemStack(Items.IRON_CHESTPLATE);
        AffixPages.attach(helper.getLevel(), second, EquipmentCategory.BODY_ARMOR,
                List.of(stat(RpgStats.HEALTH, RpgModifier.Operation.ADD, 2)), List.of());
        player.setItemSlot(EquipmentSlot.CHEST, second);
        player.tick();
        near(player.getAttributeValue(RpgStats.HEALTH), 22, "Identical item with a different page replaces contribution");
        near(player.getAttributeValue(RpgStats.MOVEMENT_SPEED), 0.1, "Old page attributes are removed");
        AffixPages.update(helper.getLevel(), second,
                List.of(stat(RpgStats.HEALTH, RpgModifier.Operation.ADD, 10)), List.of(), PageProgression.NONE);
        player.tick();
        near(player.getAttributeValue(RpgStats.HEALTH), 30, "In-place component updates replace old modifiers");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.tick();
        near(player.getAttributeValue(RpgStats.HEALTH), 20, "Unequip health");
        near(player.getAttributeValue(RpgStats.ARMOR), 0, "Unequip armor");
        near(player.getAttributeValue(RpgStats.DAMAGE), 1, "Unequip weapon");
        near(player.getAttributeValue(RpgStats.ATTACK_SPEED), 4, "Unequip attack speed");

        var recipient = helper.makeMockPlayer(GameType.SURVIVAL);
        SimpleContainer container = new SimpleContainer(9);
        container.setItem(0, first);
        recipient.setItemSlot(EquipmentSlot.CHEST, container.removeItem(0, 1));
        recipient.tick();
        checkPage(recipient.getItemBySlot(EquipmentSlot.CHEST), page);
        near(recipient.getAttributeValue(RpgStats.HEALTH), 46.2, "Transferred page contributes to recipient");
        check(PlayerProfiles.get(recipient).equals(CharacterProfile.UNINITIALIZED), "No equipment stats in profile");
        var receivedGear = recipient.getItemBySlot(EquipmentSlot.CHEST);
        receivedGear.setDamageValue(receivedGear.getMaxDamage());
        recipient.tick();
        near(recipient.getAttributeValue(RpgStats.HEALTH), 20, "Broken equipment stops contributing");
        helper.succeed();
    }

    static void skillStats(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (var coefficient : List.of(RpgStats.RESOURCE_COST, RpgStats.COOLDOWN,
                RpgStats.CAST_TIME, RpgStats.SKILL_DURATION)) {
            near(RpgStats.scaleSkillValue(player, coefficient, 10), 10, "Neutral skill coefficient");
            check(coefficient.value().isClientSyncable(), "Skill attribute must sync");
        }
        ItemStack gear = new ItemStack(Items.IRON_HELMET);
        AffixPages.attach(helper.getLevel(), gear, EquipmentCategory.HEAD_ARMOR,
                List.of(stat(RpgStats.RESOURCE_COST, RpgModifier.Operation.MULTIPLY_BASE, -0.2),
                        stat(RpgStats.COOLDOWN, RpgModifier.Operation.MULTIPLY_TOTAL, -0.25),
                        stat(RpgStats.CAST_TIME, RpgModifier.Operation.ADD, -0.5),
                        stat(RpgStats.SKILL_DURATION, RpgModifier.Operation.MULTIPLY_BASE, 0.5)), List.of());
        player.setItemSlot(EquipmentSlot.HEAD, gear);
        player.tick();
        near(RpgStats.scaleSkillValue(player, RpgStats.RESOURCE_COST, 100), 80, "Resource cost reduction");
        near(RpgStats.scaleSkillValue(player, RpgStats.RESOURCE_COST, 25), 20, "Different resource/base cost");
        near(RpgStats.scaleSkillValue(player, RpgStats.COOLDOWN, 40), 30, "Cooldown reduction");
        near(RpgStats.scaleSkillValue(player, RpgStats.CAST_TIME, 3), 1.5, "Cast time reduction");
        near(RpgStats.scaleSkillValue(player, RpgStats.SKILL_DURATION, 8), 12, "Duration increase");
        near(RpgStats.scaleSkillValue(player, RpgStats.CAST_TIME, 0), 0, "Zero base stays zero");
        var external = new AttributeModifier(id("test_external_source"), 0.5, AttributeModifier.Operation.ADD_VALUE);
        player.getAttribute(RpgStats.RESOURCE_COST).addTransientModifier(external);
        near(RpgStats.scaleSkillValue(player, RpgStats.RESOURCE_COST, 100), 120, "External native modifier combines with page");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.tick();
        near(RpgStats.scaleSkillValue(player, RpgStats.RESOURCE_COST, 100), 150, "Unequip preserves external source");
        player.getAttribute(RpgStats.RESOURCE_COST).removeModifier(external);
        for (var coefficient : List.of(RpgStats.RESOURCE_COST, RpgStats.COOLDOWN,
                RpgStats.CAST_TIME, RpgStats.SKILL_DURATION)) {
            near(RpgStats.scaleSkillValue(player, coefficient, 10), 10, "Unequip resets skill coefficient");
        }
        player.getAttribute(RpgStats.CAST_TIME).setBaseValue(-2);
        near(RpgStats.scaleSkillValue(player, RpgStats.CAST_TIME, 3), 0, "Negative coefficient clamps to zero");
        expectFailure(() -> RpgStats.scaleSkillValue(player, RpgStats.COOLDOWN, -1));
        expectFailure(() -> RpgStats.scaleSkillValue(player, RpgStats.COOLDOWN, Double.NaN));
        helper.succeed();
    }

    @SuppressWarnings("removal")
    static void statPersistence(GameTestHelper helper) {
        List<ServerPlayer> players = new ArrayList<>();
        try {
            var source = helper.makeMockServerPlayerInLevel();
            players.add(source);
            ItemStack gear = new ItemStack(Items.IRON_CHESTPLATE);
            var page = AffixPages.attach(helper.getLevel(), gear, EquipmentCategory.BODY_ARMOR,
                    List.of(stat(RpgStats.HEALTH, RpgModifier.Operation.ADD, 6),
                            stat(RpgStats.DODGE, RpgModifier.Operation.ADD, 0.25),
                            stat(RpgStats.RESOURCE_COST, RpgModifier.Operation.MULTIPLY_BASE, -0.2)), List.of());
            source.setItemSlot(EquipmentSlot.CHEST, gear);
            source.doTick();
            near(source.getAttributeValue(RpgStats.HEALTH), 26, "Equipped before save");
            source.setHealth(26);
            var list = helper.getLevel().getServer().getPlayerList();
            list.saveAll();
            var disk = list.loadPlayerData(source.nameAndId()).orElseThrow();
            disk.remove("UUID");
            var loaded = helper.makeMockServerPlayerInLevel();
            players.add(loaded);
            var reporter = new ProblemReporter.Collector();
            loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
            check(reporter.isEmpty(), "Stat player-file decoding");
            loaded.doTick();
            checkPage(loaded.getItemBySlot(EquipmentSlot.CHEST), page);
            near(loaded.getAttributeValue(RpgStats.HEALTH), 26, "File reload reconstructs equipment once");
            near(loaded.getHealth(), 26, "Current health above unequipped maximum survives reload");
            near(loaded.getAttributeValue(RpgStats.DODGE), 0.25, "Dodge reload");
            near(RpgStats.scaleSkillValue(loaded, RpgStats.RESOURCE_COST, 50), 40, "Skill reload");
            loaded.doTick();
            near(loaded.getAttributeValue(RpgStats.HEALTH), 26, "Reload must not compound");
            source.setHealth(23.5F);
            var injured = helper.makeMockServerPlayerInLevel();
            players.add(injured);
            restore(helper, source, injured);
            injured.doTick();
            near(injured.getHealth(), 23.5, "Injured health above base maximum reloads without healing");
            var endReturn = helper.makeMockServerPlayerInLevel();
            players.add(endReturn);
            endReturn.restoreFrom(source, true);
            endReturn.doTick();
            near(endReturn.getAttributeValue(RpgStats.HEALTH), 26, "End clone reconstructs retained equipment");
            near(endReturn.getHealth(), 23.5, "End return preserves current health above base maximum");
            var died = helper.makeMockServerPlayerInLevel();
            players.add(died);
            died.restoreFrom(source, false);
            died.doTick();
            near(died.getAttributeValue(RpgStats.HEALTH), 20, "Death without retained gear has no stale bonus");
            near(died.getAttributeValue(RpgStats.DODGE), 0, "Death removes equipment dodge");
            var rules = helper.getLevel().getGameRules();
            boolean keepInventory = rules.get(GameRules.KEEP_INVENTORY);
            try {
                rules.set(GameRules.KEEP_INVENTORY, true, helper.getLevel().getServer());
                var retained = helper.makeMockServerPlayerInLevel();
                players.add(retained);
                retained.restoreFrom(source, false);
                retained.doTick();
                near(retained.getAttributeValue(RpgStats.HEALTH), 26, "Retained equipment is reconstructed after death");
                near(retained.getHealth(), 26, "Death with retained gear starts at effective full health");
            } finally {
                rules.set(GameRules.KEEP_INVENTORY, keepInventory, helper.getLevel().getServer());
            }
            loaded.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            loaded.doTick();
            near(loaded.getAttributeValue(RpgStats.HEALTH), 20, "No saved transient health bonus after unequip");
            near(loaded.getAttributeValue(RpgStats.DODGE), 0, "No saved transient dodge bonus");
            check(PlayerProfiles.get(loaded).equals(CharacterProfile.UNINITIALIZED), "Profile stays independent");
            helper.succeed();
        } finally {
            players.forEach(player -> helper.getLevel().getServer().getPlayerList().remove(player));
        }
    }

    static void dodge(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var attacker = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack gear = new ItemStack(Items.IRON_CHESTPLATE);
        AffixPages.attach(level, gear, EquipmentCategory.BODY_ARMOR,
                List.of(stat(RpgStats.DODGE, RpgModifier.Operation.ADD, 2)), List.of());
        player.setItemSlot(EquipmentSlot.CHEST, gear);
        player.tick();
        near(player.getAttributeValue(RpgStats.DODGE), 1, "Native attribute retains its bound");
        near(RpgStats.dodgeChance(player), .85, "Effective ordinary Dodge is capped at 85 percent");
        player.getRandom().setSeed(0);
        float health = player.getHealth();
        check(!player.hurtServer(level, level.damageSources().playerAttack(attacker), 4), "Seeded capped Dodge rejects melee hit");
        near(player.getHealth(), health, "Dodge preserves health");
        check(gear.getDamageValue() == 0 && player.invulnerableTime == 0, "Dodged hit must not damage armor or add immunity");
        var arrow = EntityType.ARROW.create(level, EntitySpawnReason.COMMAND);
        player.getRandom().setSeed(0);
        check(!player.hurtServer(level, level.damageSources().arrow(arrow, attacker), 4), "Seeded capped Dodge rejects projectile");
        check(player.hurtServer(level, level.damageSources().fall(), 3), "Dodge must not prevent falling damage");
        check(player.getHealth() < health, "Environmental damage still hurts");
        player.invulnerableTime = 0;
        check(player.hurtServer(level, level.damageSources().explosion(attacker, attacker), 3), "Explosions cannot be dodged");
        player.invulnerableTime = 0;
        check(player.hurtServer(level, level.damageSources().fellOutOfWorld(), 1), "Bypass damage cannot be dodged");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.tick();
        near(player.getAttributeValue(RpgStats.DODGE), 0, "Unequip removes dodge");
        player.invulnerableTime = 0;
        check(player.hurtServer(level, level.damageSources().playerAttack(attacker), 1), "Zero dodge allows melee hit");
        helper.succeed();
    }

    private static RpgModifier stat(Holder<Attribute> attribute, RpgModifier.Operation operation, double amount) {
        return new RpgModifier(BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()), operation, amount);
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.00001, message + ": expected " + expected + ", got " + actual);
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
        if (!condition) { throw new IllegalStateException(message); }
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("covenant", path); }
}
