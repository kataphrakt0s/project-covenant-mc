package dev.covenant.mixin.client;

import dev.covenant.client.ClientDash;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An exceptional native correction or teleport supersedes any previously received dash path. */
@Mixin(ClientPacketListener.class)
abstract class DashTeleportMixin {
    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void covenant$releasePrediction(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null) { ((ClientDash.Access) player).covenant$dash().teleported(player); }
    }
}
