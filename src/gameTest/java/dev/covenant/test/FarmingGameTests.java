package dev.covenant.test;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.progression.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;

/** Real server hoe-use and native break/placement transactions, with independent classless progression. */
final class FarmingGameTests {
    static void feedback(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 195)) {
            var packets = new ArrayList<net.minecraft.network.protocol.Packet<?>>();
            var player = f.entities.player(null, packets); player.setPos(Vec3.atBottomCenterOf(f.pos(2, 0, 2)));
            buy(player, 1, true); hoe(player);
            for (int x = 6; x <= 8; x++) { for (int z = 6; z <= 8; z++) { crop(f, x, 0, z, wheat(7)); } }
            packets.clear(); use(player, f.pos(7, 0, 7));
            var notices = shortages(packets);
            check(notices.size() == 1 && notices.getFirst().getArgs()[0].equals(9), "Nine missing replants produce one accurate batch cue");
            crop(f, 7, 0, 7, wheat(7)); player.setShiftKeyDown(true); packets.clear(); use(player, f.pos(7, 0, 7));
            check(shortages(packets).size() == 1 && shortages(packets).getFirst().getArgs()[0].equals(1), "Sneaking reports only its single harvested crop");
            crop(f, 7, 0, 7, wheat(7)); player.getInventory().setItem(1, new ItemStack(Items.WHEAT_SEEDS, 2));
            Consumer<EntityPlaceEvent> deny = event -> { if (event.getEntity() == player) { event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(deny);
            try { packets.clear(); use(player, f.pos(7, 0, 7));
                check(shortages(packets).isEmpty() && player.getInventory().getItem(1).getCount() == 2, "Protection-denied placement is not reported as missing supplies");
            } finally { NeoForge.EVENT_BUS.unregister(deny); }
            crop(f, 7, 0, 7, wheat(7)); player.getInventory().setItem(1, ItemStack.EMPTY);
            Consumer<BreakBlockEvent> block = event -> { if (event.getPlayer() == player) { event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(block);
            try { packets.clear(); use(player, f.pos(7, 0, 7));
                check(shortages(packets).isEmpty() && player.level().getBlockState(f.pos(7, 0, 7)) == wheat(7), "Denied harvest does not emit an invented replant failure");
            } finally { NeoForge.EVENT_BUS.unregister(block); }
            crop(f, 7, 0, 7, wheat(7)); var tool = hoe(player); tool.setDamageValue(tool.getMaxDamage() - 1);
            packets.clear(); use(player, f.pos(7, 0, 7));
            check(tool.isEmpty() && shortages(packets).size() == 1, "Last successful crop can report a missing replant supply even when its hoe breaks");
            helper.succeed();
        }
    }
    private static List<net.minecraft.network.chat.contents.TranslatableContents> shortages(List<net.minecraft.network.protocol.Packet<?>> packets) {
        return packets.stream().filter(p -> p instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket s && s.overlay()
                && s.content().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().equals("profession.covenant.replant_supplies"))
                .map(p -> (net.minecraft.network.chat.contents.TranslatableContents)((net.minecraft.network.protocol.game.ClientboundSystemChatPacket)p).content().getContents()).toList();
    }
    private static final net.minecraft.resources.Identifier FARMING = ProfessionData.id("farming");
    private static void check(boolean value, String message) { ProfessionGameTests.check(value, message); }
    private static BlockState wheat(int age) { return Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, age); }
    private static void buy(ServerPlayer player, int rank, boolean replant) {
        var state = player.getData(RpgData.PROFESSIONS);
        player.setData(RpgData.PROFESSIONS, state.with(FARMING, new ProfessionState.Track(10, 0)));
        var desired = new HashMap<net.minecraft.resources.Identifier, Integer>();
        if (rank > 0) { desired.put(ProfessionData.SCYTHING, rank); }
        if (replant) { desired.put(ProfessionData.FARM_REPLANT, 1); }
        ProfessionAllocations.apply(player, FARMING, ProfessionData.snapshot(player.level().getServer()).revision(), Map.of(), desired);
        var track = player.getData(RpgData.PROFESSIONS).tracks().get(FARMING);
        check(track.rank(ProfessionData.SCYTHING) == rank && track.rank(ProfessionData.FARM_REPLANT) == (replant ? 1 : 0), "Farming purchases pass server gates");
    }
    private static ItemStack hoe(ServerPlayer player) {
        var stack = new ItemStack(Items.DIAMOND_HOE); player.setItemInHand(InteractionHand.MAIN_HAND, stack); return stack;
    }
    private static void use(ServerPlayer player, BlockPos pos) {
        player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }
    private static BlockPos crop(ProfessionActivityGameTests.ActivityFixture f, int x, int y, int z, BlockState state) {
        f.set(x, y - 1, z, (state.is(Blocks.NETHER_WART) ? Blocks.SOUL_SAND : Blocks.FARMLAND).defaultBlockState());
        return f.set(x, y, z, state);
    }
    private static List<BlockPos> field(ProfessionActivityGameTests.ActivityFixture f) {
        var positions = new ArrayList<BlockPos>();
        for (int x = 3; x <= 11; x++) {
            for (int z = 3; z <= 11; z++) {
                positions.add(crop(f, x, 0, z, (x + z) % 2 == 0 ? wheat(7)
                        : Blocks.CARROTS.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7)));
            }
        }
        return positions;
    }
    private static long gone(ServerPlayer player, List<BlockPos> positions) {
        return positions.stream().filter(pos -> player.level().getBlockState(pos).isAir()).count();
    }
    private static void clearDrops(ProfessionActivityGameTests.ActivityFixture f) {
        f.helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(Vec3.atLowerCornerOf(f.pos(0, -2, 0)), Vec3.atLowerCornerOf(f.pos(16, 5, 16)))).forEach(ItemEntity::discard);
    }

    static void ranks(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 160)) {
            var player = f.player();
            for (int rank = 1; rank <= 3; rank++) {
                buy(player, rank, false); var tool = hoe(player); var positions = field(f);
                var immature = crop(f, 8, 0, 7, wheat(3));
                var upper = crop(f, 7, 3, 7, wheat(7));
                long before = player.getData(RpgData.PROFESSIONS).tracks().get(FARMING).experience();
                use(player, f.pos(7, 0, 7));
                int expected = new int[] {8, 24, 80}[rank - 1];
                check(gone(player, positions) == expected, "Exact mixed-crop harvest count at Scything rank " + rank);
                check(tool.getDamageValue() == expected, "One durability attempt per mature crop, without doubled native mining wear");
                check(player.level().getBlockState(immature) == wheat(3) && player.level().getBlockState(upper) == wheat(7), "Immature crops and other Y layers remain untouched");
                check(player.getData(RpgData.PROFESSIONS).tracks().get(FARMING).experience() == before + expected * 5, "Each actual crop pays existing Farming XP once");
                clearDrops(f);
            }
            check(!PlayerProfiles.get(player).created(), "Farming does not select a combat class");
            helper.succeed();
        }
    }

    static void rules(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 165)) {
            var player = f.player(); var positions = field(f); var tool = hoe(player);
            use(player, f.pos(7, 0, 7));
            check(gone(player, positions) == 1 && tool.getDamageValue() == 1, "Unlearned Scything retains one-crop hoe harvesting");
            buy(player, 3, false); positions = field(f); tool = hoe(player); player.setShiftKeyDown(true);
            use(player, f.pos(7, 0, 7));
            check(gone(player, positions) == 1 && tool.getDamageValue() == 1, "Sneaking limits learned Scything to one crop");
            player.setShiftKeyDown(false); positions = field(f); tool = hoe(player);
            player.gameMode.destroyBlock(f.pos(7, 0, 7));
            check(gone(player, positions) == 1 && tool.getDamageValue() == 0, "Left-click uses ordinary crop breaking without Scything or added wear");
            positions = field(f); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_AXE));
            use(player, f.pos(7, 0, 7)); check(gone(player, positions) == 0, "Scything requires a hoe");
            hoe(player); player.setGameMode(GameType.CREATIVE); use(player, f.pos(7, 0, 7));
            check(gone(player, positions) == 0, "Creative interaction cannot farm through Covenant"); player.setGameMode(GameType.SURVIVAL);
            var ground = f.set(1, 0, 1, Blocks.DIRT.defaultBlockState()); f.set(1, 1, 1, Blocks.AIR.defaultBlockState());
            use(player, ground); check(player.level().getBlockState(ground).is(Blocks.FARMLAND), "Ordinary hoe tilling still works");
            positions = field(f); tool = hoe(player);
            tool.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 3);
            player.getRandom().setSeed(67543); use(player, f.pos(7, 0, 7));
            check(gone(player, positions) == 81 && tool.getDamageValue() > 0 && tool.getDamageValue() < 81, "Native Unbreaking reduces harvest wear");
            clearDrops(f); helper.succeed();
        }
    }

    static void replant(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 170)) {
            var player = f.player(); buy(player, 2, true); var tool = hoe(player);
            var crops = List.of(wheat(7), Blocks.CARROTS.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7),
                    Blocks.POTATOES.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7),
                    Blocks.BEETROOTS.defaultBlockState().setValue(BlockStateProperties.AGE_3, 3),
                    Blocks.NETHER_WART.defaultBlockState().setValue(BlockStateProperties.AGE_3, 3));
            var seeds = List.of(Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS, Items.NETHER_WART);
            var targets = player.getData(RpgData.PROFESSION_VIEW).crops();
            check(targets.size() == 5 && crops.stream().allMatch(state -> targets.stream().anyMatch(target -> target.test(state)))
                    && targets.stream().noneMatch(target -> target.test(wheat(0))), "Owner view predicts only configured mature targets, avoiding a second offhand use");
            var positions = new ArrayList<BlockPos>();
            for (int i = 0; i < crops.size(); i++) {
                positions.add(crop(f, 5 + i, 0, 7, crops.get(i)));
                player.getInventory().setItem(i + 1, new ItemStack(seeds.get(i), 2));
            }
            use(player, f.pos(7, 0, 7));
            for (int i = 0; i < crops.size(); i++) {
                var state = player.level().getBlockState(positions.get(i));
                check(state == crops.get(i).getBlock().defaultBlockState(), "Correct initial crop state replanted for " + seeds.get(i));
                check(player.getInventory().getItem(i + 1).getCount() == 1, "One matching inventory item consumed per replant");
            }
            check(tool.getDamageValue() == 5, "Replanting adds no durability cost");
            var lone = crop(f, 7, 0, 7, wheat(7)); player.getInventory().setItem(1, ItemStack.EMPTY);
            player.setShiftKeyDown(true); use(player, lone);
            check(player.level().getBlockState(lone).isAir(), "Missing inventory seeds leaves harvested crop empty despite native seed drops");
            // Replanting is independently purchasable, and sneak still allows its single-crop step.
            buy(player, 0, true); crop(f, 7, 0, 7, wheat(7));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WHEAT_SEEDS));
            use(player, lone);
            check(player.level().getBlockState(lone) == wheat(0) && player.getOffhandItem().isEmpty(), "Replant alone uses offhand inventory supply while sneaking");
            // Left-click never runs the hoe-use replant interaction.
            crop(f, 7, 0, 7, wheat(7)); player.getInventory().setItem(1, new ItemStack(Items.WHEAT_SEEDS));
            player.gameMode.destroyBlock(lone);
            check(player.level().getBlockState(lone).isAir() && player.getInventory().getItem(1).getCount() == 1, "Ordinary breaking neither replants nor spends seed");
            clearDrops(f); helper.succeed();
        }
    }

    static void protection(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 175)) {
            var player = f.player(); buy(player, 1, true); var tool = hoe(player);
            var positions = field(f); var origin = f.pos(7, 0, 7);
            player.getInventory().setItem(1, new ItemStack(Items.WHEAT_SEEDS, 10));
            Consumer<PlayerInteractEvent.RightClickBlock> denyUse = event -> { if (event.getEntity() == player) { event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, denyUse);
            try { use(player, origin); } finally { NeoForge.EVENT_BUS.unregister(denyUse); }
            check(gone(player, positions) == 0 && tool.getDamageValue() == 0, "Final right-click cancellation prevents all work");
            var blocked = f.pos(6, 0, 6);
            Consumer<BreakBlockEvent> denyBreak = event -> { if (event.getPos().equals(blocked)) { event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, denyBreak);
            try { use(player, origin); } finally { NeoForge.EVENT_BUS.unregister(denyBreak); }
            check(tool.getDamageValue() == 1 && player.level().getBlockState(origin) == wheat(0)
                    && player.level().getBlockState(blocked) == wheat(7), "First protected crop stops the batch, retaining completed harvest and replant");
            check(player.getInventory().getItem(1).getCount() == 9, "Blocked crop consumes no planting item");
            player.setShiftKeyDown(true); crop(f, 7, 0, 7, wheat(7));
            Consumer<EntityPlaceEvent> denyPlant = event -> { if (event.getEntity() == player) { event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, denyPlant);
            try { use(player, origin); } finally { NeoForge.EVENT_BUS.unregister(denyPlant); }
            check(player.level().getBlockState(origin).isAir() && player.getInventory().getItem(1).getCount() == 9,
                    "Native cancelled placement restores the empty block and preserves inventory seed");
            crop(f, 7, 0, 7, wheat(7));
            Consumer<BlockDropsEvent> cancelLoot = event -> { if (event.getBreaker() == player) { event.setCanceled(true); } };
            clearDrops(f); NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancelLoot);
            try { use(player, origin); } finally { NeoForge.EVENT_BUS.unregister(cancelLoot); }
            check(player.level().getBlockState(origin) == wheat(0) && player.getInventory().getItem(1).getCount() == 8,
                    "Cancelled loot stays cancelled while inventory-funded replant can complete");
            var items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(2));
            check(items.isEmpty(), "Replant does not reconstruct cancelled loot");
            crop(f, 7, 0, 7, wheat(7));
            Consumer<BlockDropsEvent> removeSoil = event -> {
                if (event.getBreaker() == player) { f.set(7, -1, 7, Blocks.DIRT.defaultBlockState()); }
            };
            NeoForge.EVENT_BUS.addListener(removeSoil);
            try { use(player, origin); } finally { NeoForge.EVENT_BUS.unregister(removeSoil); }
            check(player.level().getBlockState(origin).isAir() && player.getInventory().getItem(1).getCount() == 8,
                    "Invalid soil after harvest leaves the crop empty without consuming seed");
            clearDrops(f); helper.succeed();
        }
    }

    static void breakage(GameTestHelper helper) {
        try (var f = new ProfessionActivityGameTests.ActivityFixture(helper, 180)) {
            var player = f.player(); buy(player, 1, true); var tool = hoe(player); var positions = field(f);
            tool.setDamageValue(tool.getMaxDamage() - 1); player.getInventory().setItem(1, new ItemStack(Items.WHEAT_SEEDS, 2));
            int[] breaks = {0};
            Consumer<PlayerDestroyItemEvent> replace = event -> {
                if (event.getEntity() == player && event.getOriginal().is(Items.DIAMOND_HOE)) { breaks[0]++; hoe(player); }
            };
            NeoForge.EVENT_BUS.addListener(replace);
            try { use(player, f.pos(7, 0, 7)); } finally { NeoForge.EVENT_BUS.unregister(replace); }
            check(breaks[0] == 1 && tool.isEmpty() && player.getMainHandItem().getDamageValue() == 0, "One native break notification; replacement hoe does no further harvesting");
            check(gone(player, positions) == 0 && player.level().getBlockState(f.pos(7, 0, 7)) == wheat(0), "Last harvest still replants after hoe break");
            check(player.getInventory().getItem(1).getCount() == 1, "Last replant consumes exactly one seed");
            check(player.getData(RpgData.PROFESSIONS).tracks().get(FARMING).experience() == 5, "Hoe break retains exactly one crop reward");
            clearDrops(f); helper.succeed();
        }
    }
}
