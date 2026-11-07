package dev.covenant.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.covenant.rpg.progression.ProfessionHarvesting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;

/** Native canHarvest + actual removal is stronger evidence than BreakEvent or destroyBlock's return value. */
@Mixin(ServerPlayerGameMode.class)
abstract class ProfessionHarvestMixin {
    @Shadow @Final protected ServerPlayer player;

    @WrapMethod(method = "removeBlock")
    private boolean covenant$harvest(BlockPos pos, BlockState state, boolean canHarvest, ItemStack tool, Operation<Boolean> original) {
        var award = ProfessionHarvesting.beforeRemoval(player, pos, state, canHarvest);
        boolean removed = original.call(pos, state, canHarvest, tool);
        if (removed && player.level().getBlockState(pos) != state) { ProfessionHarvesting.removed(player, award); }
        return removed;
    }
}
