package dev.covenant.rpg.skills;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Event-only aura feedback. Neither passive refresh nor source cleanup calls this helper. */
final class AuraVisuals {
    private AuraVisuals() {}

    static void emit(LivingEntity target, Auras.Kind kind, SimpleParticleType type) {
        // Battle Cry and Discharge have their own presentation. Ordinals are transient,
        // same-version particle parameters, never persisted IDs or client gameplay state.
        if (kind == Auras.Kind.BATTLE_CRY || kind == Auras.Kind.DISCHARGE_FREEZE
                || !(target.level() instanceof ServerLevel level)) { return; }
        level.sendParticles(type, target.getX(), target.getY(), target.getZ(),
                0, kind.ordinal(), target.getBbWidth(), target.getBbHeight(), 1);
    }
}
