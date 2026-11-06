package dev.covenant.mixin;

import dev.covenant.rpg.skills.SkillControls;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class RootJumpMixin {
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void covenant$rootJump(CallbackInfo ci) {
        if ((SkillControls.blocksMovement((LivingEntity) (Object) this)
                || dev.covenant.rpg.skills.Combos.moving((LivingEntity) (Object) this))) { ci.cancel(); }
    }

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;jumpInFluid(Lnet/neoforged/neoforge/fluids/FluidType;)V"), require = 3)
    private void covenant$rootSwimJump(LivingEntity target, net.neoforged.neoforge.fluids.FluidType fluid,
            Operation<Void> original) {
        if (!SkillControls.blocksMovement(target)
                && !dev.covenant.rpg.skills.Combos.moving(target)) { original.call(target, fluid); }
    }

    // Gliding normally adds look-directed lift/steering without calling moveRelative.
    @WrapOperation(method = "travel", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z"))
    private boolean covenant$controlledGliding(LivingEntity target, Operation<Boolean> original) {
        return !SkillControls.blocksMovement(target) && original.call(target);
    }
}
