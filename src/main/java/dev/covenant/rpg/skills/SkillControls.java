package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import java.util.Set;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Shared control policy. Restricted players retain native server physics, never client position authority. */
public final class SkillControls {
    /** Entity-local, unsaved bookkeeping; no position history or separate movement simulation. */
    public static final class MovementState {
        boolean ticking;
        long nextCorrection;
        int correctionTeleport = -1;
        boolean sendingCorrection;
    }

    private SkillControls() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, SkillControls::beforeTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SkillControls::afterTick);
        NeoForge.EVENT_BUS.addListener(SkillControls::useItem);
        NeoForge.EVENT_BUS.addListener(SkillControls::useBlock);
        NeoForge.EVENT_BUS.addListener(SkillControls::interact);
        NeoForge.EVENT_BUS.addListener(SkillControls::interactSpecific);
    }

    public static boolean canAct(LivingEntity entity) { return !Auras.frozen(entity); }

    /** Root and freeze stop voluntary input; an unacknowledged dash endpoint also accepts only passive physics. */
    public static boolean blocksMovement(LivingEntity entity) {
        try (var zone = Profiler.get().zone("covenant:movement_control_check")) {
            return Rooted.contains(entity) || Auras.frozen(entity)
                    || (entity instanceof ServerPlayer player && ownsPhysics(player));
        }
    }

    private static boolean constrained(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger()
                && (Rooted.contains(player) || Auras.frozen(player) || Combos.awaitingMovement(player)
                    || player.getData(RpgData.SKILL_RUNTIME).controlMovement.correctionTeleport != -1);
    }

    /** Native travel and collision remain responsible for gravity, fluids, pushes, and fall damage. */
    public static boolean ownsPhysics(ServerPlayer player) {
        return player.getData(RpgData.SKILL_RUNTIME).controlMovement.ticking || constrained(player);
    }

    private static void beforeTick(PlayerTickEvent.Pre event) {
        try (var zone = Profiler.get().zone("covenant:controls_pre_tick")) {
            if (event.getEntity() instanceof ServerPlayer player) {
                player.getData(RpgData.SKILL_RUNTIME).controlMovement.ticking = constrained(player);
            }
        }
    }

    private static void afterTick(PlayerTickEvent.Post event) {
        try (var zone = Profiler.get().zone("covenant:controls_post_tick")) {
            if (!(event.getEntity() instanceof ServerPlayer player)) { return; }
            var state = player.getData(RpgData.SKILL_RUNTIME).controlMovement;
            if (state.ticking || constrained(player)) {
                // The enclosing native connection tick otherwise restores its pre-travel position.
                player.connection.resetPosition();
                player.level().getChunkSource().move(player);
                player.connection.resetFlyingTicks();
            }
            state.ticking = false;
        }
    }

    /** Reject position reports, retaining finite look changes. Divergence corrections are bounded to four/second. */
    public static boolean interceptMove(ServerPlayer player, ServerboundMovePlayerPacket packet) {
        try (var zone = Profiler.get().zone("covenant:movement_packet")) {
            if (!constrained(player)) { return false; }
            Vec3 reported = new Vec3(packet.getX(player.getX()), packet.getY(player.getY()), packet.getZ(player.getZ()));
            if (!DashMotion.finite(reported) || !Float.isFinite(packet.getYRot(player.getYRot()))
                    || !Float.isFinite(packet.getXRot(player.getXRot()))) {
                return false; // Preserve vanilla's invalid-movement disconnection.
            }
            // Dash start/stop already has its own ordered acknowledgement and exceptional correction.
            if (!Combos.awaitingMovement(player) && reported.distanceToSqr(player.position()) > .0625) {
                var state = player.getData(RpgData.SKILL_RUNTIME).controlMovement;
                long now = ClassResources.now(player);
                if (now >= state.nextCorrection) {
                    state.nextCorrection = now + 5;
                    correctPosition(player, Mth.wrapDegrees(packet.getYRot(player.getYRot())),
                            Mth.clamp(packet.getXRot(player.getXRot()), -90, 90));
                }
            }
            return true;
        }
    }

    /** A native teleport and its native retry retain the current authoritative impulse. */
    public static void correctPosition(ServerPlayer player, float yaw, float pitch) {
        var state = player.getData(RpgData.SKILL_RUNTIME).controlMovement;
        state.sendingCorrection = true;
        try {
            player.connection.teleport(new PositionMoveRotation(player.position(), player.getDeltaMovement(), yaw, pitch), Set.of());
        } finally { state.sendingCorrection = false; }
    }

    public static boolean isControlCorrection(ServerPlayer player, int sequence) {
        return player.getData(RpgData.SKILL_RUNTIME).controlMovement.correctionTeleport == sequence;
    }

    /** Called only by the native connection teleport hooks; unrelated teleports keep ordinary semantics. */
    public static void beforeTeleport(ServerPlayer player) {
        var state = player.getData(RpgData.SKILL_RUNTIME).controlMovement;
        if (!state.sendingCorrection) {
            state.correctionTeleport = -1;
            Combos.teleported(player);
        }
    }

    public static void afterTeleport(ServerPlayer player, int sequence) {
        var state = player.getData(RpgData.SKILL_RUNTIME).controlMovement;
        if (state.sendingCorrection) { state.correctionTeleport = sequence; }
    }

    public static void correctionAcknowledged(ServerPlayer player) {
        player.getData(RpgData.SKILL_RUNTIME).controlMovement.correctionTeleport = -1;
    }

    private static void useItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && canAct(player)) { Barrage.stop(player); }
        if (!canAct(event.getEntity())) { event.setCancellationResult(InteractionResult.FAIL); event.setCanceled(true); }
    }

    private static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!canAct(event.getEntity())) { event.setCancellationResult(InteractionResult.FAIL); event.setCanceled(true); }
    }

    private static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!canAct(event.getEntity())) { event.setCancellationResult(InteractionResult.FAIL); event.setCanceled(true); }
    }

    private static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!canAct(event.getEntity())) { event.setCancellationResult(InteractionResult.FAIL); event.setCanceled(true); }
    }
}
