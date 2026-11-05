package dev.covenant.mixin.client;

import dev.covenant.client.BowShotAnimation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** LocalPlayer overrides these two getters; apply the same render-only pose to its overrides. */
@Mixin(LocalPlayer.class)
abstract class LocalBowUsePoseMixin {
    @Inject(method = "isUsingItem", at = @At("HEAD"), cancellable = true)
    private void covenant$bowPose(CallbackInfoReturnable<Boolean> callback) {
        if (BowShotAnimation.isDrawing((LocalPlayer) (Object) this)) { callback.setReturnValue(true); }
    }

    @Inject(method = "getUsedItemHand", at = @At("HEAD"), cancellable = true)
    private void covenant$bowHand(CallbackInfoReturnable<InteractionHand> callback) {
        if (BowShotAnimation.isDrawing((LocalPlayer) (Object) this)) {
            callback.setReturnValue(InteractionHand.MAIN_HAND);
        }
    }
}
