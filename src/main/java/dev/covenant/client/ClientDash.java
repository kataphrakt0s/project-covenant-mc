package dev.covenant.client;

import dev.covenant.rpg.skills.DashMotion;
import dev.covenant.rpg.skills.DashMovement;
import dev.covenant.rpg.skills.DashPrediction;
import dev.covenant.rpg.skills.SkillNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** One instance per client player, discarded with that player on disconnect/respawn. */
public final class ClientDash {
    public interface Access { ClientDash covenant$dash(); }
    private DashPrediction prediction = new DashPrediction();
    private net.minecraft.resources.Identifier dimension;
    private boolean originalNoGravity;

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(DashMotion.TYPE, (motion, context) -> {
            if (context.player() instanceof LocalPlayer player
                    && player.level().dimension().identifier().equals(motion.dimension())) {
                ((Access) player).covenant$dash().accept(player, motion);
            }
        });
    }

    private void accept(LocalPlayer player, DashMotion motion) {
        if (!prediction.accept(motion, player.position())) { return; }
        dimension = motion.dimension(); originalNoGravity = motion.noGravity();
        // Handle stop impulses immediately in packet order, before any later native knockback packet.
        player.setNoGravity(motion.active() || motion.noGravity());
        player.setDeltaMovement(motion.active() ? prediction.delta(player.position()) : motion.velocity());
        player.resetFallDistance();
        acknowledge(player);
    }

    /** Native move updates current position without resetting old position, retaining frame interpolation. */
    public boolean travel(LocalPlayer player) {
        if (dimension != null && !dimension.equals(player.level().dimension().identifier())) {
            if (prediction.ownsTravel()) { player.setNoGravity(originalNoGravity); }
            prediction = new DashPrediction(); dimension = null;
            return false;
        }
        if (!prediction.ownsTravel()) { return false; }
        Vec3 requested = prediction.delta(player.position());
        if (prediction.active()) { player.setDeltaMovement(requested); }
        var result = DashMovement.move(player, requested, null, !prediction.arcing());
        prediction.advanced(result.nextStep());
        acknowledge(player);
        return true;
    }

    private void acknowledge(LocalPlayer player) {
        if (prediction.readyToAcknowledge()) {
            // Queued before the next ordinary position packet on this connection.
            ClientPacketDistributor.sendToServer(new SkillNetworking.DashFinished(prediction.acknowledge(), player.position()));
        }
    }

    /** Native teleports own their destination and velocity; only release our temporary travel/gravity ownership. */
    public void teleported(LocalPlayer player) {
        if (prediction.ownsTravel()) { player.setNoGravity(originalNoGravity); }
        prediction.cancel(); dimension = null;
    }
}
