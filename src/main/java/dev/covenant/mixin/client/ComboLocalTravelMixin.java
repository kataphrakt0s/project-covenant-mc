package dev.covenant.mixin.client;

import dev.covenant.client.ClientDash;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class ComboLocalTravelMixin implements ClientDash.Access {
    @Unique private final ClientDash covenant$dash = new ClientDash();
    @Override public ClientDash covenant$dash() { return covenant$dash; }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void covenant$predictDash(Vec3 input, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player && covenant$dash.travel(player)) { ci.cancel(); }
    }
}
