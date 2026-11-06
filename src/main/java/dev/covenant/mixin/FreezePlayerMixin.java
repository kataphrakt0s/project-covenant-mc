package dev.covenant.mixin;

import dev.covenant.rpg.skills.Auras;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class FreezePlayerMixin {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isSwimming()Z"))
    private boolean covenant$freezeSwimSteering(Player player, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        return !Auras.frozen(player) && original.call(player);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void covenant$freezeAttack(Entity target, CallbackInfo ci) {
        if (Auras.frozen((Player) (Object) this)) { ci.cancel(); }
    }
}
