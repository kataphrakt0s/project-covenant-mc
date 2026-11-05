package dev.covenant.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.covenant.client.BowShotAnimation;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;

/** Extract native bow arm poses and item models for the caster and other tracking clients. */
@Mixin(AvatarRenderer.class)
abstract class BowThirdPersonMixin {
    @WrapMethod(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V")
    private void covenant$drawBow(Avatar player, AvatarRenderState state, float partialTick, Operation<Void> original) {
        var previous = BowShotAnimation.enter(player);
        try {
            original.call(player, state, partialTick);
        } finally {
            BowShotAnimation.exit(previous);
        }
    }
}
