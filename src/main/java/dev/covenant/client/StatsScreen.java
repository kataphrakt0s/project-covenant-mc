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
import net.minecraft.util.profiling.Profiler;
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
    private PlayerClass preview = PlayerClass.STALKER;
    private boolean details;
    private Button choose;
    private Button breakdown;

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
                var button = new MenuArt.IconButton(left + 12 + i * buttonWidth, top + 61, buttonWidth - 4, 20,
                        type == preview ? Component.literal("> ").append(type.displayName()) : type.displayName(),
                        SkillIcons.ui(type.id().getPath()), b -> { preview = type; rebuildWidgets(); });
                button.setTooltip(Tooltip.create(type.description()));
                addRenderableWidget(button);
            }
            listTop = top + 89;
        }
        statList = addRenderableWidget(new FittingMultiLineTextWidget(left + 12, listTop,
                panelWidth - 30, top + panelHeight - 56 - listTop, Component.empty(), font));
        previousStats = null;
        refreshStats();
        if (choosingClass) {
            choose = addRenderableWidget(Button.builder(Component.translatable("ui.covenant.choose_class", preview.displayName()), b -> {
                // The chosen preview is committed only by this explicit button, through native validation.
                minecraft.player.connection.sendCommand("covenant class select " + preview.id());
            }).bounds(left + 8, top + panelHeight - 27, 110, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("screen.covenant.skills.title"), b -> minecraft.setScreen(new SkillsScreen()))
                    .bounds(left + 8, top + panelHeight - 27, 77, 20).build());
        }
        breakdown = addRenderableWidget(Button.builder(Component.translatable(details ? "ui.covenant.stats_summary" : "ui.covenant.stats_details"), b -> { details = !details; refreshStats(); })
                .bounds(left + panelWidth - 172, top + panelHeight - 27, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + panelWidth - 85, top + panelHeight - 27, 77, 20).build());
    }

    private void refreshStats() {
        try (var zone = Profiler.get().zone("covenant:stats_refresh")) {
            if (choose != null && choosingClass) { choose.setMessage(Component.translatable("ui.covenant.choose_class", preview.displayName())); }
            if (breakdown != null) { breakdown.setMessage(Component.translatable(details ? "ui.covenant.stats_summary" : "ui.covenant.stats_details")); }
            MutableComponent lines = Component.empty();
            var profile = PlayerProfiles.get(minecraft.player);
            var progression = minecraft.player.getData(dev.covenant.rpg.RpgData.PROGRESSION_VIEW);
            if (choosingClass) {
                lines.append(preview.displayName().copy().withStyle(ChatFormatting.GOLD)).append("\n")
                        .append(preview.description()).append("\n\n")
                        .append(Component.translatable("ui.covenant.class_commit").withStyle(ChatFormatting.GRAY)).append("\n\n");
            }
            if (profile.created()) {
                long required = progression.requiredXp();
                String progress = required > 0 ? profile.experience() + " / " + required + " XP ("
                        + String.format(Locale.ROOT, "%.1f", 100.0 * profile.experience() / required) + "%)" : "Maximum level";
                lines.append("Level " + profile.level() + " · " + progress + "\n"
                        + dev.covenant.rpg.skills.SkillLoadouts.availablePoints(minecraft.player) + " unspent skill points"
                        + "\nDimension cap: " + progression.dimensionCap() + " · Global cap: " + progression.globalCap() + "\n\n");
            }
            lines.append(Component.translatable("ui.covenant.group.professions").withStyle(ChatFormatting.AQUA)).append("\n");
            minecraft.player.getData(dev.covenant.rpg.RpgData.PROFESSIONS).tracks().entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey()).forEach(entry -> lines.append(entry.getKey().getPath()
                            + ": level " + entry.getValue().level() + " · " + entry.getValue().experience() + " XP\n"));
            lines.append("\n");
            for (var stat : RpgStats.SUPPORTED) {
                if (stat == RpgStats.HEALTH || stat == RpgStats.RESOURCE_COST || stat == RpgStats.FOCUS_REGENERATION) {
                    String group = stat == RpgStats.HEALTH ? "combat" : stat == RpgStats.RESOURCE_COST ? "skills" : "resources";
                    lines.append(Component.translatable("ui.covenant.group." + group).withStyle(ChatFormatting.AQUA)).append("\n");
                }
                var instance = minecraft.player.getAttribute(stat);
                if (instance == null) { continue; }
                lines.append(Component.translatable("screen.covenant.stats.value",
                        Component.translatable(stat.value().getDescriptionId()), value(stat, instance.getValue()))
                        .withStyle(ChatFormatting.GOLD));
                if (!details) { lines.append("\n"); continue; }
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
                        lines.append("\n").append(text("modifier", source(modifier),
                                text("operation." + modifier.operation().getSerializedName(), amount))
                                .withStyle(ChatFormatting.GRAY)).append("\n  ").append(Component.literal(modifier.id().toString()).withStyle(ChatFormatting.DARK_GRAY));
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
    }

    @Override public void tick() {
        try (var zone = Profiler.get().zone("covenant:stats_screen_tick")) {
            if (minecraft.player == null || !minecraft.player.isAlive() || minecraft.level == null
                    || !minecraft.isWindowActive()) { onClose(); return; }
            if (choosingClass == PlayerProfiles.get(minecraft.player).created()) {
                rebuildWidgets();
            } else {
                refreshStats();
            }
        }
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x95080D14);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        try (var zone = Profiler.get().zone("covenant:stats_screen")) {
            MenuArt.panel(graphics, left, top, panelWidth, panelHeight);
            graphics.centeredText(font, title, width / 2, top + 8, 0xFFF5DBA6);
            var profile = PlayerProfiles.get(minecraft.player);
            Component className = profile.selectedClass().map(id -> PlayerClass.find(id)
                    .map(PlayerClass::displayName).orElseGet(() -> Component.literal(id.toString())))
                    .orElse(text("no_class"));
            graphics.centeredText(font, text("class", className), width / 2, top + 25, 0xFFE2E8ED);
            if (choosingClass) {
                graphics.centeredText(font, text("choose_once"), width / 2, top + 45, 0xFFB6C3D1);
            }
            MenuArt.label(graphics, Component.translatable("ui.covenant.discovery"), left + 12, top + panelHeight - 45,
                    panelWidth - 24, MenuArt.MUTED);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** Resolve only known sources; unfamiliar native/mod IDs remain visible verbatim. */
    private Component source(AttributeModifier modifier) {
        var id = modifier.id();
        if (!id.getNamespace().equals("covenant")) { return Component.literal(id.toString()); }
        String path = id.getPath();
        if (path.startsWith("page/")) {
            for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                var stack = minecraft.player.getItemBySlot(slot);
                var page = stack.get(dev.covenant.rpg.RpgData.AFFIX_PAGE.get());
                if (page != null && path.startsWith("page/" + page.identity() + "/")) {
                    return Component.translatable("ui.covenant.source.equipment", stack.getHoverName(), slot.getName());
                }
            }
        }
        String key = switch (path) {
            case "aura_armor", "aura_toughness" -> "fortitude_aura";
            case "aura_swiftness", "aura_step" -> "swiftness_aura";
            case "aura_dodge" -> "dodge_aura";
            case "aura_mana" -> "enchanting_aura";
            case "aura_cooldown" -> "icy_veins";
            case "elemental_golem" -> "elemental_golem";
            case "blood_fever_movement", "blood_fever_attack" -> "blood_fever";
            default -> null;
        };
        return key == null ? Component.literal(id.toString()) : Component.translatable("skill.covenant." + key);
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
