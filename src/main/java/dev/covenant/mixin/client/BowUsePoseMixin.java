package dev.covenant.mixin.client;

import dev.covenant.client.BowShotAnimation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Render-scoped getters select vanilla's bow pose/model without starting actual item use. */
@Mixin(LivingEntity.class)
abstract class BowUsePoseMixin {
    @Inject(method = "isUsingItem", at = @At("HEAD"), cancellable = true)
    private void covenant$bowPose(CallbackInfoReturnable<Boolean> callback) {
        if (BowShotAnimation.isDrawing((LivingEntity) (Object) this)) { callback.setReturnValue(true); }
    }

    @Inject(method = "getUsedItemHand", at = @At("HEAD"), cancellable = true)
    private void covenant$bowHand(CallbackInfoReturnable<InteractionHand> callback) {
        if (BowShotAnimation.isDrawing((LivingEntity) (Object) this)) {
            callback.setReturnValue(InteractionHand.MAIN_HAND);
        }
    }

    @Inject(method = "getUseItem", at = @At("HEAD"), cancellable = true)
    private void covenant$bowStack(CallbackInfoReturnable<ItemStack> callback) {
        var entity = (LivingEntity) (Object) this;
        if (BowShotAnimation.isDrawing(entity)) { callback.setReturnValue(entity.getMainHandItem()); }
    }

    @Inject(method = "getUseItemRemainingTicks", at = @At("HEAD"), cancellable = true)
    private void covenant$bowRemaining(CallbackInfoReturnable<Integer> callback) {
        var entity = (LivingEntity) (Object) this;
        if (BowShotAnimation.isDrawing(entity)) {
            callback.setReturnValue(entity.getMainHandItem().getUseDuration(entity) - BowShotAnimation.drawTicks(entity));
        }
    }

    @Inject(method = "getTicksUsingItem()I", at = @At("HEAD"), cancellable = true)
    private void covenant$bowElapsed(CallbackInfoReturnable<Integer> callback) {
        var entity = (LivingEntity) (Object) this;
        if (BowShotAnimation.isDrawing(entity)) { callback.setReturnValue(BowShotAnimation.drawTicks(entity)); }
    }
}
