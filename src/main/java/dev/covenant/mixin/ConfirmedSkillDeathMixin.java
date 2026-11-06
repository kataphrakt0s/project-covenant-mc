package dev.covenant.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.covenant.rpg.skills.SkillLifecycle;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** ServerPlayer overrides die without calling LivingEntity; both native paths must confirm cleanup. */
@Mixin({LivingEntity.class, ServerPlayer.class})
abstract class ConfirmedSkillDeathMixin {
    @ModifyExpressionValue(method = "die", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/CommonHooks;onLivingDeath(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private boolean covenant$afterDeathDecision(boolean cancelled) {
        // Even a LOWEST-priority listener can still cancel. This observes the final hook result.
        if (!cancelled) { SkillLifecycle.confirmedDeath((LivingEntity) (Object) this); }
        return cancelled;
    }
}
