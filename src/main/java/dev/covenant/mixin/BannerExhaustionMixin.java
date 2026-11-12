package dev.covenant.mixin;

import dev.covenant.rpg.skills.WarBanners;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Banner coverage prevents native action exhaustion without clearing already-stored exhaustion. */
@Mixin(Player.class)
abstract class BannerExhaustionMixin {
    @Inject(method="causeFoodExhaustion", at=@At("HEAD"), cancellable=true)
    private void covenant$bannerExhaustion(float amount, CallbackInfo ci) {
        var player=(Player)(Object)this;
        if (!player.level().isClientSide() && WarBanners.covered(player)) { ci.cancel(); }
    }
}
