package dev.covenant.mixin;

import dev.covenant.rpg.skills.Combos;
import dev.covenant.rpg.skills.SkillControls;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The server advances its own collision steps; the owner predicts through a client-only mixin. */
@Mixin(Player.class)
abstract class ComboPlayerMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void covenant$ownedTravel(Vec3 input, CallbackInfo ci) {
        var player = (Player) (Object) this;
        if (player instanceof net.minecraft.server.level.ServerPlayer server && Combos.blocksExternalMovement(server)) {
            if (Combos.moving(player)) { player.setDeltaMovement(Vec3.ZERO); player.resetFallDistance(); }
            ci.cancel();
        }
    }

    @Inject(method = "isClientAuthoritative", at = @At("HEAD"), cancellable = true)
    private void covenant$controlledPhysics(CallbackInfoReturnable<Boolean> ci) {
        if ((Object) this instanceof net.minecraft.server.level.ServerPlayer player && SkillControls.ownsPhysics(player)) {
            ci.setReturnValue(false);
        }
    }
}
