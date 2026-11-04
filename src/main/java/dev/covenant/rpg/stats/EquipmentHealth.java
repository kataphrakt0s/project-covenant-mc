package dev.covenant.rpg.stats;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Repairs vanilla's health clamp ordering at player load/clone without storing another health value. */
public final class EquipmentHealth {
    private EquipmentHealth() {}

    /** Called once after inventory load, using the existing vanilla Health field. */
    public static void restoreAfterLoad(Player player, float savedHealth) {
        if (player.level().isClientSide()) { return; }
        applyMaximum(player);
        player.setHealth(savedHealth);
    }

    static void onClone(PlayerEvent.Clone event) {
        var player = event.getEntity();
        if (player.level().isClientSide()) { return; }
        applyMaximum(player);
        // Vanilla respawns at full health; End return preserves the previous current health.
        player.setHealth(event.isWasDeath() ? player.getMaxHealth() : event.getOriginal().getHealth());
    }

    private static void applyMaximum(Player player) {
        var maximum = player.getAttribute(RpgStats.HEALTH);
        for (var slot : EquipmentSlot.VALUES) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || stack.isBroken()) { continue; }
            stack.forEachModifier(slot, (attribute, modifier) -> {
                if (attribute.equals(RpgStats.HEALTH)) {
                    // The normal equipment update replaces these same IDs on its first tick.
                    maximum.removeModifier(modifier.id());
                    maximum.addTransientModifier(modifier);
                }
            });
        }
    }
}
