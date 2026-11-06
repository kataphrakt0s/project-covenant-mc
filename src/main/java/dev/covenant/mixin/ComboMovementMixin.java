package dev.covenant.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.covenant.rpg.skills.Combos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the combo's native collision step may move the server player during its brief travel. */
@Mixin(Entity.class)
abstract class ComboMovementMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void covenant$ownMovement(MoverType type, Vec3 delta, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && Combos.blocksExternalMovement(player)) { ci.cancel(); }
    }

    @WrapOperation(method = "saveWithoutId", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isNoGravity()Z"))
    private boolean covenant$saveGravity(Entity entity, Operation<Boolean> original) {
        return entity instanceof ServerPlayer player ? Combos.savedNoGravity(player) : original.call(entity);
    }
}
