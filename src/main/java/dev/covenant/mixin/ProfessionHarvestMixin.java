package dev.covenant.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.covenant.rpg.progression.ProfessionHarvesting;
import dev.covenant.rpg.progression.ProfessionGathering;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;

/** Native canHarvest + actual removal is stronger evidence than BreakEvent or destroyBlock's return value. */
@Mixin(ServerPlayerGameMode.class)
abstract class ProfessionHarvestMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private BlockPos covenant$origin;
    @Unique private ProfessionGathering.Batch covenant$batch;
    @Unique private BlockPos covenant$confirmed;

    @WrapMethod(method = "destroyBlock")
    private boolean covenant$gather(BlockPos pos, Operation<Boolean> original) {
        if (covenant$origin != null) { return original.call(pos); }
        covenant$origin = pos.immutable();
        try {
            boolean result = original.call(pos);
            var batch = covenant$batch;
            covenant$batch = null;
            if (result && batch != null) { batch.harvest(player, this::covenant$harvestExtra); }
            return result;
        } finally {
            covenant$batch = null; covenant$origin = null; covenant$confirmed = null;
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
        var batch = pos.equals(covenant$origin) ? ProfessionGathering.prepare(player, pos, state, canHarvest, tool) : null;
        boolean removed = original.call(pos, state, canHarvest, tool);
        if (removed && player.level().getBlockState(pos) != state) {
            covenant$confirmed = pos.immutable();
            ProfessionHarvesting.removed(player, award);
            if (batch != null) { covenant$batch = batch; }
        }
        return removed;
    }
}
