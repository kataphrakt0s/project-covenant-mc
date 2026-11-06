package dev.covenant.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.covenant.rpg.skills.Combos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Observe the resulting impulse after cancellation/resistance, preserving knockback that took effect. */
@Mixin(LivingEntity.class)
abstract class ComboKnockbackMixin {
    @WrapMethod(method = "knockback")
    private void covenant$knockback(double strength, double x, double z, Operation<Void> original) {
        var target = (LivingEntity) (Object) this;
        var before = target.getDeltaMovement();
        original.call(strength, x, z);
        if (target instanceof ServerPlayer player && !before.equals(player.getDeltaMovement())) {
            Combos.interruptMovement(player, true);
        }
    }
}
