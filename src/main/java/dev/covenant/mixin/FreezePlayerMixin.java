package dev.covenant.mixin;

import dev.covenant.rpg.skills.SkillControls;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class FreezePlayerMixin {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isSwimming()Z"))
    private boolean covenant$freezeSwimSteering(Player player, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        return !SkillControls.blocksMovement(player) && original.call(player);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void covenant$freezeAttack(Entity target, CallbackInfo ci) {
        if (!SkillControls.canAct((Player) (Object) this)) { ci.cancel(); }
    }

    @Inject(method = "stabAttack", at = @At("HEAD"), cancellable = true)
    private void covenant$freezeStab(EquipmentSlot slot, Entity target, float damage, boolean dealsDamage,
            boolean dealsKnockback, boolean dismounts, CallbackInfoReturnable<Boolean> ci) {
        if (!SkillControls.canAct((Player) (Object) this)) { ci.setReturnValue(false); }
    }

    @Inject(method = "cannotAttackWithItem", at = @At("HEAD"), cancellable = true)
    private void covenant$freezeAttackIntent(ItemStack item, int tolerance, CallbackInfoReturnable<Boolean> ci) {
        if (!SkillControls.canAct((Player) (Object) this)) { ci.setReturnValue(true); }
    }
}
