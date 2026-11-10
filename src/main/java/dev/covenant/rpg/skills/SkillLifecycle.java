package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Shared cancellation boundary; persistent target wounds outlive their caster's transient activity. */
public final class SkillLifecycle {
    public enum Reason { CLASS_CHANGE, LOGOUT, DIMENSION_CHANGE, CONFIRMED_DEATH }

    private SkillLifecycle() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SkillLifecycle::loggedOut);
        NeoForge.EVENT_BUS.addListener(SkillLifecycle::changedDimension);
    }

    /** Ends owned activity, committing deferred combo cooldowns before player replacement or saving. */
    public static void cancelPlayer(ServerPlayer player, Reason reason) {
        ClassResources.requireServer(player);
        Combos.clear(player);
        player.getData(RpgData.SKILL_RUNTIME).cancelCast(player);
        Traps.clear(player, null);
        RageStorm.clear(player);
        BloodFever.clear(player);
        WeaverPower.clear(player);
        Auras.clear(player);
        BuffDisplay.clear(player);
        if (reason == Reason.CONFIRMED_DEATH) { clearTargetOnDeath(player); }
    }

    /** Called only after the native death hook has finished dispatching and accepted the death. */
    public static void confirmedDeath(LivingEntity target) {
        if (target.level().isClientSide()) { return; }
        if (target instanceof ServerPlayer player) {
            cancelPlayer(player, Reason.CONFIRMED_DEATH);
        } else {
            Auras.clear(target);
            clearTargetOnDeath(target);
        }
    }

    private static void clearTargetOnDeath(LivingEntity target) {
        target.removeData(RpgData.BLEED);
        target.removeEffect(Rooted.EFFECT);
        target.removeEffect(ArmorShred.EFFECT);
        target.removeEffect(Vulnerability.EFFECT);
        target.removeData(RpgData.MULTISHOT_HITS);
    }

    /** Runs after an actual validated edit; unchanged requests never reconcile or cancel activity. */
    public static void loadoutChanged(ServerPlayer player, SkillLoadout previous, SkillLoadout next) {
        ClassResources.requireServer(player);
        for (var definition : SkillDefinition.ALL) {
            if (previous.learned().contains(definition.id()) && !next.learned().contains(definition.id())) {
                player.getData(RpgData.SKILL_RUNTIME).cancelSkill(player, definition);
            }
        }
        for (var binding : previous.bindings().entrySet()) {
            if (!binding.getValue().equals(next.bindings().get(binding.getKey()))) {
                Combos.bindingChanged(player, binding.getValue());
            }
        }
        Auras.reconcile(player);
        BloodFever.reconcile(player);
        WeaverPower.reconcile(player);
    }

    private static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { cancelPlayer(player, Reason.LOGOUT); }
    }

    private static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { cancelPlayer(player, Reason.DIMENSION_CHANGE); }
    }
}
