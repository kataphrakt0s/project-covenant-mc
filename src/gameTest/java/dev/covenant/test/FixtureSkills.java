package dev.covenant.test;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.skills.SkillLifecycle;
import dev.covenant.rpg.skills.SkillTree;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Trusted setup for combat regression fixtures; allocation tests exercise the production purchase boundary. */
final class FixtureSkills {
    static void learn(ServerPlayer player, Identifier id, boolean learn) {
        var old = player.getData(RpgData.SKILL_LOADOUT);
        var next = learn ? old.withLearned(id, true) : SkillTree.deallocate(old, id);
        player.setData(RpgData.SKILL_LOADOUT, next);
        SkillLifecycle.loadoutChanged(player, old, next);
    }
}
