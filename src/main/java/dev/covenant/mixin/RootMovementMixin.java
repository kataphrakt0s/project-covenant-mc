package dev.covenant.mixin;

import dev.covenant.rpg.skills.SkillControls;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppress self propulsion on both logical sides, including native flying-mob travel. */
@Mixin(Entity.class)
abstract class RootMovementMixin {
    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
    private void covenant$rootInput(float speed, Vec3 input, CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity living && SkillControls.blocksMovement(living)) { ci.cancel(); }
    }
}
