package dev.covenant.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.covenant.client.BowShotAnimation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;

/** NeoForge has no first-person item-use render-state override; scope the native renderer instead. */
@Mixin(ItemInHandRenderer.class)
abstract class BowFirstPersonMixin {
    @WrapMethod(method = "renderHandsWithItems")
    private void covenant$drawBow(float partialTick, PoseStack poseStack, SubmitNodeCollector collector,
                                  LocalPlayer player, int light, Operation<Void> original) {
        var previous = BowShotAnimation.enter(player);
        try {
            original.call(partialTick, poseStack, collector, player, light);
        } finally {
            BowShotAnimation.exit(previous);
        }
    }
}
