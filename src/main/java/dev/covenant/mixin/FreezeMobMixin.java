package dev.covenant.mixin;

import dev.covenant.rpg.skills.Auras;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stop native goals/brains/navigation and attack controllers, leaving travel and external physics intact. */
@Mixin(Mob.class)
abstract class FreezeMobMixin {
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void covenant$freezeMelee(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity target,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
        if (Auras.frozen((Mob) (Object) this)) { ci.setReturnValue(false); }
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void covenant$freezeAi(CallbackInfo ci) {
        if (Auras.frozen((Mob) (Object) this)) { ci.cancel(); }
    }
}
