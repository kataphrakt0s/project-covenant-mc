package dev.covenant.rpg.skills;

import dev.covenant.rpg.classes.PlayerClass;

/** Entity-local, server-only ephemeral state. Casts/combat never copy across replacement or logout. */
public final class SkillRuntime {
    long combatUntil;
    long lastTick = Long.MIN_VALUE;
    long nextActivation;
    SkillDefinition casting;
    PlayerClass castClass;
    long castEnds;
    double castCost;
    long castCooldown;

    public void cancelCast() {
        casting = null;
        castClass = null;
    }

    public void cancelSkill(SkillDefinition skill) {
        if (casting == skill) { cancelCast(); }
    }
}
