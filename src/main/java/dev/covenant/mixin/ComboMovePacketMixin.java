package dev.covenant.mixin;

import dev.covenant.rpg.skills.Combos;
import dev.covenant.rpg.skills.SkillControls;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Set;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ComboMovePacketMixin {
    @Shadow public ServerPlayer player;
    @Shadow private Vec3 awaitingPositionFromClient;
    @Shadow private int awaitingTeleport;

    // Run after vanilla's thread handoff. Ignore predicted positions through the endpoint acknowledgement.
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void covenant$ownedPosition(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        // Invalid packets must still reach vanilla validation, including during active dash ownership.
        if (!Double.isFinite(packet.getX(player.getX())) || !Double.isFinite(packet.getY(player.getY()))
                || !Double.isFinite(packet.getZ(player.getZ())) || !Float.isFinite(packet.getYRot(player.getYRot()))
                || !Float.isFinite(packet.getXRot(player.getXRot()))) { return; }
        if (awaitingPositionFromClient != null) {
            if (SkillControls.isControlCorrection(player, awaitingTeleport)) { awaitingPositionFromClient = player.position(); }
            return; // Vanilla owns teleport acknowledgement/retry; never replace an outstanding teleport.
        }
        if (!Combos.blocksMove(player) && !SkillControls.interceptMove(player, packet)) { return; }
        float yaw = packet.getYRot(player.getYRot()), pitch = packet.getXRot(player.getXRot());
        if (Float.isFinite(yaw) && Float.isFinite(pitch)) {
            player.setYRot(Mth.wrapDegrees(yaw)); player.setXRot(Mth.clamp(pitch, -90, 90));
        }
        ci.cancel();
    }

    @Inject(method = "handleAcceptTeleportPacket", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER))
    private void covenant$acceptControlCorrection(ServerboundAcceptTeleportationPacket packet, CallbackInfo ci) {
        if (awaitingPositionFromClient != null && packet.getId() == awaitingTeleport
                && SkillControls.isControlCorrection(player, awaitingTeleport)) {
            // Native physics may have advanced since the correction was sent. Its acknowledgement must not rewind that movement.
            awaitingPositionFromClient = player.position();
            SkillControls.correctionAcknowledged(player);
        }
    }

    @WrapOperation(method = "updateAwaitingTeleport", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"))
    private void covenant$retryControlCorrection(ServerGamePacketListenerImpl connection, double x, double y, double z,
            float yaw, float pitch, Operation<Void> original) {
        if (SkillControls.isControlCorrection(player, awaitingTeleport)) { SkillControls.correctPosition(player, yaw, pitch); }
        else { original.call(connection, x, y, z, yaw, pitch); }
    }

    @Inject(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("HEAD"))
    private void covenant$beforeTeleport(PositionMoveRotation destination, Set<Relative> relatives, CallbackInfo ci) {
        SkillControls.beforeTeleport(player);
    }

    @Inject(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("RETURN"))
    private void covenant$afterTeleport(PositionMoveRotation destination, Set<Relative> relatives, CallbackInfo ci) {
        SkillControls.afterTeleport(player, awaitingTeleport);
    }
}
