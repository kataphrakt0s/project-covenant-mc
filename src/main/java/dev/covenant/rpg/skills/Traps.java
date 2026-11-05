package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.stats.RpgStats;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Bounded owner-local deployment slots. No global entity index or chunk tickets. */
public final class Traps {
    private Traps() {}

    /** Spawn before spending; a rejected entity-add leaves the old deployment and balances intact. */
    static TrapEntity prepare(ServerPlayer owner, SkillDefinition skill) {
        var kind = TrapKind.forSkill(skill);
        int duration = (int) Math.clamp(Math.ceil(RpgStats.scaleSkillValue(owner,
                RpgStats.SKILL_DURATION, kind.duration)), 0, Integer.MAX_VALUE);
        var trap = new TrapEntity(Skills.TRAP.get(), owner.level());
        trap.initialize(owner, kind, duration);
        double angle = Math.toRadians(owner.getYRot());
        var forward = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
        // Keep the release box inside the player's horizontal clearance when pressed against a wall.
        trap.setPos(owner.position().add(forward.scale(0.1)).add(0, 0.65, 0));
        trap.setDeltaMovement(forward.scale(0.3625).add(0, 0.14, 0));
        return owner.level().addFreshEntity(trap) ? trap : null;
    }

    static void accept(ServerPlayer owner, TrapEntity trap) {
        var old = owner.getData(RpgData.SKILL_RUNTIME).traps.put(trap.kind(), trap);
        if (old != null && old != trap) { old.discard(); }
    }

    static void triggered(ServerPlayer owner, TrapEntity trap) {
        var runtime = owner.getData(RpgData.SKILL_RUNTIME);
        runtime.traps.remove(trap.kind(), trap);
        if (trap.kind() == TrapKind.FROST || trap.kind() == TrapKind.FLAME) {
            var old = runtime.patches.put(trap.kind(), trap);
            if (old != null && old != trap) { old.discard(); }
        }
    }

    /** Also called for every dependent actually deallocated; binding changes do not call this. */
    public static void clear(ServerPlayer owner, Identifier skillId) {
        var runtime = owner.getData(RpgData.SKILL_RUNTIME);
        for (var trap : runtime.traps.values().toArray(TrapEntity[]::new)) {
            if (skillId == null || trap.kind().skillId.equals(skillId)) { trap.discard(); }
        }
        for (var patch : runtime.patches.values().toArray(TrapEntity[]::new)) {
            if (skillId == null || patch.kind().skillId.equals(skillId)) { patch.discard(); }
        }
    }
}
