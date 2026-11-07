package dev.covenant.mixin;

import dev.covenant.rpg.progression.ProfessionHarvesting;
import net.minecraft.advancements.criterion.EnchantedItemTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native enchanting completion runs after material/level spending, once regardless of enchantment count. */
@Mixin(EnchantedItemTrigger.class)
abstract class ProfessionEnchantingMixin {
    @Inject(method = "trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;I)V", at = @At("TAIL"))
    private void covenant$enchanted(ServerPlayer player, ItemStack item, int option, CallbackInfo ci) {
        ProfessionHarvesting.enchanted(player, option);
    }
}
