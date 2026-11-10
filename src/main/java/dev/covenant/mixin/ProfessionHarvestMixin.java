package dev.covenant.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.covenant.rpg.progression.ProfessionFarming;
import dev.covenant.rpg.progression.ProfessionHarvesting;
import dev.covenant.rpg.progression.ProfessionGathering;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Native canHarvest + actual removal is stronger evidence than BreakEvent or destroyBlock's return value. */
@Mixin(ServerPlayerGameMode.class)
abstract class ProfessionHarvestMixin implements ProfessionGathering.HarvestAccess {
    @Shadow @Final protected ServerPlayer player;
    @Unique private BlockPos covenant$origin;
    @Unique private ProfessionGathering.Batch covenant$batch;
    @Unique private BlockPos covenant$confirmed;
    @Unique private BlockPos covenant$current;
    @Unique private boolean covenant$farming;
    @Unique private boolean covenant$farmUseActive;

    @Override public ProfessionGathering.Batch covenant$gatheringBatch() { return covenant$batch; }
    @Override public BlockPos covenant$harvestingBlock() { return covenant$current; }

    @Override public boolean covenant$harvestCrop(BlockPos pos) {
        if (covenant$origin != null) { return false; }
        covenant$origin = pos.immutable(); covenant$current = covenant$origin; covenant$farming = true;
        try { return covenant$harvestExtra(pos); }
        finally { covenant$origin = null; covenant$current = null; covenant$confirmed = null; covenant$farming = false; }
    }

    // Observe the final RightClickBlock decision, including cancellation by the last subscriber.
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;onRightClickBlock(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/neoforged/neoforge/event/entity/player/PlayerInteractEvent$RightClickBlock;"))
    private PlayerInteractEvent.RightClickBlock covenant$farmUse(Player player, InteractionHand hand, BlockPos pos,
            BlockHitResult hit, Operation<PlayerInteractEvent.RightClickBlock> original) {
        var event = original.call(player, hand, pos, hit);
        if (!covenant$farmUseActive) {
            covenant$farmUseActive = true;
            try { ProfessionFarming.use(event); } finally { covenant$farmUseActive = false; }
        }
        return event;
    }

    // Farming applies exactly one native durability attempt after confirmed removal, even for modded hoes/crops.
    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;mineBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V"))
    private void covenant$cropWear(ItemStack stack, Level level, BlockState state, BlockPos pos, Player player, Operation<Void> original) {
        if (!covenant$farming || !pos.equals(covenant$current)) { original.call(stack, level, state, pos, player); }
    }

    @WrapMethod(method = "destroyBlock")
    private boolean covenant$gather(BlockPos pos, Operation<Boolean> original) {
        if (covenant$origin != null) {
            var previous = covenant$current; covenant$current = pos.immutable();
            try { return original.call(pos); } finally { covenant$current = previous; }
        }
        covenant$origin = pos.immutable();
        covenant$current = covenant$origin;
        try {
            boolean result = original.call(pos);
            var batch = covenant$batch;
            if (result && batch != null) { batch.harvest(player, this::covenant$harvestExtra); }
            return result;
        } finally {
            covenant$batch = null; covenant$origin = null; covenant$confirmed = null; covenant$current = null;
        }
    }

    @Unique
    private boolean covenant$harvestExtra(BlockPos pos) {
        covenant$confirmed = null;
        return player.gameMode.destroyBlock(pos) && pos.equals(covenant$confirmed);
    }

    @WrapMethod(method = "removeBlock")
    private boolean covenant$harvest(BlockPos pos, BlockState state, boolean canHarvest, ItemStack tool, Operation<Boolean> original) {
        var award = ProfessionHarvesting.beforeRemoval(player, pos, state, canHarvest);
        var batch = !covenant$farming && pos.equals(covenant$origin) ? ProfessionGathering.prepare(player, pos, state, canHarvest, tool) : null;
        boolean removed = original.call(pos, state, canHarvest, tool);
        if (removed && player.level().getBlockState(pos) != state) {
            covenant$confirmed = pos.immutable();
            ProfessionHarvesting.removed(player, award);
            if (batch != null) { covenant$batch = batch; }
        }
        return removed;
    }
}
