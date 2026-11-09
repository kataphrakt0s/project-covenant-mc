package dev.covenant.client;

import dev.covenant.Covenant;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.skills.ClassResources;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Reads owner-synced resources without predicting spending or recovery. */
public final class ResourceHud {
    private static final int WIDTH = 81;
    private static final int HEIGHT = 11;
    private static long displayedCurrent = Long.MIN_VALUE;
    private static long displayedMaximum = Long.MIN_VALUE;
    private static String label = "";
    private static float displayedShield = -1;
    private static net.minecraft.network.chat.Component shieldLabel = net.minecraft.network.chat.Component.empty();
    private static dev.covenant.rpg.skills.WeaverDisplay weaverView;
    private static net.minecraft.network.chat.Component dischargeLabel, threefoldLabel;

    private ResourceHud() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
                Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "class_resource"), (graphics, deltaTracker) -> {
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;
            if (minecraft.options.hideGui || player == null || minecraft.level == null) { return; }
            var type = PlayerProfiles.getSelectedClass(player).orElse(null);
            if (type == null) { return; }

            // Reserve a row after hunger, before mount health/air. Keep the same anchor in Creative.
            int height = Math.max(49, minecraft.gui.rightHeight);
            int x = graphics.guiWidth() / 2 + 91 - WIDTH;
            int y = graphics.guiHeight() - height - 3;
            minecraft.gui.rightHeight = height + HEIGHT + 3;
            double maximum = ClassResources.maximum(player, type);
            double current = ClassResources.current(player, type);
            int fill = maximum > 0 ? (int) Math.round((WIDTH - 2) * Math.clamp(current / maximum, 0, 1)) : 0;
            int color = switch (type) {
                case REAVER -> 0xFFD83B36;
                case WEAVER -> 0xFF367FE5;
                case STALKER -> 0xFFF1CE42;
            };
            graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF16181E);
            graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, 0xFF363941);
            if (fill > 0) { graphics.fill(x + 1, y + 1, x + 1 + fill, y + HEIGHT - 1, color); }

            // Whole units keep the small bar legible; scale only for unusually large configured capacities.
            long units = (long) Math.floor(current);
            long capacity = (long) Math.floor(maximum);
            if (units != displayedCurrent || capacity != displayedMaximum) {
                displayedCurrent = units;
                displayedMaximum = capacity;
                label = units + " / " + capacity;
            }
            float scale = Math.min(1, (WIDTH - 4f) / Math.max(1, minecraft.font.width(label)));
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + WIDTH / 2f, y + (HEIGHT - minecraft.font.lineHeight * scale) / 2f);
            graphics.pose().scale(scale, scale);
            graphics.centeredText(minecraft.font, label, 0, 0, 0xFFFFFFFF);
            graphics.pose().popMatrix();
            float shield = player.getData(dev.covenant.rpg.RpgData.FORTITUDE_CAPACITY);
            if (shield > 0) {
                if (shield != displayedShield) {
                    displayedShield = shield;
                    shieldLabel = net.minecraft.network.chat.Component.translatable("hud.covenant.fortitude",
                            String.format(java.util.Locale.ROOT, "%.1f", shield));
                }
                int shieldY = y - HEIGHT - 3;
                minecraft.gui.rightHeight += HEIGHT + 3;
                graphics.fill(x, shieldY, x + WIDTH, shieldY + HEIGHT, 0xFF16181E);
                graphics.fill(x + 1, shieldY + 1, x + 1 + (int) ((WIDTH - 2) * shield / 6), shieldY + HEIGHT - 1, 0xFF7E9DAB);
                graphics.centeredText(minecraft.font, shieldLabel, x + WIDTH / 2, shieldY + 1, 0xFFFFFFFF);
            }
            var combos = player.getData(dev.covenant.rpg.RpgData.COMBO_DISPLAY);
            int comboY = y - (shield > 0 ? 2 : 1) * (HEIGHT + 3);
            var weaver = player.getData(dev.covenant.rpg.RpgData.WEAVER_DISPLAY);
            if (!weaver.equals(weaverView)) {
                weaverView = weaver;
                dischargeLabel = net.minecraft.network.chat.Component.translatable("hud.covenant.discharge", weaver.fire(), weaver.ice(), weaver.lightning(), weaver.maxCharges());
                threefoldLabel = net.minecraft.network.chat.Component.translatable(weaver.buff() ? "hud.covenant.threefold.buff" : "hud.covenant.threefold.reserve",
                        weaver.firePower(), weaver.icePower(), weaver.lightningPower());
            }
            if (weaver.discharge()) {
                graphics.text(minecraft.font, dischargeLabel, Math.max(3, x + WIDTH - minecraft.font.width(dischargeLabel)), comboY + 1, 0xFFFFFFFF);
                minecraft.gui.rightHeight += HEIGHT + 3; comboY -= HEIGHT + 3;
            }
            if (weaver.threefold()) {
                graphics.text(minecraft.font, threefoldLabel, Math.max(3, x + WIDTH - minecraft.font.width(threefoldLabel)), comboY + 1, 0xFFFFFFFF);
                minecraft.gui.rightHeight += HEIGHT + 3; comboY -= HEIGHT + 3;
            }
            for (var row : combos.rows()) {
                long remaining = row.deadline() - (minecraft.level.getGameTime() + combos.clockOffset());
                if (!row.moving() && remaining <= 0) { continue; }
                String skill = row.combo() == 0 ? "lunging_combo" : "rip_and_rush";
                var action = net.minecraft.network.chat.Component.translatable("combo.covenant." + skill + "." + row.stage());
                var text = net.minecraft.network.chat.Component.translatable(row.moving() ? "hud.covenant.combo.moving" : "hud.covenant.combo.waiting",
                        net.minecraft.network.chat.Component.translatable("skill.covenant." + skill), action,
                        String.format(java.util.Locale.ROOT, "%.1f", Math.max(0, remaining) / 20.0));
                int width = minecraft.font.width(text);
                graphics.text(minecraft.font, text, Math.max(3, x + WIDTH - width), comboY + 1, 0xFFFFFFFF);
                minecraft.gui.rightHeight += HEIGHT + 3;
                comboY -= HEIGHT + 3;
            }
        });
    }
}
