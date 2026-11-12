package dev.covenant.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.covenant.rpg.skills.WarBanners;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Native food ticking includes regeneration exhaustion which bypasses Player.causeFoodExhaustion. */
@Mixin(FoodData.class)
abstract class BannerFoodTickMixin {
    @Shadow private float exhaustionLevel;
    @WrapMethod(method="tick")
    private void covenant$protectedFoodTick(ServerPlayer player, Operation<Void> original) {
        if (!WarBanners.covered(player)) { original.call(player); return; }
        float previous=exhaustionLevel;
        exhaustionLevel=0;
        try { original.call(player); }
        finally { exhaustionLevel=previous; }
    }
}
