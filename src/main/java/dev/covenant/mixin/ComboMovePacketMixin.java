package dev.covenant.mixin;

import dev.covenant.rpg.skills.Combos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ComboMovePacketMixin {
    @Shadow public ServerPlayer player;

    // Run after vanilla's thread handoff. Ignore predicted positions through the endpoint acknowledgement.
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void covenant$ownedPosition(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!Combos.blocksMove(player)) { return; }
        float yaw = packet.getYRot(player.getYRot()), pitch = packet.getXRot(player.getXRot());
        if (Float.isFinite(yaw) && Float.isFinite(pitch)) {
            player.setYRot(Mth.wrapDegrees(yaw)); player.setXRot(Mth.clamp(pitch, -90, 90));
        }
        ci.cancel();
    }
}
