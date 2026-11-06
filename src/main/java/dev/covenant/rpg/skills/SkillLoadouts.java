package dev.covenant.rpg.skills;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Server boundary for point purchases, assignment and slot activation. */
public final class SkillLoadouts {
    private SkillLoadouts() {}

    public static Component learn(ServerPlayer player, Identifier id, boolean learn) {
        ClassResources.requireServer(player);
        if (!learn) { return Component.literal("Use a respec item to refund committed upgrades."); }
        var previous = player.getData(RpgData.SKILL_LOADOUT);
        var desired = new java.util.HashMap<>(previous.ranks());
        desired.merge(id, 1, Integer::sum);
        return apply(player, previous.ranks(), desired);
    }

    public static int availablePoints(net.minecraft.world.entity.player.Player player) {
        return Math.max(0, PlayerProfiles.get(player).level() - player.getData(RpgData.SKILL_LOADOUT).spent());
    }

    /** Full expected snapshot makes a replay or stale screen harmless. No mutations before validation. */
    public static Component apply(ServerPlayer player, java.util.Map<Identifier, Integer> expected,
            java.util.Map<Identifier, Integer> desired) {
        ClassResources.requireServer(player);
        if (!available(player)) { return Skills.message("unavailable"); }
        if (ClassResources.inCombat(player)) { return Component.literal("Leave combat before applying upgrades."); }
        var previous = player.getData(RpgData.SKILL_LOADOUT);
        if (!previous.ranks().equals(expected)) { return Component.literal("Your skills changed. Reopen the skill tree."); }
        if (desired.equals(expected)) { return Component.literal("No pending upgrades."); }
        if (desired.size() > 8 || !desired.keySet().containsAll(expected.keySet())) { return Component.literal("Invalid skill allocation (maximum 8 skills)."); }
        var type = PlayerProfiles.getSelectedClass(player).orElse(null);
        if (type == null) { return Skills.message("class"); }
        int spent = 0;
        for (var entry : desired.entrySet()) {
            var node = SkillTree.find(entry.getKey()).orElse(null);
            int rank = entry.getValue();
            if (node == null || rank < previous.rank(entry.getKey()) || rank < 1 || rank > node.availableMaximum()) {
                return Component.literal("That skill rank is unavailable.");
            }
            if (rank > previous.rank(entry.getKey()) && (type != node.playerClass()
                    || (previous.rank(entry.getKey()) == 0 && PlayerProfiles.get(player).level() < node.diagramLevel()))) { return Component.literal("Class or level requirement not met."); }
            spent += rank;
        }
        if (spent > PlayerProfiles.get(player).level()) { return Component.literal("Not enough skill points."); }
        var next = new SkillLoadout(desired, previous.bindings());
        for (var id : desired.keySet()) {
            var blocked = SkillTree.blocked(SkillTree.find(id).orElseThrow(), next);
            if (blocked.isPresent()) { return blocked.orElseThrow(); }
        }
        player.setData(RpgData.SKILL_LOADOUT, next);
        SkillLifecycle.loadoutChanged(player, previous, next);
        return Component.literal("Upgrades applied.");
    }

    /** Consuming the item is the caller's responsibility after this successful atomic reset. */
    public static boolean respec(ServerPlayer player) {
        ClassResources.requireServer(player);
        if (!available(player) || ClassResources.inCombat(player) || player.getData(RpgData.SKILL_LOADOUT).ranks().isEmpty()) { return false; }
        var previous = player.getData(RpgData.SKILL_LOADOUT);
        player.setData(RpgData.SKILL_LOADOUT, SkillLoadout.EMPTY);
        SkillLifecycle.loadoutChanged(player, previous, SkillLoadout.EMPTY);
        return true;
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
        if (previous.equals(next)) { return message(id == null ? "cleared" : "assigned", slot + 1); }
        player.setData(RpgData.SKILL_LOADOUT, next);
        SkillLifecycle.loadoutChanged(player, previous, next);
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
