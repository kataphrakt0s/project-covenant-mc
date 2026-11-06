package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import net.minecraft.server.level.ServerPlayer;

/** Entity-local, server-only ephemeral state. Casts/combat never copy across replacement or logout. */
public final class SkillRuntime {
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
    final java.util.EnumMap<TrapKind, TrapEntity> traps = new java.util.EnumMap<>(TrapKind.class);
    final java.util.EnumMap<TrapKind, TrapEntity> patches = new java.util.EnumMap<>(TrapKind.class);
    long combatUntil;
    long lastTick = Long.MIN_VALUE;
    long nextActivation;
    net.minecraft.world.damagesource.DamageSource skillDamageSource;
    PendingCast cast;
    final SkillControls.MovementState controlMovement = new SkillControls.MovementState();
    final SkillRequestBudget requests = new SkillRequestBudget();

    public void cancelCast(ServerPlayer player) {
        var previous = cast;
        cast = null;
        if (previous != null) { previous.finish(player); }
    }

    public void cancelSkill(ServerPlayer player, SkillDefinition skill) {
        if (cast != null && cast.skill() == skill) { cancelCast(player); }
        Combos.cancel(player, skill);
        Traps.clear(player, skill.id());
    }
}
