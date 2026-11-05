package dev.covenant.rpg.skills;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Server boundary for learning, assignment and slot activation. No point economy in this prototype. */
public final class SkillLoadouts {
    private SkillLoadouts() {}

    public static Component learn(ServerPlayer player, Identifier id, boolean learn) {
        ClassResources.requireServer(player);
        var skill = SkillTree.find(id).orElse(null);
        if (!available(player)) { return Skills.message("unavailable"); }
        if (skill == null) { return message("unknown"); }
        // Unlearning remains possible after a testing class switch; learning requires the active class.
        var type = PlayerProfiles.getSelectedClass(player).orElse(null);
        if (learn && type != skill.playerClass()) {
            return Skills.message("class");
        }
        var previous = player.getData(RpgData.SKILL_LOADOUT);
        if (learn) {
            var blocked = SkillTree.blocked(skill, previous);
            if (blocked.isPresent()) { return blocked.orElseThrow(); }
        }
        var next = learn ? previous.withLearned(id, true) : SkillTree.deallocate(previous, id);
        if (!previous.equals(next)) { player.setData(RpgData.SKILL_LOADOUT, next); }
        if (!learn) {
            for (var definition : SkillDefinition.ALL) {
                if (previous.learned().contains(definition.id()) && !next.learned().contains(definition.id())) {
                    player.getData(RpgData.SKILL_RUNTIME).cancelSkill(definition);
                }
            }
        }
        return message(learn ? "learned" : "unlearned", skill.displayName());
    }

    /** Null skill clears the slot; unknown IDs, invalid indices and unlearned skills are rejected. */
    public static Component bind(ServerPlayer player, int slot, Identifier id) {
        ClassResources.requireServer(player);
        if (!available(player)) { return Skills.message("unavailable"); }
        if (!SkillLoadout.validSlot(slot)) { return message("invalid_slot"); }
        var previous = player.getData(RpgData.SKILL_LOADOUT);
        if (id != null) {
            var skill = SkillTree.find(id).orElse(null);
            if (skill == null) { return message("unknown"); }
            if (!previous.learned().contains(id)) { return Skills.message("unlearned"); }
            var type = PlayerProfiles.getSelectedClass(player).orElse(null);
            if (type != skill.playerClass()) {
                return Skills.message("class");
            }
            var blocked = SkillTree.blocked(skill, previous);
            if (blocked.isPresent()) { return blocked.orElseThrow(); }
        }
        var next = previous.withBinding(slot, id);
        if (!previous.equals(next)) { player.setData(RpgData.SKILL_LOADOUT, next); }
        return message(id == null ? "cleared" : "assigned", slot + 1);
    }

    public static Component activateSlot(ServerPlayer player, int slot) {
        ClassResources.requireServer(player);
        if (!SkillLoadout.validSlot(slot)) { return message("invalid_slot"); }
        var id = player.getData(RpgData.SKILL_LOADOUT).bindings().get(slot);
        if (id != null && SkillTree.find(id).filter(node -> !node.implemented()).isPresent()) {
            if (!available(player)) { return Skills.message("unavailable"); }
            var node = SkillTree.find(id).orElseThrow();
            if (PlayerProfiles.getSelectedClass(player).orElse(null) != node.playerClass()) { return Skills.message("class"); }
            return message("planned");
        }
        var skill = id == null ? null : SkillDefinition.find(id).orElse(null);
        return skill == null ? message("empty") : Skills.activate(player, skill);
    }

    private static boolean available(ServerPlayer player) { return player.isAlive() && !player.isSpectator(); }
    private static Component message(String key, Object... args) {
        return Component.translatable("skills.covenant." + key, args);
    }
}
