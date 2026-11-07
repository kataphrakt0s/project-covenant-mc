package dev.covenant.mixin;

import dev.covenant.rpg.progression.ProfessionHarvesting;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Record placements after every cancellation subscriber has finished, including multi-block placements. */
@Mixin(EventHooks.class)
abstract class ProfessionPlacementMixin {
    @Inject(method = "onBlockPlace", at = @At("RETURN"))
    private static void covenant$placed(Entity entity, BlockSnapshot snapshot, Direction direction, CallbackInfoReturnable<Boolean> ci) {
        if (!ci.getReturnValue()) { ProfessionHarvesting.placed(snapshot); }
    }

    @Inject(method = "onMultiBlockPlace", at = @At("RETURN"))
    private static void covenant$multiPlaced(Entity entity, List<BlockSnapshot> snapshots, Direction direction, CallbackInfoReturnable<Boolean> ci) {
        if (!ci.getReturnValue()) { snapshots.forEach(ProfessionHarvesting::placed); }
    }
}
