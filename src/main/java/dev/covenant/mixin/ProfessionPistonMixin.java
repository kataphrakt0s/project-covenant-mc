package dev.covenant.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.covenant.rpg.progression.ProfessionHarvesting;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Moving placed ore must not turn it into a fresh Mining XP source. */
@Mixin(PistonBaseBlock.class)
abstract class ProfessionPistonMixin {
    @ModifyExpressionValue(method = "moveBlocks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;getToPush()Ljava/util/List;"))
    private List<BlockPos> covenant$capture(List<BlockPos> pushed, Level level, BlockPos pos, Direction direction,
            boolean extending, @Share("provenance") LocalRef<ProfessionHarvesting.PistonMove> move) {
        // Native retraction removes its old head before resolving; use that resolved list rather than a second resolver.
        move.set(ProfessionHarvesting.capturePiston(level, pushed, direction, extending));
        return pushed;
    }

    @Inject(method = "moveBlocks", at = @At("RETURN"))
    private void covenant$moved(Level level, BlockPos pos, Direction direction, boolean extending,
            CallbackInfoReturnable<Boolean> ci, @Share("provenance") LocalRef<ProfessionHarvesting.PistonMove> move) {
        if (ci.getReturnValue() && move.get() != null && level instanceof ServerLevel server) { move.get().complete(server); }
    }
}
