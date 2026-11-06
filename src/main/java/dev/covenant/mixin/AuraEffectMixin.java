package dev.covenant.mixin;

import dev.covenant.rpg.skills.SkillControls;
import dev.covenant.rpg.skills.CovenantSlows;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reconcile after native saved effect changes, including load, expiry, milk, and direct commands. */
@Mixin(LivingEntity.class)
abstract class AuraEffectMixin {
    @Inject(method = {"onEffectAdded", "onEffectUpdated", "onEffectsRemoved"}, at = @At("RETURN"))
    private void covenant$slowChanged(CallbackInfo ci) { CovenantSlows.update((LivingEntity) (Object) this); }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void covenant$loadedSlow(CallbackInfo ci) { CovenantSlows.update((LivingEntity) (Object) this); }

    @Inject(method = "startUsingItem", at = @At("HEAD"), cancellable = true)
    private void covenant$freezeUse(InteractionHand hand, CallbackInfo ci) {
        if (!SkillControls.canAct((LivingEntity) (Object) this)) { ci.cancel(); }
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void covenant$freezeMelee(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> ci) {
        if (!SkillControls.canAct((LivingEntity) (Object) this)) { ci.setReturnValue(false); }
    }

    @Inject(method = "stabAttack", at = @At("HEAD"), cancellable = true)
    private void covenant$freezeStab(EquipmentSlot slot, Entity target, float damage, boolean dealsDamage,
            boolean dealsKnockback, boolean dismounts, CallbackInfoReturnable<Boolean> ci) {
        if (!SkillControls.canAct((LivingEntity) (Object) this)) { ci.setReturnValue(false); }
    }
}
