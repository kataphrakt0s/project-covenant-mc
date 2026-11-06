package dev.covenant.rpg.skills;

import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.RpgData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

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
    boolean deploying;
    net.minecraft.world.damagesource.DamageSource skillDamageSource;
    SkillDefinition casting;
    PlayerClass castClass;
    long castEnds;
    double castCost;
    long castCooldown;

    public void cancelCast(ServerPlayer player) {
        if (casting == SkillDefinition.AIMED_SHOT) {
            player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(Skills.AIMED_SHOT_SLOW);
            player.setData(RpgData.BOW_DRAW_UNTIL, 0L);
        }
        casting = null;
        castClass = null;
    }

    public void cancelSkill(ServerPlayer player, SkillDefinition skill) {
        if (casting == skill) { cancelCast(player); }
        Combos.cancel(player, skill);
        Traps.clear(player, skill.id());
    }
}
