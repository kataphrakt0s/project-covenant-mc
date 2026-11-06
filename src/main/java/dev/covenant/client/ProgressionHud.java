package dev.covenant.client;

import dev.covenant.Covenant;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** One-pixel bar in the gap below vanilla XP and above the selected hotbar border. */
public final class ProgressionHud {
    private ProgressionHud() {}
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CONTEXTUAL_INFO_BAR, Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "character_xp"), (graphics, delta) -> {
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;
            if (minecraft.options.hideGui || player == null || player.isSpectator() || !PlayerProfiles.get(player).created()) { return; }
            var profile = PlayerProfiles.get(player);
            var view = player.getData(RpgData.PROGRESSION_VIEW);
            double fraction = profile.level() >= view.globalCap() ? 1 : view.requiredXp() > 0
                    ? Math.clamp((double) profile.experience() / view.requiredXp(), 0, 1) : 0;
            int x = graphics.guiWidth() / 2 - 91, y = graphics.guiHeight() - 24;
            graphics.fill(x, y, x + 182, y + 1, 0xFF292536);
            int filled = (int) (fraction * 182);
            if (filled > 0) { graphics.fill(x, y, x + filled, y + 1, 0xFFBA93ED); }
        });
    }
}
