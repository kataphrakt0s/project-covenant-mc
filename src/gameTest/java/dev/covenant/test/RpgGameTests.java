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
import java.util.function.Consumer;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Runs only in runGameTestServer; no test classes, fixtures, or test mod ship in the production JAR. */
@Mod("covenant_test")
public final class RpgGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, "covenant_test");

    static {
        FUNCTIONS.register("feedback_volley", () -> SkillFeedbackGameTests::volley);
        FUNCTIONS.register("feedback_casting", () -> SkillFeedbackGameTests::casting);
        FUNCTIONS.register("feedback_impacts", () -> SkillFeedbackGameTests::impacts);
        FUNCTIONS.register("feedback_melee", () -> SkillFeedbackGameTests::melee);
        FUNCTIONS.register("weaver_activation", () -> WeaverGameTests::activation);
        FUNCTIONS.register("weaver_discharge_cast", () -> WeaverGameTests::dischargeCast);
        FUNCTIONS.register("weaver_discharge_mixed", () -> WeaverGameTests::dischargeMixed);
        FUNCTIONS.register("weaver_discharge_defenses", () -> WeaverGameTests::dischargeDefenses);
        FUNCTIONS.register("weaver_threefold_damage", () -> WeaverGameTests::threefoldDamage);
        FUNCTIONS.register("weaver_threefold_accounting", () -> WeaverGameTests::threefoldAccounting);
        FUNCTIONS.register("weaver_threefold_expiry", () -> WeaverGameTests::threefoldExpiry);
        FUNCTIONS.register("weaver_threefold_snapshots", () -> WeaverGameTests::threefoldSnapshots);
        FUNCTIONS.register("weaver_teleport_geometry", () -> WeaverGameTests::teleportGeometry);
        FUNCTIONS.register("weaver_teleport_blocked", () -> WeaverGameTests::teleportBlocked);
        FUNCTIONS.register("weaver_teleport_terrain", () -> WeaverGameTests::teleportTerrain);
        FUNCTIONS.register("weaver_lifecycle", () -> WeaverGameTests::lifecycle);
        FUNCTIONS.register("weaver_config", () -> WeaverGameTests::config);
        FUNCTIONS.register("weaver_discharge_threefold", () -> WeaverGameTests::dischargeThreefold);
        FUNCTIONS.register("weaver_reload", () -> WeaverGameTests::reload);
        FUNCTIONS.register("reaver_cry", () -> ReaverGameTests::cry);
        FUNCTIONS.register("reaver_cry_duration", () -> ReaverGameTests::cryDuration);
        FUNCTIONS.register("reaver_fever", () -> ReaverGameTests::fever);
        FUNCTIONS.register("reaver_fever_updates", () -> ReaverGameTests::feverUpdates);
        FUNCTIONS.register("reaver_storm_cast", () -> ReaverGameTests::stormCast);
        FUNCTIONS.register("reaver_storm_pulses", () -> ReaverGameTests::stormPulses);
        FUNCTIONS.register("reaver_storm_terrain", () -> ReaverGameTests::stormTerrain);
        FUNCTIONS.register("reaver_storm_safety", () -> ReaverGameTests::stormSafety);
        FUNCTIONS.register("reaver_config", () -> ReaverGameTests::config);
        FUNCTIONS.register("reaver_reload", () -> ReaverGameTests::reload);
        FUNCTIONS.register("itemization_rules", () -> ItemizationGameTests::rules);
        FUNCTIONS.register("itemization_reload", () -> ItemizationReloadGameTests::run);
        FUNCTIONS.register("workbench_common", () -> ItemizationGameTests::common);
        FUNCTIONS.register("workbench_upgrades", () -> ItemizationGameTests::upgrades);
        FUNCTIONS.register("workbench_salvage", () -> ItemizationGameTests::salvageBatch);
        FUNCTIONS.register("workbench_menus", () -> ItemizationGameTests::menus);
        FUNCTIONS.register("workbench_storage", () -> ItemizationGameTests::storage);
        FUNCTIONS.register("itemization_loot", () -> ItemizationGameTests::loot);
        FUNCTIONS.register("stalker_activation", () -> StalkerGameTests::activation);
        FUNCTIONS.register("stalker_disengage", () -> StalkerGameTests::disengage);
        FUNCTIONS.register("stalker_movement_lifecycle", () -> StalkerGameTests::movementLifecycle);
        FUNCTIONS.register("stalker_collision_prediction", () -> StalkerGameTests::collisionAndPrediction);
        FUNCTIONS.register("stalker_arcane", () -> StalkerGameTests::arcane);
        FUNCTIONS.register("stalker_ammunition", () -> StalkerGameTests::ammunition);
        FUNCTIONS.register("stalker_multishot", () -> StalkerGameTests::multishot);
        FUNCTIONS.register("stalker_persistence", () -> StalkerGameTests::persistence);
        FUNCTIONS.register("stalker_flight", () -> StalkerGameTests::flight);
        FUNCTIONS.register("stalker_defenses", () -> StalkerGameTests::defenses);
        FUNCTIONS.register("profile_lifecycle", () -> RpgGameTests::profileLifecycle);
        FUNCTIONS.register("item_lifecycle", () -> RpgGameTests::itemLifecycle);
        FUNCTIONS.register("stack_safety", () -> RpgGameTests::stackSafety);
        FUNCTIONS.register("data_validation", () -> RpgGameTests::dataValidation);
        FUNCTIONS.register("stat_equipment", () -> RpgGameTests::statEquipment);
        FUNCTIONS.register("skill_stats", () -> RpgGameTests::skillStats);
        FUNCTIONS.register("stat_persistence", () -> RpgGameTests::statPersistence);
        FUNCTIONS.register("dodge", () -> RpgGameTests::dodge);
        FUNCTIONS.register("class_foundation", () -> ClassGameTests::run);
        FUNCTIONS.register("stat_commands", () -> StatCommandGameTests::run);
        FUNCTIONS.register("affix_commands", () -> AffixCommandGameTests::run);
        FUNCTIONS.register("affix_restart", () -> AffixCommandGameTests::restart);
        FUNCTIONS.register("skill_lifecycle", () -> SkillGameTests::lifecycle);
        FUNCTIONS.register("lifecycle_death_decision", () -> LifecycleHardeningGameTests::deathDecision);
        FUNCTIONS.register("lifecycle_cancellation_routes", () -> LifecycleHardeningGameTests::cancellationRoutes);
        FUNCTIONS.register("control_movement", () -> ControlGameTests::movement);
        FUNCTIONS.register("control_actions", () -> ControlGameTests::actions);
        FUNCTIONS.register("control_dash_handoff", () -> ControlGameTests::dashHandoff);
        FUNCTIONS.register("projectile_persistence", () -> ProjectilePersistenceGameTests::run);
        FUNCTIONS.register("skill_network_ingress", () -> SkillNetworkingGameTests::ingress);
        FUNCTIONS.register("connection_versions", () -> SkillNetworkingGameTests::connectionVersions);
        FUNCTIONS.register("lightning_visual_codec", () -> SkillNetworkingGameTests::lightningCodec);
        FUNCTIONS.register("infinite_resource", () -> SkillGameTests::infiniteResource);
        FUNCTIONS.register("ignore_cooldowns", () -> SkillGameTests::ignoreCooldowns);
        FUNCTIONS.register("skill_recovery", () -> SkillGameTests::recovery);
        FUNCTIONS.register("rage_damage", () -> RageGameTests::run);
        FUNCTIONS.register("skill_effects", () -> SkillGameTests::effects);
        FUNCTIONS.register("skill_foundations", () -> SkillFoundationGameTests::run);
        FUNCTIONS.register("rend", () -> BleedGameTests::rend);
        FUNCTIONS.register("combo_progression", () -> ComboGameTests::progression);
        FUNCTIONS.register("combo_movement", () -> ComboGameTests::movement);
        FUNCTIONS.register("combo_effects", () -> ComboGameTests::effects);
        FUNCTIONS.register("combo_lifecycle", () -> ComboGameTests::lifecycle);
        FUNCTIONS.register("combo_persistence", () -> ComboGameTests::persistence);
        FUNCTIONS.register("combo_deadlines", () -> ComboGameTests::deadlines);
        FUNCTIONS.register("combo_interactions", () -> ComboGameTests::interactions);
        FUNCTIONS.register("combo_prediction", () -> ComboGameTests::prediction);
        FUNCTIONS.register("combo_connection_movement", () -> ComboGameTests::connectionMovement);
        FUNCTIONS.register("combo_sliding", () -> ComboGameTests::sliding);
        FUNCTIONS.register("elemental_statuses", () -> ElementalBoltGameTests::statuses);
        FUNCTIONS.register("elemental_collision", () -> ElementalBoltGameTests::collision);
        FUNCTIONS.register("elemental_lightning", () -> ElementalBoltGameTests::lightning);
        FUNCTIONS.register("bleed_pulses", () -> BleedGameTests::pulses);
        FUNCTIONS.register("bleed_persistence", () -> BleedGameTests::persistence);
        FUNCTIONS.register("skill_loadout", () -> SkillLoadoutGameTests::run);
        FUNCTIONS.register("trap_deployment", () -> TrapGameTests::deployment);
        FUNCTIONS.register("trap_terrain", () -> TrapGameTests::terrain);
        FUNCTIONS.register("trap_bear", () -> TrapGameTests::bear);
        FUNCTIONS.register("trap_root_persistence", () -> TrapGameTests::rootPersistence);
        FUNCTIONS.register("trap_frost", () -> TrapGameTests::frost);
        FUNCTIONS.register("trap_flame", () -> TrapGameTests::flame);
        FUNCTIONS.register("trap_flame_defenses", () -> TrapGameTests::flameDefenses);
        FUNCTIONS.register("trap_blast", () -> TrapGameTests::blast);
        FUNCTIONS.register("trap_lifecycle", () -> TrapGameTests::lifecycle);
        FUNCTIONS.register("trap_targeting", () -> TrapGameTests::targeting);
        FUNCTIONS.register("trap_activation", () -> TrapGameTests::activation);
        FUNCTIONS.register("aura_ownership", () -> AuraGameTests::ownership);
        FUNCTIONS.register("status_transitions", () -> StatusDisplayGameTests::transitions);
        FUNCTIONS.register("status_consumption", () -> StatusDisplayGameTests::consumption);
        FUNCTIONS.register("status_storage", () -> StatusDisplayGameTests::storage);
        FUNCTIONS.register("aura_coverage", () -> AuraGameTests::coverage);
        FUNCTIONS.register("aura_dodge", () -> AuraGameTests::dodge);
        FUNCTIONS.register("aura_fortitude", () -> AuraGameTests::fortitude);
        FUNCTIONS.register("aura_ripping", () -> AuraGameTests::ripping);
        FUNCTIONS.register("aura_slows", () -> AuraGameTests::slows);
        FUNCTIONS.register("aura_casting", () -> AuraGameTests::casting);
        FUNCTIONS.register("aura_fireandshock", () -> AuraGameTests::fireAndShock);
        FUNCTIONS.register("aura_chain", () -> AuraGameTests::chain);
        FUNCTIONS.register("aura_enchanting", () -> AuraGameTests::enchanting);
        FUNCTIONS.register("aura_lifecycle", () -> AuraGameTests::lifecycle);
        FUNCTIONS.register("aura_rage", () -> AuraGameTests::rage);
        FUNCTIONS.register("aura_projectiles", () -> AuraGameTests::projectiles);
        FUNCTIONS.register("aura_expiries", () -> AuraGameTests::expiries);
        FUNCTIONS.register("skill_tree", () -> SkillTreeGameTests::run);
        FUNCTIONS.register("progression_engine", () -> ProgressionGameTests::engine);
        FUNCTIONS.register("progression_rewards", () -> ProgressionGameTests::rewards);
        FUNCTIONS.register("progression_support", () -> ProgressionGameTests::support);
        FUNCTIONS.register("progression_wither", () -> ProgressionGameTests::wither);
        FUNCTIONS.register("progression_death", () -> ProgressionGameTests::deathDecision);
        FUNCTIONS.register("progression_timer", () -> ProgressionGameTests::timer);
        FUNCTIONS.register("progression_professions", () -> ProgressionGameTests::professions);
        FUNCTIONS.register("progression_respec", () -> ProgressionGameTests::respec);
        FUNCTIONS.register("progression_arrow_ranks", () -> ProgressionGameTests::arrowRanks);
        FUNCTIONS.register("progression_reload", () -> ProgressionReloadGameTests::run);
        FUNCTIONS.register("progression_defenses", () -> ProgressionGameTests::defenses);
        FUNCTIONS.register("profession_storage", () -> ProfessionGameTests::storage);
        FUNCTIONS.register("profession_validation", () -> ProfessionGameTests::validation);
        FUNCTIONS.register("profession_packets", () -> ProfessionGameTests::packets);
        FUNCTIONS.register("profession_damage", () -> ProfessionGameTests::damage);
        FUNCTIONS.register("profession_respec", () -> ProfessionGameTests::respec);
        FUNCTIONS.register("profession_catalogue", () -> ProfessionGameTests::catalogue);
        FUNCTIONS.register("profession_reload", () -> ProfessionReloadGameTests::run);
        FUNCTIONS.register("activity_mining", () -> ProfessionActivityGameTests::mining);
        FUNCTIONS.register("activity_placement", () -> ProfessionActivityGameTests::placement);
        FUNCTIONS.register("activity_piston", () -> ProfessionActivityGameTests::piston);
        FUNCTIONS.register("activity_crops", () -> ProfessionActivityGameTests::crops);
        FUNCTIONS.register("activity_logging", () -> ProfessionActivityGameTests::logging);
        FUNCTIONS.register("activity_breeding", () -> ProfessionActivityGameTests::breeding);
        FUNCTIONS.register("activity_enchanting", () -> ProfessionActivityGameTests::enchanting);
        FUNCTIONS.register("farming_ranks", () -> FarmingGameTests::ranks);
        FUNCTIONS.register("farming_rules", () -> FarmingGameTests::rules);
        FUNCTIONS.register("farming_replant", () -> FarmingGameTests::replant);
        FUNCTIONS.register("farming_protection", () -> FarmingGameTests::protection);
        FUNCTIONS.register("farming_breakage", () -> FarmingGameTests::breakage);
        FUNCTIONS.register("gathering_vein_ranks", () -> GatheringGameTests::veinRanks);
        FUNCTIONS.register("gathering_vein_rules", () -> GatheringGameTests::veinRules);
        FUNCTIONS.register("gathering_failures", () -> GatheringGameTests::failures);
        FUNCTIONS.register("gathering_tree_ranks", () -> GatheringGameTests::treeRanks);
        FUNCTIONS.register("gathering_tree_continuation", () -> GatheringGameTests::treeContinuation);
        FUNCTIONS.register("gathering_tree_rules", () -> GatheringGameTests::treeRules);
        FUNCTIONS.register("gathering_drop_rules", () -> GatheringGameTests::dropRules);
        FUNCTIONS.register("profiler_activity", () -> GatheringGameTests::profiler);
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

    private static void statEquipment(GameTestHelper helper) {
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

    private static void skillStats(GameTestHelper helper) {
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
    private static void statPersistence(GameTestHelper helper) {
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

    private static void dodge(GameTestHelper helper) {
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
