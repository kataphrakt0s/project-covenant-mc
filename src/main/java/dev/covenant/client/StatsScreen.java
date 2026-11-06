package dev.covenant.client;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.stats.RpgStats;
import java.util.Comparator;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Read-only native attribute inspection plus the existing one-time server class selection. */
final class StatsScreen extends Screen {
    private FittingMultiLineTextWidget statList;
    private Component previousStats;
    private boolean choosingClass;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    StatsScreen() { super(text("title")); }

    @Override protected void init() {
        panelWidth = Math.min(480, width - 16);
        panelHeight = Math.min(360, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        choosingClass = !PlayerProfiles.get(minecraft.player).created();
        int listTop = top + 46;
        if (choosingClass) {
            int buttonWidth = (panelWidth - 24) / PlayerClass.all().size();
            for (int i = 0; i < PlayerClass.all().size(); i++) {
                var type = PlayerClass.all().get(i);
                addRenderableWidget(Button.builder(type.displayName(), button -> {
                    // Native command networking retains the shared server validation and owner sync.
                    minecraft.player.connection.sendCommand("covenant class select " + type.id());
                }).bounds(left + 12 + i * buttonWidth, top + 61, buttonWidth - 4, 20)
                        .tooltip(Tooltip.create(type.description().copy().append("\n").append(text("choose_once"))))
                        .build());
            }
            listTop = top + 89;
        }
        statList = addRenderableWidget(new FittingMultiLineTextWidget(left + 12, listTop,
                panelWidth - 30, top + panelHeight - 34 - listTop, Component.empty(), font));
        previousStats = null;
        refreshStats();
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 40, top + panelHeight - 27, 80, 20).build());
    }

    private void refreshStats() {
        MutableComponent lines = Component.empty();
        var profile = PlayerProfiles.get(minecraft.player);
        var progression = minecraft.player.getData(dev.covenant.rpg.RpgData.PROGRESSION_VIEW);
        if (profile.created()) {
            long required = progression.requiredXp();
            String progress = required > 0 ? profile.experience() + " / " + required + " XP ("
                    + String.format(Locale.ROOT, "%.1f", 100.0 * profile.experience() / required) + "%)" : "Maximum level";
            lines.append("Level " + profile.level() + " · " + progress + "\n"
                    + dev.covenant.rpg.skills.SkillLoadouts.availablePoints(minecraft.player) + " unspent skill points"
                    + "\nDimension cap: " + progression.dimensionCap() + " · Global cap: " + progression.globalCap() + "\n\n");
        }
        minecraft.player.getData(dev.covenant.rpg.RpgData.PROFESSIONS).tracks().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey()).forEach(entry -> lines.append(entry.getKey().getPath()
                        + ": level " + entry.getValue().level() + " · " + entry.getValue().experience() + " XP\n"));
        lines.append("\n");
        for (var stat : RpgStats.SUPPORTED) {
            var instance = minecraft.player.getAttribute(stat);
            if (instance == null) { continue; }
            lines.append(Component.translatable("screen.covenant.stats.value",
                    Component.translatable(stat.value().getDescriptionId()), value(stat, instance.getValue()))
                    .withStyle(ChatFormatting.GOLD));
            lines.append("\n").append(text("base", value(stat, instance.getBaseValue()))
                    .withStyle(ChatFormatting.GRAY));
            var modifiers = instance.getModifiers().stream().sorted(Comparator
                    .comparing(AttributeModifier::operation).thenComparing(modifier -> modifier.id().toString())).toList();
            if (modifiers.isEmpty()) {
                lines.append("\n").append(text("no_modifiers").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                for (var modifier : modifiers) {
                    String amount = number(modifier.amount()
                            * (modifier.operation() == AttributeModifier.Operation.ADD_VALUE ? 1 : 100));
                    if (modifier.amount() >= 0) { amount = "+" + amount; }
                    lines.append("\n").append(text("modifier", modifier.id().toString(),
                            text("operation." + modifier.operation().getSerializedName(), amount))
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            lines.append("\n\n");
        }
        if (!lines.equals(previousStats)) {
            previousStats = lines;
            statList.setMessage(lines);
            statList.refreshScrollAmount();
        }
    }

    @Override public void tick() {
        if (minecraft.player == null || !minecraft.player.isAlive() || minecraft.level == null
                || !minecraft.isWindowActive()) { onClose(); return; }
        if (choosingClass == PlayerProfiles.get(minecraft.player).created()) {
            rebuildWidgets();
        } else {
            refreshStats();
        }
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x95080D14);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF018202B);
        graphics.outline(left, top, panelWidth, panelHeight, 0xFF8E7851);
        graphics.centeredText(font, title, width / 2, top + 8, 0xFFF5DBA6);
        var profile = PlayerProfiles.get(minecraft.player);
        Component className = profile.selectedClass().map(id -> PlayerClass.find(id)
                .map(PlayerClass::displayName).orElseGet(() -> Component.literal(id.toString())))
                .orElse(text("no_class"));
        graphics.centeredText(font, text("class", className), width / 2, top + 25, 0xFFE2E8ED);
        if (choosingClass) {
            graphics.centeredText(font, text("choose_once"), width / 2, top + 45, 0xFFB6C3D1);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static String value(Holder<Attribute> stat, double value) {
        if (stat == RpgStats.DODGE) { return number(value * 100) + "%"; }
        if (RpgStats.SUPPORTED.indexOf(stat) >= 6) { return number(value) + "x"; }
        return number(value);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.4f", value).replaceAll("\\.?0+$", "");
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private static MutableComponent text(String key, Object... args) {
        return Component.translatable("screen.covenant.stats." + key, args);
    }
}
