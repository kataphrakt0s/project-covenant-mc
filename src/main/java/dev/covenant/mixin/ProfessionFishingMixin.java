package dev.covenant.mixin;

import dev.covenant.rpg.progression.ProfessionFishing;
import java.util.Collection;
import net.minecraft.advancements.criterion.FishingRodHookedTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native trigger receives the actual loot after the cancellable fishing event; entity hooks receive an empty list. */
@Mixin(FishingRodHookedTrigger.class)
abstract class ProfessionFishingMixin {
    @Inject(method = "trigger", at = @At("TAIL"))
    private void covenant$caught(ServerPlayer player, ItemStack rod, FishingHook hook, Collection<ItemStack> loot, CallbackInfo ci) {
        ProfessionFishing.caught(player, hook, loot);
    }
}
