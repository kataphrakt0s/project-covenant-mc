package dev.covenant.mixin;

import dev.covenant.rpg.stats.EquipmentHealth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.1.2 loads/clamps living health before loading player inventory. NeoForge exposes no event
 * with the original ValueInput here; restore its Health only after equipment is available.
 */
@Mixin(Player.class)
abstract class PlayerHealthLoadMixin {
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void covenant$restoreEquipmentHealth(ValueInput input, CallbackInfo callback) {
        Player player = (Player) (Object) this;
        EquipmentHealth.restoreAfterLoad(player, input.getFloatOr("Health", player.getHealth()));
    }
}
