package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import net.minecraft.server.level.ServerPlayer;

/** Entity-local, server-only ephemeral state. Casts/combat never copy across replacement or logout. */
public final class SkillRuntime {
    /** Shared player-intent ingress budget; gameplay validation remains the caller's responsibility. */
    public boolean acceptRequest(long tick, net.minecraft.network.protocol.common.custom.CustomPacketPayload request) {
        return requests.accept(tick, request);
    }
    /** Operator testing toggle; never saved, synced, or copied to a replacement player. */
    boolean infiniteResource;
    /** Operator cooldown-check bypass; entity-local and never saved, synced, or death-copied. */
    boolean ignoreCooldowns;
    boolean applyingRend;
    boolean executing;
    final java.util.EnumMap<Combos.Kind, Combos.Progress> combos = new java.util.EnumMap<>(Combos.Kind.class);
    Combos.Movement movement;
    long motionSequence;
    long awaitingDashSequence;
    long dashAcknowledgementUntil;
    long rushSpeedUntil;
    int bloodFeverCount;
    long nextBloodFever;
    final java.util.Set<RageStorm> storms = new java.util.HashSet<>();
    final java.util.EnumMap<TrapKind, TrapEntity> traps = new java.util.EnumMap<>(TrapKind.class);
    final java.util.EnumMap<TrapKind, TrapEntity> patches = new java.util.EnumMap<>(TrapKind.class);
    long combatUntil;
    long lastTick = Long.MIN_VALUE;
    long nextActivation;
    net.minecraft.world.damagesource.DamageSource skillDamageSource;
    PendingCast cast;
    Barrage.Channel channel;
    double nextBarrageShot = Double.NEGATIVE_INFINITY;
    long channelInputSequence;
    GroundSkillEntity firestorm, banner;
    WeaverPower.State weaver;
    final SkillControls.MovementState controlMovement = new SkillControls.MovementState();
    final SkillRequestBudget requests = new SkillRequestBudget();

    public void cancelCast(ServerPlayer player) {
        var previous = cast;
        cast = null;
        if (previous != null) { previous.finish(player); }
    }

    public void cancelSkill(ServerPlayer player, SkillDefinition skill) {
        if (skill == SkillDefinition.BARRAGE) { Barrage.stop(player); }
        if (skill == SkillDefinition.FIRESTORM && firestorm != null) { firestorm.discard(); }
        if (skill == SkillDefinition.WAR_BANNER && banner != null) { banner.discard(); }
        if (cast != null && cast.skill().id().equals(skill.id())) { cancelCast(player); }
        if (skill == SkillDefinition.RAGE_STORM) { RageStorm.clear(player); }
        if (skill == SkillDefinition.BATTLE_CRY) { Auras.removeSource(player, Auras.Kind.BATTLE_CRY); }
        if (skill == SkillDefinition.DISCHARGE) { Auras.removeSource(player, Auras.Kind.DISCHARGE_FREEZE); }
        Combos.cancel(player, skill);
        Traps.clear(player, skill.id());
    }
}
