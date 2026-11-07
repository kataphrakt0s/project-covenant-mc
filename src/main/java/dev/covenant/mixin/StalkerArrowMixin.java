package dev.covenant.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.skills.Skills;
import dev.covenant.rpg.skills.StalkerArrows;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Retain native arrow flight, ammunition effects and post-hit handling around one authoritative damage call. */
@Mixin(AbstractArrow.class)
abstract class StalkerArrowMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void covenant$expireFan(CallbackInfo ci) {
        var arrow = (AbstractArrow) (Object) this;
        if (!arrow.level().isClientSide() && arrow.hasData(RpgData.STALKER_SHOT) && StalkerArrows.expired(arrow)) {
            arrow.discard(); ci.cancel();
        }
    }

    @Inject(method = "canHitEntity", at = @At("RETURN"), cancellable = true)
    private void covenant$allies(Entity target, CallbackInfoReturnable<Boolean> cir) {
        var arrow = (AbstractArrow) (Object) this;
        if (arrow.hasData(RpgData.STALKER_SHOT) && target instanceof LivingEntity living
                && arrow.getOwner() instanceof LivingEntity owner && !Skills.canHit(owner, living)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void covenant$claimFan(EntityHitResult hit, CallbackInfo ci) {
        var arrow = (AbstractArrow) (Object) this;
        if (arrow.level().isClientSide() || !arrow.hasData(RpgData.STALKER_SHOT)) { return; }
        if (hit.getEntity() instanceof LivingEntity target
                && (!(arrow.getOwner() instanceof LivingEntity owner) || !Skills.canHit(owner, target)
                    || !StalkerArrows.claim(arrow, target))) {
            arrow.discard(); ci.cancel();
        }
    }

    @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean covenant$magicHit(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
        var arrow = (AbstractArrow) (Object) this;
        if (arrow.level().isClientSide() || !arrow.hasData(RpgData.STALKER_SHOT)) { return original.call(target, source, amount); }
        float health = target instanceof LivingEntity living ? living.getHealth() : 0;
        float absorption = target instanceof LivingEntity living ? living.getAbsorptionAmount() : 0;
        boolean result = original.call(target, StalkerArrows.source(arrow, source), StalkerArrows.damage(arrow, amount));
        if (target instanceof LivingEntity living) { StalkerArrows.landed(arrow, living, health, absorption); }
        return result;
    }
}
