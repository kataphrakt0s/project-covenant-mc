package dev.covenant.rpg.progression;

import dev.covenant.rpg.RpgData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** One synchronous hoe interaction, bounded to 81 loaded positions. No saved jobs or per-tick work. */
public final class ProfessionFarming {
    private ProfessionFarming() {}
    private record Target(BlockPos pos, BlockState state, HarvestDefinitions.Crop crop) {}

    /** Called after all native right-click event subscribers; a cancelled/denied interaction never starts work. */
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getUseItem().isFalse() || event.getUseBlock().isFalse()
                || event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)
                || !participant(player) || !hoe(player.getMainHandItem()) || player.getCooldowns().isOnCooldown(player.getMainHandItem())) { return; }
        try (var zone = Profiler.get().zone("covenant:farming_harvest")) {
            var level = player.level(); var origin = event.getPos();
            var snapshot = ProfessionData.snapshot(level.getServer());
            var first = target(level, origin, snapshot.catalogue().harvesting().crops());
            if (first == null) { return; } // Ordinary hoe use is unchanged outside supported mature crops.
            var track = player.getData(RpgData.PROFESSIONS).tracks().get(ProfessionData.id("farming"));
            var scything = snapshot.catalogue().abilities().get(ProfessionData.SCYTHING);
            int rank = track.rank(ProfessionData.SCYTHING);
            int width = player.isShiftKeyDown() || rank == 0 || scything == null ? 1
                    : scything.ranks().get(Math.min(rank, scything.ranks().size()) - 1).area();
            boolean replant = track.rank(ProfessionData.FARM_REPLANT) > 0
                    && snapshot.catalogue().abilities().containsKey(ProfessionData.FARM_REPLANT);
            var targets = new ArrayList<Target>(); targets.add(first);
            int radius = width / 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) { continue; }
                    var next = target(level, origin.offset(dx, 0, dz), snapshot.catalogue().harvesting().crops());
                    if (next != null) { targets.add(next); }
                }
            }
            // Consume this interaction even when a mature target's native harvest is refused.
            event.setCanceled(true); event.setCancellationResult(InteractionResult.CONSUME);
            var tool = player.getMainHandItem(); boolean harvested = false; int missingSupplies = 0;
            for (var target : targets) {
                if (!participant(player) || player.level() != level || player.getMainHandItem() != tool || !hoe(tool)
                        || read(level, target.pos()) != target.state()
                        || !level.getWorldBorder().isWithinBounds(target.pos()) || !level.mayInteract(player, target.pos())
                        || target.state().getDestroySpeed(level, target.pos()) < 0
                        || !target.state().canHarvestBlock(level, target.pos(), player)) { break; }
                if (!((ProfessionGathering.HarvestAccess) player.gameMode).covenant$harvestCrop(target.pos())) { break; }
                harvested = true;
                var beforeWear = tool.copy();
                tool.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
                if (tool.isEmpty()) { EventHooks.onPlayerDestroyItem(player, beforeWear, InteractionHand.MAIN_HAND); }
                // Finish this crop even if its harvest broke the hoe; never use a replacement tool for more crops.
                if (replant && participant(player) && player.level() == level && !replant(player, target)) { missingSupplies++; }
            }
            if (harvested) { player.swing(InteractionHand.MAIN_HAND); }
            if (missingSupplies > 0) { player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("profession.covenant.replant_supplies", missingSupplies), true); }
            player.inventoryMenu.broadcastChanges();
        }
    }

    private static boolean participant(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator() && !(player instanceof FakePlayer);
    }
    private static boolean hoe(ItemStack stack) { return !stack.isEmpty() && stack.is(ItemTags.HOES); }
    private static BlockState read(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) { return null; }
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }
    private static Target target(ServerLevel level, BlockPos pos, List<HarvestDefinitions.Crop> crops) {
        var state = read(level, pos);
        if (state == null) { return null; }
        for (var crop : crops) {
            if (crop.plantingItem() != null && crop.test(state)) { return new Target(pos.immutable(), state, crop); }
        }
        return null;
    }

    /** Native item placement supplies survival checks, placement events/rollback, sound and game events. */
    private static boolean replant(ServerPlayer player, Target target) {
        var level = player.level(); var ground = target.pos().below();
        var state = read(level, target.pos());
        if (state == null || !state.isAir() || read(level, ground) == null
                || !level.mayInteract(player, target.pos()) || !level.mayInteract(player, ground)
                || !level.getWorldBorder().isWithinBounds(target.pos())) { return true; }
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var supply = inventory.getItem(slot);
            if (supply.isEmpty() || !supply.is(target.crop().plantingItem())) { continue; }
            // A context-owned copy avoids temporarily replacing the held hoe and never takes seeds from drops.
            var planting = supply.copyWithCount(1);
            planting.remove(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            var hit = new BlockHitResult(Vec3.atCenterOf(ground).add(0, .5, 0), Direction.UP, ground, false);
            var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, planting, hit);
            if (planting.useOn(context).consumesAction() && planting.isEmpty()) { supply.shrink(1); inventory.setChanged(); }
            return true; // Native placement denial is not mislabeled as missing supplies.
        }
        return false;
    }
}
