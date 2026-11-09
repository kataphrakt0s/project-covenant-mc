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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Exercise real Survival block removal, item placement, breeding, enchanting and chunk serialization. */
final class ProfessionActivityGameTests {
    private static void check(boolean value, String message) { ProfessionGameTests.check(value, message); }
    private static long earned(ServerPlayer player, String profession) {
        var track = player.getData(RpgData.PROFESSIONS).tracks().get(ProfessionData.id(profession));
        return 50L * track.level() * (track.level() + 1) + track.experience();
    }
    private static void tool(ServerPlayer player, Item item) { player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item)); }

    static void mining(GameTestHelper helper) {
        try (var f = new ActivityFixture(helper, 140)) {
            var player = f.player(); tool(player, Items.DIAMOND_PICKAXE);
            var blocks = List.of(Blocks.COAL_ORE, Blocks.COPPER_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.IRON_ORE,
                    Blocks.GOLD_ORE, Blocks.NETHER_GOLD_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE,
                    Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.ANCIENT_DEBRIS, Blocks.DEEPSLATE_IRON_ORE);
            long[] rewards = {10, 10, 10, 17, 25, 25, 25, 25, 50, 50, 200, 17};
            long total = 0;
            for (int i = 0; i < blocks.size(); i++) {
                var pos = f.set(4, 0, 4, blocks.get(i).defaultBlockState());
                check(player.gameMode.destroyBlock(pos), "Native ore removal succeeds"); total += rewards[i];
                check(earned(player, "mining") == total, "Each original ore block pays exactly its configured reward: " + blocks.get(i));
            }
            var enchants = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            for (var enchantment : List.of(Enchantments.SILK_TOUCH, Enchantments.FORTUNE)) {
                tool(player, Items.DIAMOND_PICKAXE); player.getMainHandItem().enchant(enchants.getOrThrow(enchantment), 1);
                player.gameMode.destroyBlock(f.set(4, 0, 4, Blocks.DIAMOND_ORE.defaultBlockState())); total += 50;
                check(earned(player, "mining") == total, "Silk Touch and Fortune do not change original-block XP");
            }
            tool(player, Items.WOODEN_PICKAXE); player.gameMode.destroyBlock(f.set(4, 0, 4, Blocks.DIAMOND_ORE.defaultBlockState()));
            check(earned(player, "mining") == total, "Incorrect harvest tool gives no Mining XP even when block is removed");
            tool(player, Items.DIAMOND_PICKAXE);
            var cancelled = f.set(4, 0, 4, Blocks.IRON_ORE.defaultBlockState());
            Consumer<BreakBlockEvent> cancel = event -> { if (event.getPos().equals(cancelled)) { event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancel);
            try { check(!player.gameMode.destroyBlock(cancelled), "Final break cancellation prevents removal"); }
            finally { NeoForge.EVENT_BUS.unregister(cancel); }
            check(earned(player, "mining") == total && helper.getLevel().getBlockState(cancelled).is(Blocks.IRON_ORE), "Cancelled harvesting spends no block or XP");
            player.setGameMode(GameType.CREATIVE); player.gameMode.destroyBlock(cancelled);
            check(earned(player, "mining") == total, "Creative breaking does not progress professions");
            player.setGameMode(GameType.SURVIVAL);
            var fake = FakePlayerFactory.getMinecraft(helper.getLevel()); tool(fake, Items.DIAMOND_PICKAXE);
            fake.gameMode.destroyBlock(f.set(4, 0, 4, Blocks.IRON_ORE.defaultBlockState()));
            check(earned(fake, "mining") == 0, "Fake-player automation does not earn activity XP");
            check(!PlayerProfiles.get(player).created() && earned(player, "farming") == 0, "Natural mining remains classless and independent");
            var state = player.getData(RpgData.PROFESSIONS);
            player.setData(RpgData.PROFESSIONS, state.with(ProfessionData.id("mining"), new ProfessionState.Track(20, 0)));
            player.gameMode.destroyBlock(f.set(4, 0, 4, Blocks.IRON_ORE.defaultBlockState()));
            check(player.getData(RpgData.PROFESSIONS).tracks().get(ProfessionData.id("mining")).experience() == 0, "Natural awards do not bank beyond caps");
            helper.succeed();
        }
    }

    static void placement(GameTestHelper helper) {
        try (var f = new ActivityFixture(helper, 150)) {
            var placer = f.player(); var miner = f.player();
            var pos = f.placeOre(placer, 4, 0, 4);
            check(ProfessionHarvesting.isPlaced(helper.getLevel(), pos), "Actual BlockItem use records placed ore");
            var chunk = helper.getLevel().getChunkAt(pos);
            var disk = SerializableChunkData.copyOf(helper.getLevel(), chunk).write();
            var decoded = SerializableChunkData.parse(helper.getLevel(), helper.getLevel().palettedContainerFactory(), disk);
            check(decoded != null && decoded.attachmentData() != null, "Native chunk serialization includes placement exclusions");
            var loaded = decoded.read(helper.getLevel(), helper.getLevel().getPoiManager(),
                    new RegionStorageInfo("covenant-activity-test", helper.getLevel().dimension(), "chunk"), chunk.getPos());
            check(loaded.hasData(RpgData.PLACED_ORES) && !loaded.hasData(RpgData.HARVEST_TREES), "Native chunk load restores provenance without saving tree evidence");
            chunk.removeData(RpgData.PLACED_ORES);
            chunk.readAttachmentsFromNBT(helper.getLevel().registryAccess(), decoded.attachmentData());
            check(ProfessionHarvesting.isPlaced(helper.getLevel(), pos), "Placement exclusion survives native save/load");
            tool(miner, Items.DIAMOND_PICKAXE); miner.gameMode.destroyBlock(pos);
            check(earned(miner, "mining") == 0, "A different player cannot earn XP from saved placed ore");
            check(!ProfessionHarvesting.isPlaced(helper.getLevel(), pos), "Removed ore cleans its exclusion");
            f.set(4, 0, 4, Blocks.IRON_ORE.defaultBlockState()); miner.gameMode.destroyBlock(pos);
            check(earned(miner, "mining") == 17, "A new unplaced ore block is not excluded by stale history");
            var rejected = f.pos(5, 0, 4);
            Consumer<BlockEvent.EntityPlaceEvent> cancel = event -> { if (event.getPos().equals(rejected)) { event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancel);
            try { f.placeOre(placer, 5, 0, 4); } finally { NeoForge.EVENT_BUS.unregister(cancel); }
            check(helper.getLevel().getBlockState(rejected).isAir() && !ProfessionHarvesting.isPlaced(helper.getLevel(), rejected), "Cancelled item placement rolls back without a provenance marker");
            helper.succeed();
        }
    }

    static void piston(GameTestHelper helper) {
        var f = new ActivityFixture(helper, 160);
        try {
            var placer = f.player(); var miner = f.player(); var source = f.placeOre(placer, 5, 0, 4);
            var piston = f.set(4, 0, 4, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
            f.set(4, -1, 4, Blocks.REDSTONE_BLOCK.defaultBlockState());
            helper.getLevel().updateNeighborsAt(piston.below(), Blocks.REDSTONE_BLOCK, null);
            helper.runAfterDelay(8, () -> {
                try {
                    var destination = source.east();
                    check(helper.getLevel().getBlockState(destination).is(Blocks.IRON_ORE), "Native piston finished moving placed ore");
                    check(!ProfessionHarvesting.isPlaced(helper.getLevel(), source) && ProfessionHarvesting.isPlaced(helper.getLevel(), destination), "Exclusion follows native piston movement and final block restoration");
                    f.set(4, -1, 4, Blocks.AIR.defaultBlockState());
                    helper.getLevel().updateNeighborsAt(piston.below(), Blocks.REDSTONE_BLOCK, null);
                    helper.runAfterDelay(8, () -> {
                        try {
                            check(helper.getLevel().getBlockState(source).is(Blocks.IRON_ORE), "Sticky piston retracts the ore to its source");
                            check(ProfessionHarvesting.isPlaced(helper.getLevel(), source) && !ProfessionHarvesting.isPlaced(helper.getLevel(), destination),
                                    "Exclusion follows native retraction without stale destination evidence");
                            tool(miner, Items.DIAMOND_PICKAXE); miner.gameMode.destroyBlock(source);
                            check(earned(miner, "mining") == 0, "Piston movement cannot launder placed ore into XP");
                            helper.succeed();
                        } finally { f.close(); }
                    });
                } catch (RuntimeException | Error error) { f.close(); throw error; }
            });
        } catch (RuntimeException | Error error) { f.close(); throw error; }
    }

    static void crops(GameTestHelper helper) {
        try (var f = new ActivityFixture(helper, 170)) {
            var player = f.player();
            var blocks = List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS, Blocks.NETHER_WART);
            for (var block : blocks) {
                var property = (net.minecraft.world.level.block.state.properties.IntegerProperty) block.getStateDefinition().getProperty("age");
                int max = Collections.max(property.getPossibleValues());
                f.set(4, -1, 4, block == Blocks.NETHER_WART ? Blocks.SOUL_SAND.defaultBlockState() : Blocks.FARMLAND.defaultBlockState());
                long before = earned(player, "farming");
                player.gameMode.destroyBlock(f.set(4, 0, 4, block.defaultBlockState().setValue(property, max - 1)));
                check(earned(player, "farming") == before, "Immature crops do not award Farming XP");
                var pos = f.set(4, 0, 4, block.defaultBlockState().setValue(property, max));
                player.gameMode.destroyBlock(pos);
                check(earned(player, "farming") == before + 5, "Each supported mature crop pays once");
                player.gameMode.destroyBlock(pos);
                check(earned(player, "farming") == before + 5, "Repeated break request cannot pay for a harvested crop twice");
            }
            long before = earned(player, "farming");
            helper.getLevel().destroyBlock(f.set(4, 0, 4, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7)), true);
            check(earned(player, "farming") == before, "Unattributed destruction does not grant harvesting XP");
            helper.succeed();
        }
    }

    static void logging(GameTestHelper helper) {
        try (var f = new ActivityFixture(helper, 180)) {
            var player = f.player(); tool(player, Items.IRON_AXE);
            f.tree(4, 4, false);
            for (int y = 0; y < 4; y++) { player.gameMode.destroyBlock(f.pos(4, y, 4)); }
            check(earned(player, "logging") == 80, "One recognized tree rewards every manually chopped log after the base disappears");
            f.tree(8, 4, true); player.gameMode.destroyBlock(f.pos(8, 0, 4));
            check(earned(player, "logging") == 80, "Persistent player-placed leaves do not establish a natural canopy");
            f.set(4, 0, 7, Blocks.OAK_LOG.defaultBlockState()); player.gameMode.destroyBlock(f.pos(4, 0, 7));
            check(earned(player, "logging") == 80, "Isolated construction logs do not earn Logging XP");
            f.tree(4, 9, false); player.gameMode.destroyBlock(f.pos(4, 0, 9));
            check(earned(player, "logging") == 100, "A second supported tree reaches Level 1");
            // Replacing a certified upper log must revoke evidence; its floating remainder cannot establish a new base.
            f.set(4, 1, 9, Blocks.AIR.defaultBlockState()); f.set(4, 1, 9, Blocks.OAK_LOG.defaultBlockState());
            player.gameMode.destroyBlock(f.pos(4, 1, 9));
            check(earned(player, "logging") == 100, "Replacing a cached log cannot recycle its old tree eligibility");
            f.tree(4, 13, false);
            for (int x = 5; x < 11; x++) { f.set(x, 3, 13, Blocks.OAK_LOG.defaultBlockState()); }
            player.gameMode.destroyBlock(f.pos(4, 0, 13));
            check(earned(player, "logging") == 100, "Overwide connected structures fail bounded recognition without rewards");
            helper.succeed();
        }
    }

    static void breeding(GameTestHelper helper) {
        try (var f = new ActivityFixture(helper, 230)) {
            var first = f.player(); var second = f.player();
            var cow = f.entities.mob(EntityType.COW); var partner = f.entities.mob(EntityType.COW);
            cow.setPos(Vec3.atBottomCenterOf(f.pos(4, 0, 4))); partner.setPos(Vec3.atBottomCenterOf(f.pos(5, 0, 4)));
            cow.setInLove(first); partner.setInLove(second);
            check(earned(first, "farming") == 0 && earned(second, "farming") == 0, "Feeding/love preparation alone gives no XP");
            Consumer<BabyEntitySpawnEvent> cancel = event -> { if (event.getParentA() == cow) { event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancel);
            try { cow.spawnChildFromBreeding(helper.getLevel(), partner); } finally { NeoForge.EVENT_BUS.unregister(cancel); }
            check(earned(first, "farming") == 0 && earned(second, "farming") == 0, "Cancelled breeding pays nobody");
            cow.setAge(0); partner.setAge(0); cow.setInLove(first); partner.setInLove(second);
            cow.spawnChildFromBreeding(helper.getLevel(), partner);
            check(earned(first, "farming") == 100 && earned(second, "farming") == 0, "Only the native credited breeder earns one successful-event reward");
            var uncredited = f.entities.mob(EntityType.COW); var other = f.entities.mob(EntityType.COW);
            uncredited.setPos(Vec3.atBottomCenterOf(f.pos(6, 0, 4))); other.setPos(Vec3.atBottomCenterOf(f.pos(7, 0, 4)));
            uncredited.setInLove(null); other.setInLove(null);
            uncredited.spawnChildFromBreeding(helper.getLevel(), other);
            check(earned(first, "farming") == 100 && earned(second, "farming") == 0, "Breeding without a credited player pays nobody");
            helper.succeed();
        }
    }

    static void enchanting(GameTestHelper helper) {
        try (var f = new ActivityFixture(helper, 250)) {
            var player = f.player(); var pos = f.set(4, 0, 4, Blocks.ENCHANTING_TABLE.defaultBlockState());
            var menu = new EnchantmentMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), pos));
            long[] rewards = {20, 50, 100}; long total = 0;
            for (int option = 0; option < 3; option++) {
                player.experienceLevel = 50;
                menu.getSlot(0).set(new ItemStack(Items.DIAMOND_PICKAXE));
                menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
                // Deliberately identical displayed requirements prove reward choice depends on option, not requirement.
                menu.costs[option] = 30;
                check(menu.clickMenuButton(player, option), "Native enchanting option completes"); total += rewards[option];
                check(earned(player, "crafting") == total, "Fixed selected-option reward: " + option);
                check(player.experienceLevel == 50 - option - 1 && menu.getSlot(1).getItem().getCount() == 2 - option, "Native lapis and level costs remain intact");
                check(!menu.getSlot(0).getItem().getEnchantments().isEmpty(), "Enchanting produced real native enchantments");
                menu.clickMenuButton(player, option);
                check(earned(player, "crafting") == total, "Replaying an already-enchanted item does not duplicate XP");
            }
            menu.getSlot(0).set(new ItemStack(Items.DIAMOND_PICKAXE)); menu.getSlot(1).set(ItemStack.EMPTY);
            check(!menu.clickMenuButton(player, 0) && earned(player, "crafting") == total, "Unaffordable enchanting gives no XP");
            helper.succeed();
        }
    }

    static final class ActivityFixture implements AutoCloseable {
        final GameTestHelper helper;
        final ProgressionGameTests.Fixture entities;
        final Set<BlockPos> edits = new HashSet<>();
        final int height;
        ActivityFixture(GameTestHelper helper, int height) { this.helper = helper; this.height = height; entities = new ProgressionGameTests.Fixture(helper); }
        ServerPlayer player() { var player = entities.player(null); player.setPos(Vec3.atBottomCenterOf(pos(2, 0, 2))); return player; }
        BlockPos pos(int x, int y, int z) { return helper.absolutePos(new BlockPos(x, height + y, z)); }
        BlockPos set(int x, int y, int z, BlockState state) {
            var pos = pos(x, y, z); edits.add(pos); helper.getLevel().setBlock(pos, state, 2); return pos;
        }
        BlockPos placeOre(ServerPlayer player, int x, int y, int z) {
            var ground = set(x, y - 1, z, Blocks.STONE.defaultBlockState());
            var pos = set(x, y, z, Blocks.AIR.defaultBlockState());
            player.setPos(Vec3.atBottomCenterOf(pos.offset(-2, 0, 0)));
            var stack = new ItemStack(Items.IRON_ORE); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(ground).add(0, .5, 0), Direction.UP, ground, false));
            return pos;
        }
        void tree(int x, int z, boolean persistent) {
            set(x, -1, z, Blocks.DIRT.defaultBlockState());
            for (int y = 0; y < 4; y++) { set(x, y, z, Blocks.OAK_LOG.defaultBlockState()); }
            set(x, 4, z, Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, persistent));
            set(x + 1, 3, z, Blocks.OAK_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, persistent));
        }
        @Override public void close() {
            for (var pos : edits) { helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2); }
            // Include native piston destinations and entities produced by breeding within this fixture's lane.
            helper.getLevel().setBlock(pos(6, 0, 4), Blocks.AIR.defaultBlockState(), 2);
            for (var entity : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                    new net.minecraft.world.phys.AABB(Vec3.atLowerCornerOf(pos(0, -2, 0)), Vec3.atLowerCornerOf(pos(16, 10, 16))))) { entity.discard(); }
            entities.close();
        }
    }
}
