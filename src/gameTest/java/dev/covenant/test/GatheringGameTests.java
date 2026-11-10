package dev.covenant.test;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.progression.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Real Survival harvesting: point purchases, native per-block costs/loot, protection and bounded continuation. */
final class GatheringGameTests {
    private static void check(boolean value, String message) { ProfessionGameTests.check(value, message); }
    private static ItemStack tool(ServerPlayer player, Item item) {
        var stack = new ItemStack(item); player.setItemInHand(InteractionHand.MAIN_HAND, stack); return stack;
    }
    private static long earned(ServerPlayer player, String profession) {
        var track = player.getData(RpgData.PROFESSIONS).tracks().get(ProfessionData.id(profession));
        return 50L * track.level() * (track.level() + 1) + track.experience();
    }
    private static void buy(ServerPlayer player, Identifier ability, int rank, int level) {
        var snapshot = ProfessionData.snapshot(player.level().getServer()); var node = snapshot.catalogue().abilities().get(ability);
        player.setData(RpgData.PROFESSIONS, player.getData(RpgData.PROFESSIONS).with(node.profession(), new ProfessionState.Track(level, 0)));
        ProfessionAllocations.apply(player, node.profession(), snapshot.revision(), Map.of(), Map.of(ability, rank));
        check(player.getData(RpgData.PROFESSIONS).tracks().get(node.profession()).rank(ability) == rank, "Gathering rank purchased through server validation");
    }
    private static float exhaustion(ServerPlayer player) {
        var output = TagValueOutput.createWithContext(new ProblemReporter.Collector(), player.registryAccess());
        player.getFoodData().addAdditionalSaveData(output);
        return output.buildResult().getFloatOr("foodExhaustionLevel", 0);
    }
    private static List<BlockPos> vein(ProfessionActivityGameTests.ActivityFixture f, int count) {
        var result = new ArrayList<BlockPos>();
        for (int index = 0; index < count; index++) {
            result.add(f.set(3 + index % 8, 0, 3 + index / 8,
                    (index % 2 == 0 ? Blocks.IRON_ORE : Blocks.DEEPSLATE_IRON_ORE).defaultBlockState()));
        }
        return result;
    }
    private static List<BlockPos> tree(ProfessionActivityGameTests.ActivityFixture f, int logs) {
        var result = new ArrayList<BlockPos>(); f.set(4, -1, 4, Blocks.DIRT.defaultBlockState());
        int height = Math.min(logs, 32);
        for (int y = 0; y < height; y++) { result.add(f.set(4, y, 4, Blocks.OAK_LOG.defaultBlockState())); }
        if (logs > 32) { result.add(f.set(5, 30, 4, Blocks.OAK_LOG.defaultBlockState())); }
        f.set(4, height, 4, Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, false));
        f.set(5, height - 1, 4, Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, false));
        return result;
    }
    private static long gone(GameTestHelper helper, List<BlockPos> positions) {
        return positions.stream().filter(pos -> helper.getLevel().getBlockState(pos).isAir()).count();
    }
    private static int drops(ProfessionActivityGameTests.ActivityFixture f, Item item) {
        var box = new AABB(Vec3.atLowerCornerOf(f.pos(0, -2, 0)), Vec3.atLowerCornerOf(f.pos(16, 36, 16)));
        return f.helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).stream()
                .filter(entity -> entity.getItem().is(item)).mapToInt(entity -> entity.getItem().getCount()).sum();
    }
    private static void clearDrops(ProfessionActivityGameTests.ActivityFixture f) {
        var box = new AABB(Vec3.atLowerCornerOf(f.pos(0, -2, 0)), Vec3.atLowerCornerOf(f.pos(16, 36, 16)));
        f.helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
    }
    private static void gatheredDrops(ProfessionActivityGameTests.ActivityFixture f, Item item, BlockPos origin) {
        var box = new AABB(Vec3.atLowerCornerOf(f.pos(0, -2, 0)), Vec3.atLowerCornerOf(f.pos(16, 36, 16)));
        var drops = f.helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item));
        check(!drops.isEmpty(), "Harvest generated item drops");
        var point = drops.getFirst().position();
        check(new AABB(origin).contains(point), "Gathering uses a drop point inside the initiating block");
        check(drops.stream().allMatch(e -> e.position().equals(point)), "All batch drops share the first block's drop point");
    }

    /** Use the server's real metrics recorder; never replace its active thread-local profiler. */
    static void profiler(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ProfileResults[] recorded = {null};
        server.startRecordingMetrics(result -> recorded[0] = result, path -> {});
        helper.runAfterDelay(1, () -> {
            try (var zone = Profiler.get().zone("covenant_test:profiling")) {
                try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 145)) {
                    var player = f.player(); buy(player, ProfessionData.VEIN_MINING, 1, 2);
                    tool(player, Items.DIAMOND_PICKAXE);
                    var positions = vein(f, 5);
                    player.gameMode.destroyBlock(positions.getFirst());
                    check(gone(helper, positions) == 4, "Profiled harvesting retains its native rank cap");
                    // Classless activation rejects inside its profiling scope.
                    dev.covenant.rpg.skills.Skills.activate(player, dev.covenant.rpg.skills.SkillDefinition.CLEAVE);
                    check(!dev.covenant.rpg.skills.SkillControls.blocksMovement(player), "Idle movement checks preserve control");
                    check(!dev.covenant.rpg.skills.Auras.frozen(player), "Idle freeze checks preserve control");
                    // No active bleed/slow: their callback guards must still be labelled in the pie.
                    NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
                    // The common arrow callback is registered only by the physical-client entry point.
                    dev.covenant.rpg.skills.SkillVisuals.arrowTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
                    positions = vein(f, 5);
                    var batch = ProfessionGathering.prepare(player, positions.getFirst(),
                            helper.getLevel().getBlockState(positions.getFirst()), true, player.getMainHandItem());
                    check(batch != null, "Exception fixture has additional harvest targets");
                    var expected = new IllegalStateException("fixture harvest failure");
                    try {
                        batch.harvest(player, pos -> { throw expected; });
                        throw new AssertionError("Fixture must throw from inside the harvesting scope");
                    } catch (IllegalStateException failure) {
                        check(failure == expected, "Profiling preserves the original exception");
                    }
                    try (var after = Profiler.get().zone("covenant_test:after_failure")) {
                        check(!PlayerProfiles.get(player).created(), "Rejected skill leaves class state untouched");
                    }
                }
            } finally { server.finishRecordingMetrics(); }
        });
        helper.runAfterDelay(3, () -> {
            check(recorded[0] != null, "Native metrics recorder completed");
            var path = findProfilePath(recorded[0], "root", "covenant_test:profiling");
            check(path != null, "Native report contains the recorded activity");
            var children = recorded[0].getTimes(path).stream().skip(1).map(field -> field.name).toList();
            for (var name : List.of("covenant:gathering_prepare", "covenant:gathering_harvest",
                    "covenant:harvest_reward_check", "covenant:skill_activate", "covenant_test:after_failure",
                    "covenant:movement_control_check", "covenant:freeze_check", "covenant:bleeding",
                    "covenant:frost_slows", "covenant:auras", "covenant:arrow_visuals")) {
                check(children.contains(name), "Scopes remain siblings after early return/exception: " + name);
            }
            helper.succeed();
        });
    }

    private static String findProfilePath(ProfileResults results, String path, String name) {
        for (var field : results.getTimes(path).stream().skip(1).toList()) {
            if (field.name.equals("unspecified")) { continue; }
            var child = path + ProfileResults.PATH_SEPARATOR + field.name;
            if (field.name.equals(name)) { return child; }
            var found = findProfilePath(results, child, name);
            if (found != null) { return found; }
        }
        return null;
    }

    static void veinRanks(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 60)) {
            var player = f.player(); int[] levels = {2, 5, 10, 15}; int[] limits = {4, 8, 16, 32};
            for (int rank = 1; rank <= 4; rank++) {
                buy(player, ProfessionData.VEIN_MINING, rank, levels[rank - 1]); var stack = tool(player, Items.DIAMOND_PICKAXE);
                int cap = limits[rank - 1]; var positions = vein(f, cap + 1); long xp = earned(player, "mining"); float cost = exhaustion(player);
                check(player.gameMode.destroyBlock(positions.getFirst()), "Native initiating ore break succeeds");
                check(gone(helper, positions) == cap, "Rank cap includes initial ore and suppresses recursive expansion: " + rank);
                check(stack.getDamageValue() == cap, "One native durability use per ore: " + rank);
                check(Math.abs(exhaustion(player) - cost - cap * .005F) < .0001, "One native exhaustion charge per ore: " + rank);
                check(earned(player, "mining") == xp + cap * 17L, "Each stone/deepslate ore earns exactly one original-block reward");
                check(drops(f, Items.RAW_IRON) == cap, "Every harvested ore retains its native loot");
                gatheredDrops(f, Items.RAW_IRON, positions.getFirst()); clearDrops(f);
                for (var pos : positions) { helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2); }
            }
            check(!PlayerProfiles.get(player).created(), "All gathering ranks work without selecting a combat class");
            helper.succeed();
        }
    }

    static void veinRules(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 70)) {
            var player = f.player(); tool(player, Items.DIAMOND_PICKAXE);
            var a = f.set(4, 0, 4, Blocks.IRON_ORE.defaultBlockState());
            var b = f.set(5, 1, 5, Blocks.DEEPSLATE_IRON_ORE.defaultBlockState());
            var copper = f.set(6, 2, 6, Blocks.COPPER_ORE.defaultBlockState());
            player.gameMode.destroyBlock(a); check(!helper.getLevel().getBlockState(b).isAir(), "Unpurchased ability preserves single-block mining");
            buy(player, ProfessionData.VEIN_MINING, 1, 2); f.set(4, 0, 4, Blocks.IRON_ORE.defaultBlockState());
            player.setShiftKeyDown(true); player.gameMode.destroyBlock(a);
            check(!helper.getLevel().getBlockState(b).isAir(), "Sneaking bypasses Vein Mining"); player.setShiftKeyDown(false);
            f.set(4, 0, 4, Blocks.IRON_ORE.defaultBlockState()); tool(player, Items.DIAMOND_AXE); player.gameMode.destroyBlock(a);
            check(!helper.getLevel().getBlockState(b).isAir(), "Non-pickaxes cannot expand even when they remove the initial block");
            tool(player, Items.WOODEN_PICKAXE); f.set(4, 0, 4, Blocks.IRON_ORE.defaultBlockState()); player.gameMode.destroyBlock(a);
            check(!helper.getLevel().getBlockState(b).isAir(), "Insufficient harvest tier cannot expand");
            tool(player, Items.DIAMOND_PICKAXE); f.set(4, 0, 4, Blocks.IRON_ORE.defaultBlockState()); player.gameMode.destroyBlock(a);
            check(helper.getLevel().getBlockState(b).isAir() && !helper.getLevel().getBlockState(copper).isAir(),
                    "All 26 neighbours join stone/deepslate family variants while touching different ore stays intact");
            // Placed ore remains harvestable, but neither a placed origin nor an extra placed block earns XP.
            var placed = f.placeOre(player, 4, 0, 8); f.placeOre(player, 5, 0, 8);
            f.set(6, 0, 8, Blocks.DEEPSLATE_IRON_ORE.defaultBlockState()); tool(player, Items.DIAMOND_PICKAXE);
            long before = earned(player, "mining"); player.gameMode.destroyBlock(placed);
            check(helper.getLevel().getBlockState(f.pos(6, 0, 8)).isAir() && earned(player, "mining") == before + 17,
                    "Placed ore does not block connectivity or launder placed blocks into Mining XP");
            var diamond = f.set(4, 0, 11, Blocks.DIAMOND_ORE.defaultBlockState());
            var iron = f.set(5, 0, 11, Blocks.IRON_ORE.defaultBlockState());
            var enchants = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var silk = tool(player, Items.DIAMOND_PICKAXE); silk.enchant(enchants.getOrThrow(Enchantments.SILK_TOUCH), 1);
            f.set(4, 0, 12, Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState()); before = earned(player, "mining");
            player.gameMode.destroyBlock(diamond);
            check(drops(f, Items.DIAMOND_ORE) == 1 && drops(f, Items.DEEPSLATE_DIAMOND_ORE) == 1
                    && earned(player, "mining") == before + 100, "Native Silk Touch loot and XP apply to every original block");
            var fortune = tool(player, Items.DIAMOND_PICKAXE); fortune.enchant(enchants.getOrThrow(Enchantments.FORTUNE), 3);
            fortune.enchant(enchants.getOrThrow(Enchantments.UNBREAKING), 3);
            for (int i = 0; i < 8; i++) { f.set(3 + i, 0, 14, Blocks.DIAMOND_ORE.defaultBlockState()); }
            before = earned(player, "mining"); player.gameMode.destroyBlock(f.pos(3, 0, 14));
            check(drops(f, Items.DIAMOND) >= 4 && drops(f, Items.DIAMOND) <= 16 && fortune.getDamageValue() <= 4
                    && earned(player, "mining") == before + 200, "Native Fortune/Unbreaking operate per block without multiplying profession XP");
            check(ProfessionAllocations.respec(player), "Gathering allocations can be refunded");
            f.set(4, 0, 11, Blocks.IRON_ORE.defaultBlockState()); player.gameMode.destroyBlock(f.pos(4, 0, 11));
            check(!helper.getLevel().getBlockState(iron).isAir(), "Respec disables expansion immediately");
            helper.succeed(); clearDrops(f);
        }
    }

    static void failures(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 80)) {
            var player = f.player(); buy(player, ProfessionData.VEIN_MINING, 1, 2);
            var stack = tool(player, Items.DIAMOND_PICKAXE); var positions = vein(f, 3); var blocked = positions.get(1);
            int[] attempts = {0}; Consumer<BreakBlockEvent> cancel = event -> {
                if (event.getPlayer() == player) { attempts[0]++; if (event.getPos().equals(blocked)) { event.setCanceled(true); } }
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancel);
            long before = earned(player, "mining");
            try { player.gameMode.destroyBlock(positions.getFirst()); } finally { NeoForge.EVENT_BUS.unregister(cancel); }
            check(gone(helper, positions) == 1 && attempts[0] == 2 && stack.getDamageValue() == 1
                    && earned(player, "mining") == before + 17, "First cancelled extra stops the whole batch without spending tool or XP on blocked/later ore");
            player.gameMode.destroyBlock(blocked);
            check(gone(helper, positions) == 3, "Cancellation leaves no stuck recursion/activation guard");
            positions = vein(f, 3); var origin = positions.getFirst();
            Consumer<BreakBlockEvent> cancelOrigin = event -> { if (event.getPos().equals(origin)) { event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancelOrigin);
            try { player.gameMode.destroyBlock(origin); } finally { NeoForge.EVENT_BUS.unregister(cancelOrigin); }
            check(gone(helper, positions) == 0, "Cancelled initial break never starts a batch");
            stack = tool(player, Items.DIAMOND_PICKAXE); stack.setDamageValue(stack.getMaxDamage() - 2);
            int[] broken = {0}; Consumer<PlayerDestroyItemEvent> onBreak = event -> { if (event.getEntity() == player) { broken[0]++; } };
            NeoForge.EVENT_BUS.addListener(onBreak); before = earned(player, "mining");
            try { player.gameMode.destroyBlock(origin); } finally { NeoForge.EVENT_BUS.unregister(onBreak); }
            check(gone(helper, positions) == 2 && player.getMainHandItem().isEmpty() && broken[0] == 1
                    && earned(player, "mining") == before + 34, "Breaking on an extra ore stops immediately, retaining drops/XP and firing one native tool-break event");
            positions = vein(f, 3); stack = tool(player, Items.DIAMOND_PICKAXE); stack.setDamageValue(stack.getMaxDamage() - 1);
            player.gameMode.destroyBlock(positions.getFirst()); check(gone(helper, positions) == 1, "Tool breaking on initial ore prevents all extras");
            player.setGameMode(GameType.CREATIVE); tool(player, Items.DIAMOND_PICKAXE); positions = vein(f, 3);
            player.gameMode.destroyBlock(positions.getFirst()); check(gone(helper, positions) == 1, "Creative breaking does not expand");
            var fake = FakePlayerFactory.getMinecraft(helper.getLevel()); tool(fake, Items.DIAMOND_PICKAXE);
            var saved = fake.getData(RpgData.PROFESSIONS);
            try {
                buy(fake, ProfessionData.VEIN_MINING, 1, 2); positions = vein(f, 3); fake.gameMode.destroyBlock(positions.getFirst());
                check(gone(helper, positions) == 1, "Fake-player automation cannot activate gathering even with purchased ranks");
            } finally { fake.setData(RpgData.PROFESSIONS, saved); fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); }
            helper.succeed(); clearDrops(f);
        }
    }

    static void treeRanks(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 90)) {
            var player = f.player(); int[] levels = {1, 5, 10, 15}; int[] limits = {4, 8, 16, 32};
            for (int rank = 1; rank <= 4; rank++) {
                buy(player, ProfessionData.TREE_FELLING, rank, levels[rank - 1]); var stack = tool(player, Items.DIAMOND_AXE);
                int cap = limits[rank - 1]; var positions = tree(f, cap + 1); long before = earned(player, "logging"); float cost = exhaustion(player);
                player.gameMode.destroyBlock(positions.getFirst());
                check(gone(helper, positions) == cap && stack.getDamageValue() == cap, "Felling cap includes the base and charges native durability: " + rank);
                check(earned(player, "logging") == before + cap * 20L && drops(f, Items.OAK_LOG) == cap,
                        "Each felled log retains exactly one native drop and Logging payout");
                gatheredDrops(f, Items.OAK_LOG, positions.getFirst());
                check(Math.abs(exhaustion(player) - cost - cap * .005F) < .0001, "Felling charges native exhaustion per log");
                check(helper.getLevel().getBlockState(f.pos(4, Math.min(cap + 1, 32), 4)).is(Blocks.OAK_LEAVES), "Felling leaves the canopy for native decay");
                for (var pos : positions) { helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2); } clearDrops(f);
            }
            helper.succeed();
        }
    }

    static void dropRules(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 135)) {
            var player = f.player(); buy(player, ProfessionData.VEIN_MINING, 1, 2);
            var stack = tool(player, Items.DIAMOND_PICKAXE); var positions = vein(f, 3);
            var unrelated = new ItemEntity(helper.getLevel(), positions.getFirst().getX(),
                    positions.getFirst().getY(), positions.getFirst().getZ() - 1, new ItemStack(Items.EMERALD));
            helper.getLevel().addFreshEntity(unrelated); var unrelatedPoint = unrelated.position();
            Vec3[] originPoint = {null};
            Consumer<BlockDropsEvent> modify = event -> {
                if (event.getBreaker() != player) { return; }
                if (event.getPos().equals(positions.getFirst())) { originPoint[0] = event.getDrops().getFirst().position(); }
                if (event.getPos().equals(positions.get(1))) {
                    event.getDrops().add(new ItemEntity(helper.getLevel(), event.getPos().getX(), event.getPos().getY(),
                            event.getPos().getZ(), new ItemStack(Items.EMERALD)));
                }
                if (event.getPos().equals(positions.get(2))) { event.setCanceled(true); }
            };
            NeoForge.EVENT_BUS.addListener(modify);
            try { player.gameMode.destroyBlock(positions.getFirst()); } finally { NeoForge.EVENT_BUS.unregister(modify); }
            check(gone(helper, positions) == 3 && stack.getDamageValue() == 3, "Drop cancellation preserves actual per-block native harvest costs");
            check(drops(f, Items.RAW_IRON) == 2, "Cancelled loot is not recreated by gathering");
            gatheredDrops(f, Items.RAW_IRON, positions.getFirst());
            var loot = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(Vec3.atLowerCornerOf(f.pos(0, -2, 0)), Vec3.atLowerCornerOf(f.pos(16, 36, 16))));
            check(loot.stream().filter(e -> e != unrelated).allMatch(e -> e.position().equals(originPoint[0])),
                    "Native and event-added loot use the initiating block's actual drop point");
            check(unrelated.position().equals(unrelatedPoint), "Nearby preexisting loose items are untouched");
            clearDrops(f);
            vein(f, 3);
            Consumer<BlockDropsEvent> cancelOriginLoot = event -> {
                if (event.getBreaker() == player && event.getPos().equals(positions.getFirst())) { event.setCanceled(true); }
            };
            NeoForge.EVENT_BUS.addListener(cancelOriginLoot);
            try { player.gameMode.destroyBlock(positions.getFirst()); } finally { NeoForge.EVENT_BUS.unregister(cancelOriginLoot); }
            check(drops(f, Items.RAW_IRON) == 2, "Cancelling initiating loot retains only the extra blocks' viable drops");
            var fallbackDrops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(positions.getFirst()).inflate(1));
            check(fallbackDrops.size() == 2 && fallbackDrops.stream().allMatch(e -> e.position().equals(Vec3.atCenterOf(positions.getFirst()))),
                    "No initiating loot uses the first block's center without duplicating cancelled drops");
            clearDrops(f);
            var single = f.set(3, 0, 3, Blocks.IRON_ORE.defaultBlockState());
            player.setShiftKeyDown(true); player.gameMode.destroyBlock(single);
            check(((ProfessionGathering.HarvestAccess) player.gameMode).covenant$gatheringBatch() == null,
                    "No batch drop state leaks into a subsequent ordinary harvest");
            helper.succeed(); clearDrops(f);
        }
    }

    static void treeContinuation(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 115)) {
            var player = f.player(); buy(player, ProfessionData.TREE_FELLING, 1, 1); tool(player, Items.DIAMOND_AXE);
            var positions = tree(f, 10); player.gameMode.destroyBlock(positions.getFirst());
            check(gone(helper, positions) == 4, "First activation leaves a tall tree partially standing");
            player.gameMode.destroyBlock(positions.get(4)); check(gone(helper, positions) == 8, "Lowest remaining certified log starts another capped batch");
            player.gameMode.destroyBlock(positions.get(8)); check(gone(helper, positions) == 10, "Final short batch finishes without a saved job");
            positions = tree(f, 10); player.gameMode.destroyBlock(positions.get(5));
            check(gone(helper, positions) == 1, "An upper log cannot activate felling while lower certified logs remain");
            player.gameMode.destroyBlock(positions.getFirst()); check(gone(helper, positions) == 5, "Lower part can still be felled after ordinary upper chopping");
            // Simulate disposable evidence loss on unload/reload; floating remnants cannot establish a fresh tree.
            for (var pos : positions) { helper.getLevel().getChunkAt(pos).removeData(RpgData.HARVEST_TREES); }
            long before = earned(player, "logging"); player.gameMode.destroyBlock(positions.get(4));
            check(gone(helper, positions) == 6 && earned(player, "logging") == before, "Evidence loss gives conservative ordinary chopping without stale XP/felling");
            helper.succeed(); clearDrops(f);
        }
    }

    static void treeRules(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 125)) {
            var player = f.player(); buy(player, ProfessionData.TREE_FELLING, 1, 1); tool(player, Items.DIAMOND_AXE);
            var positions = tree(f, 6); player.setShiftKeyDown(true); player.gameMode.destroyBlock(positions.getFirst());
            check(gone(helper, positions) == 1, "Sneaking preserves single-log chopping"); player.setShiftKeyDown(false);
            positions = tree(f, 6); tool(player, Items.DIAMOND_PICKAXE); player.gameMode.destroyBlock(positions.getFirst());
            check(gone(helper, positions) == 1, "Felling requires an axe even though ordinary log XP needs no tool");
            positions = tree(f, 6); tool(player, Items.DIAMOND_AXE);
            f.set(4, 6, 4, Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
            f.set(5, 5, 4, Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
            player.gameMode.destroyBlock(positions.getFirst()); check(gone(helper, positions) == 1, "Persistent leaves do not establish a canopy");
            positions = tree(f, 6); var blocked = positions.get(1);
            Consumer<BreakBlockEvent> cancel = event -> { if (event.getPos().equals(blocked)) { event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancel); long before = earned(player, "logging");
            try { player.gameMode.destroyBlock(positions.getFirst()); } finally { NeoForge.EVENT_BUS.unregister(cancel); }
            check(gone(helper, positions) == 1 && earned(player, "logging") == before + 20, "Protected log stops the whole felling batch");
            positions = tree(f, 6); var axe = tool(player, Items.DIAMOND_AXE); axe.setDamageValue(axe.getMaxDamage() - 2);
            player.gameMode.destroyBlock(positions.getFirst()); check(gone(helper, positions) == 2 && player.getMainHandItem().isEmpty(), "Broken axe leaves all unharvested logs intact");
            positions = tree(f, 6); tool(player, Items.DIAMOND_AXE); player.gameMode.destroyBlock(positions.getFirst());
            f.set(4, 4, 4, Blocks.AIR.defaultBlockState()); f.set(4, 4, 4, Blocks.OAK_LOG.defaultBlockState());
            before = earned(player, "logging"); player.gameMode.destroyBlock(positions.get(4));
            check(gone(helper, positions) == 5 && earned(player, "logging") == before, "Editing a remembered log revokes both continuation and its XP evidence");
            helper.succeed(); clearDrops(f);
        }
    }
}
