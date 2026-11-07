package dev.covenant.mixin;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.progression.ProfessionHarvesting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Constant-time invalidation only on chunks that already carry relevant evidence. */
@Mixin(LevelChunk.class)
abstract class ProfessionChunkMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void covenant$changed(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> ci) {
        var chunk = (LevelChunk) (Object) this;
        if (ci.getReturnValue() != null && (chunk.hasData(RpgData.PLACED_ORES) || chunk.hasData(RpgData.HARVEST_TREES))) {
            ProfessionHarvesting.changed(chunk, pos, state);
        }
    }
}
