package dev.covenant.client;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.progression.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Classless owner view. Each tab keeps its own draft; only Apply submits server intent. */
final class ProfessionsScreen extends Screen {
    /** Keep the complete tooltip inside the viewport, including when hovering the lowest tree row. */
    private static final ClientTooltipPositioner TOOLTIP_POSITION = (width, height, x, y, tipWidth, tipHeight) ->
            new org.joml.Vector2i(Math.max(6, Math.min(x + 12, width - tipWidth - 6)),
                    Math.max(6, Math.min(y + 12, height - tipHeight - 6)));
    private static final class Draft {
        final Map<Identifier, ProfessionState.Allocation> baseline;
        final Map<Identifier, Integer> desired;
        Draft(ProfessionState.Track track) { baseline = track.allocations(); desired = new HashMap<>(track.ranks()); }
    }
    private record NodeButton(ProfessionData.Node node, Button button) {}
    private final Map<Identifier, Draft> drafts = new HashMap<>();
    private final List<NodeButton> nodes = new ArrayList<>();
    private final Map<Identifier, Component> nodeTooltips = new HashMap<>();
    private List<net.minecraft.util.FormattedCharSequence> buildingHint = List.of();
    private ExplorationView discoveries = ExplorationView.EMPTY;
    private int discoveryPage, discoveryPages = 1, headerOffset;
    private ProfessionView catalogue = ProfessionView.EMPTY;
    private int selected;
    private int left, top, panelWidth, panelHeight, bodyTop, bodyHeight;
    private Button apply;
    private final Map<Identifier, ApplyFeedback<Map<Identifier, Integer>>> feedback = new HashMap<>();

    ProfessionsScreen() { super(text("title")); }
    private Identifier profession() { return ProgressionData.PROFESSIONS.get(selected); }
    private ProfessionState.Track track() { return minecraft.player.getData(RpgData.PROFESSIONS).tracks().get(profession()); }
    private Draft draft() { return drafts.get(profession()); }
    private static Component professionName(Identifier id) { return Component.translatable("profession." + id.getNamespace() + "." + id.getPath()); }

    @Override protected void init() {
        catalogue = minecraft.player.getData(RpgData.PROFESSION_VIEW);
        reconcileDrafts();
        nodes.clear();
        nodeTooltips.clear();
        panelWidth = Math.min(760, width - 16); panelHeight = Math.min(440, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        int tabColumns = panelWidth < 600 ? 4 : ProgressionData.PROFESSIONS.size();
        headerOffset = (ProgressionData.PROFESSIONS.size() - 1) / tabColumns * 22;
        bodyTop = top + 94 + headerOffset; bodyHeight = panelHeight - 128 - headerOffset;
        buildingHint = profession().equals(ProgressionData.BUILDING)
                ? font.split(text("building_help"), panelWidth - 24)
                : profession().equals(ProgressionData.FISHING) ? font.split(text("fishing_help"), panelWidth - 24)
                : profession().equals(ProgressionData.EXPLORATION) ? font.split(text("exploration_help"), panelWidth - 24) : List.of();
        int hintLines = Math.max(0, (bodyHeight - 58) / font.lineHeight);
        buildingHint = buildingHint.subList(0, Math.min(buildingHint.size(), hintLines));
        int hintHeight = buildingHint.isEmpty() ? 0 : buildingHint.size() * font.lineHeight + 6;
        bodyTop += hintHeight; bodyHeight -= hintHeight;
        int tabWidth = (panelWidth - 16) / tabColumns;
        for (int index = 0; index < ProgressionData.PROFESSIONS.size(); index++) {
            int tab = index;
            var id = ProgressionData.PROFESSIONS.get(index);
            var button = new MenuArt.IconButton(left + 8 + index % tabColumns * tabWidth, top + 29 + index / tabColumns * 22, tabWidth - 2, 20,
                    professionName(id), SkillIcons.ui(id.getPath()), b -> { selected = tab; discoveryPage = 0; rebuildWidgets(); });
            button.setTooltip(Tooltip.create(id.equals(ProgressionData.BUILDING)
                    ? professionName(id).copy().append("\n").append(text("building_help"))
                    : id.equals(ProgressionData.FISHING) ? professionName(id).copy().append("\n").append(text("fishing_help"))
                    : id.equals(ProgressionData.EXPLORATION) ? professionName(id).copy().append("\n").append(text("exploration_help"))
                    : id.equals(ProfessionData.id("crafting"))
                        ? professionName(id).copy().append("\nEnchant at a table to earn Crafting XP.\nLearn Basic Affix Crafting, then use a Covenant Workbench.\nOrdinary crafting grants no Crafting XP.") : professionName(id)));
            button.active = selected != index;
            addRenderableWidget(button);
        }
        var choices = catalogue.nodes(profession());
        var layout = new ProfessionTreeLayout(panelWidth - 24, bodyHeight, choices.size());
        for (int index = 0; index < choices.size(); index++) {
            var node = choices.get(index);
            var button = new ProfessionButton(node, left + 12 + layout.x(index), bodyTop + layout.y(index), layout.width(), layout.height());
            nodes.add(new NodeButton(node, button));
            addRenderableWidget(button);
        }
        discoveries = minecraft.player.getData(RpgData.EXPLORATION_VIEW);
        if (profession().equals(ProgressionData.EXPLORATION)) { addDiscoveries(); }
        apply = addRenderableWidget(Button.builder(text("apply"), b -> {
            feedback().submit(Map.copyOf(track().ranks()), Map.copyOf(draft().desired), minecraft.level.getGameTime());
            ClientPacketDistributor.sendToServer(new ProfessionNetworking.Apply(profession(), catalogue.revision(),
                    draft().baseline, draft().desired));
            refreshButtons();
        }).bounds(left + panelWidth - 83, top + panelHeight - 26, 75, 20).build());
        refreshButtons();
    }

    private void addDiscoveries() {
        int rows = Math.max(1, (bodyHeight - 24) / 29);
        discoveryPages = Math.max(1, (discoveries.entries().size() + rows - 1) / rows);
        discoveryPage = Math.min(discoveryPage, discoveryPages - 1);
        for (int row = 0; row < rows; row++) {
            int index = discoveryPage * rows + row;
            if (index >= discoveries.entries().size()) { break; }
            var entry = discoveries.entries().get(index);
            var label = Component.literal(entry.pool() + " · " + entry.title());
            String progress = entry.claimed() ? text("milestone_complete").getString() : entry.progress() + " / " + entry.required();
            var button = new Button(left + 12, bodyTop + row * 29, panelWidth - 24, 26,
                    label.copy().append(" " + progress), b -> {}, narration -> narration.get()) {
                @Override protected void extractContents(GuiGraphicsExtractor g, int x, int y, float partialTick) {
                    g.fill(getX(), getY(), getRight(), getBottom(), 0xFF302D37);
                    g.outline(getX(), getY(), getWidth(), getHeight(), entry.claimed() ? MenuArt.GOLD : MenuArt.EDGE);
                    MenuArt.label(g, label, getX() + 5, getY() + 3, getWidth() - 10, MenuArt.TEXT);
                    g.text(font, Component.literal(progress), getX() + 5, getY() + 15, entry.claimed() ? MenuArt.GOLD : MenuArt.MUTED);
                }
            };
            button.setTooltip(Tooltip.create(label.copy().append("\n" + progress)));
            addRenderableWidget(button);
        }
        int pagerY = bodyTop + bodyHeight - 22;
        var previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> { discoveryPage--; rebuildWidgets(); })
                .bounds(left + 12, pagerY, 24, 20).build());
        previous.active = discoveryPage > 0;
        var next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { discoveryPage++; rebuildWidgets(); })
                .bounds(left + panelWidth - 36, pagerY, 24, 20).build());
        next.active = discoveryPage + 1 < discoveryPages;
    }

    private ApplyFeedback<Map<Identifier, Integer>> feedback() { return feedback.computeIfAbsent(profession(), id -> new ApplyFeedback<>()); }

    private void reconcileDrafts() {
        var state = minecraft.player.getData(RpgData.PROFESSIONS);
        for (var id : ProgressionData.PROFESSIONS) {
            var previous = drafts.get(id); var track = state.tracks().get(id);
            feedback.computeIfAbsent(id, key -> new ApplyFeedback<>()).observe(track.ranks(), minecraft.level.getGameTime());
            if (previous == null || !previous.baseline.equals(track.allocations())) { drafts.put(id, new Draft(track)); }
        }
    }
    private int pendingCost() {
        int cost = 0;
        for (var entry : draft().desired.entrySet()) {
            var node = catalogue.abilities().get(entry.getKey());
            if (node == null) { continue; }
            int old = draft().baseline.containsKey(entry.getKey()) ? draft().baseline.get(entry.getKey()).rank() : 0;
            for (int index = old; index < entry.getValue(); index++) { cost += node.ranks().get(index).cost(); }
        }
        return cost;
    }
    private boolean canAdd(ProfessionData.Node node) {
        int rank = draft().desired.getOrDefault(node.id(), 0);
        return node.implemented() && rank < node.ranks().size() && track().level() >= node.ranks().get(rank).level()
                && track().points() - pendingCost() >= node.ranks().get(rank).cost()
                && node.prerequisite().filter(id -> draft().desired.getOrDefault(id, 0) == 0).isEmpty();
    }
    private void refreshButtons() {
        for (var entry : nodes) {
            var node = entry.node(); int rank = draft().desired.getOrDefault(node.id(), 0);
            int old = track().rank(node.id());
            entry.button().setMessage(node.ranks().isEmpty() ? node.name()
                    : node.name().copy().append("  " + rank + "/" + node.ranks().size() + (rank > old ? " *" : "")));
            // Keep pending ranks hit-testable for undo even when another purchase is blocked.
            entry.button().active = !feedback().waiting();
            var tip = node.name().copy().withStyle(ChatFormatting.GOLD).append("\n").append(node.description().copy().withStyle(ChatFormatting.GRAY));
            tip.append("\n").append(text(node.implemented() ? "implemented" : "planned"));
            if (!node.ranks().isEmpty()) { tip.append("\n").append(Component.translatable("ui.covenant.profession_current", old, rank)); }
            if (rank < node.ranks().size()) {
                var next = node.ranks().get(rank);
                tip.append("\n").append(Component.translatable("ui.covenant.profession_next", rank + 1, next.level(), next.cost()));
            }
            for (int index = 0; index < node.ranks().size(); index++) {
                var value = node.ranks().get(index);
                tip.append("\n").append(text("rank", index + 1, value.level(), value.cost()));
                if (node.id().equals(ProfessionData.OFFENSIVE_MASTERY)) {
                    tip.append(" · " + String.format(Locale.ROOT, "+%.2f%%", value.bonus() * 100));
                }
                if (value.blocks() > 0) { tip.append(" · ").append(text("blocks", value.blocks())); }
                if (value.area() > 0) { tip.append(" · " + value.area() + "×" + value.area()); }
            }
            node.prerequisite().map(catalogue.abilities()::get).ifPresent(parent -> tip.append("\n").append(text("requires", parent.name())));
            tip.append("\n").append(text("node_help"));
            entry.button().setTooltip(Tooltip.create(tip));
            nodeTooltips.put(node.id(), tip);
        }
        boolean combat = minecraft.player.getData(RpgData.PROGRESSION_VIEW).inCombat();
        apply.active = pendingCost() > 0 && !combat && catalogue.revision() > 0 && !feedback().waiting();
        apply.setTooltip(Tooltip.create(text(combat ? "combat" : "apply_help")));
    }
    private void undo(ProfessionData.Node node) {
        if (feedback().waiting()) { return; }
        int current = draft().desired.getOrDefault(node.id(), 0);
        int committed = track().rank(node.id());
        if (current <= committed) { return; }
        if (current == 1) { draft().desired.remove(node.id()); } else { draft().desired.put(node.id(), current - 1); }
        boolean changed;
        do {
            changed = false;
            for (var entry : nodes) {
                var candidate = entry.node(); int rank = draft().desired.getOrDefault(candidate.id(), 0);
                int base = track().rank(candidate.id());
                if (rank > base && candidate.prerequisite().filter(id -> !draft().desired.containsKey(id)).isPresent()) {
                    if (base == 0) { draft().desired.remove(candidate.id()); } else { draft().desired.put(candidate.id(), base); }
                    changed = true;
                }
            }
        } while (changed);
        feedback().edit(); refreshButtons();
    }
    @Override public void tick() {
        try (var zone = Profiler.get().zone("covenant:professions_screen_tick")) {
            if (minecraft.player == null || !minecraft.player.isAlive() || minecraft.player.isSpectator()
                    || minecraft.level == null || !minecraft.isWindowActive()) { onClose(); return; }
            var view = minecraft.player.getData(RpgData.PROFESSION_VIEW);
            if (view.revision() != catalogue.revision()) { catalogue = view; drafts.clear(); feedback.clear(); rebuildWidgets(); return; }
            catalogue = view;
            if (profession().equals(ProgressionData.EXPLORATION) && !discoveries.equals(minecraft.player.getData(RpgData.EXPLORATION_VIEW))) {
                rebuildWidgets(); return;
            }
            reconcileDrafts(); refreshButtons();
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            for (var entry : nodes) {
                if (entry.button().isMouseOver(event.x(), event.y())) { undo(entry.node()); return true; }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x95080D14);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        try (var zone = Profiler.get().zone("covenant:professions_screen")) {
            MenuArt.panel(graphics, left, top, panelWidth, panelHeight);
            graphics.centeredText(font, title, left + panelWidth / 2, top + 10, 0xFFF5DBA6);
            var track = track(); var view = catalogue.tracks().get(profession());
            if (view != null) {
                var progress = profession().equals(ProgressionData.EXPLORATION)
                        ? text("exploration_progress", track.level(), view.maximum(), discoveries.entries().stream().filter(ExplorationView.Entry::claimed).count(), discoveries.entries().size())
                        : track.level() >= view.maximum()
                        ? text("progress_max", track.level(), view.maximum(), view.dimensionCap())
                        : text("progress", track.level(), view.maximum(), track.experienceFraction() == 0 ? Long.toString(track.experience())
                                : track.experience() + "." + (track.experienceFraction() < 10 ? "0" : "") + track.experienceFraction(), view.requiredXp(), view.dimensionCap());
                MenuArt.label(graphics, progress, left + 10, top + 59 + headerOffset, panelWidth - 20, 0xFFE2E8ED);
            }
            graphics.text(font, text("points", track.points() - pendingCost(), pendingCost()), left + 10, top + 73 + headerOffset, 0xFFF5DBA6);
            graphics.fill(left + 8, top + 90 + headerOffset, left + panelWidth - 8, bodyTop + bodyHeight, 0xFF0F1620);
            for (int index = 0; index < buildingHint.size(); index++) {
                graphics.text(font, buildingHint.get(index), left + 12, top + 94 + headerOffset + index * font.lineHeight, MenuArt.MUTED);
            }
            for (var child : nodes) {
                if (child.node().prerequisite().isEmpty()) { continue; }
                for (var parent : nodes) {
                    if (!parent.node().id().equals(child.node().prerequisite().orElseThrow())) { continue; }
                    int x1 = parent.button().getX() + parent.button().getWidth() / 2, y1 = parent.button().getY() + parent.button().getHeight();
                    int x2 = child.button().getX() + child.button().getWidth() / 2, y2 = child.button().getY();
                    int middle = (y1 + y2) / 2;
                    graphics.fill(x1, Math.min(y1, middle), x1 + 1, Math.max(y1, middle) + 1, 0xFF8E7851);
                    graphics.fill(Math.min(x1, x2), middle, Math.max(x1, x2) + 1, middle + 1, 0xFF8E7851);
                    graphics.fill(x2, Math.min(middle, y2), x2 + 1, Math.max(middle, y2) + 1, 0xFF8E7851);
                }
            }
            if (profession().equals(ProgressionData.EXPLORATION)) {
                graphics.centeredText(font, text("checklist_page", discoveryPage + 1, discoveryPages), left + panelWidth / 2, bodyTop + bodyHeight - 16, MenuArt.MUTED);
            } else if (nodes.isEmpty()) {
                MenuArt.label(graphics, text(catalogue.revision() == 0 ? "loading" : "skills_planned"), left + 12, bodyTop + 10, panelWidth - 24, MenuArt.MUTED);
            }
            MenuArt.label(graphics, feedback().state() == ApplyFeedback.State.IDLE ? text(profession().equals(ProgressionData.FISHING) || profession().equals(ProgressionData.EXPLORATION) ? "skills_planned" : "node_help")
                    : Component.translatable(feedback().key()), left + 10, top + panelHeight - 20, panelWidth - 101, MenuArt.MUTED);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            for (var entry : nodes) {
                var button = entry.button();
                if (button.isHovered() || button.isFocused() && minecraft.getLastInputType().isKeyboard()) {
                    // Wider wrapping keeps the four-rank gathering descriptions readable at normal GUI scales.
                    var lines = font.split(nodeTooltips.get(entry.node().id()), Math.min(320, width - 12));
                    graphics.setTooltipForNextFrame(font, lines, TOOLTIP_POSITION, mouseX, mouseY, true);
                    break;
                }
            }
        }
    }

    private final class ProfessionButton extends Button {
        private final ProfessionData.Node node;
        ProfessionButton(ProfessionData.Node node, int x, int y, int w, int h) {
            super(x, y, w, h, node.name(), b -> {
                if (canAdd(node) && !feedback().waiting()) {
                    draft().desired.merge(node.id(), 1, Integer::sum); feedback().edit(); refreshButtons();
                }
            }, DEFAULT_NARRATION);
            this.node = node;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            int rank = draft().desired.getOrDefault(node.id(), 0), old = track().rank(node.id());
            boolean pending = rank > old;
            int edge = pending ? MenuArt.PENDING : old > 0 ? MenuArt.GOLD : MenuArt.EDGE;
            g.fill(getX(), getY(), getRight(), getBottom(), 0xFF302D37);
            g.outline(getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused() ? MenuArt.TEXT : edge);
            String path = node.id().getPath().substring(node.id().getPath().lastIndexOf('/') + 1);
            String emblem = switch (path) { case "basic_affix_crafting" -> "basic"; case "focused_recovery" -> "focused"; default -> path; };
            int iconSize = Math.min(16, getHeight() - 4);
            if (!SkillIcons.draw(g, SkillIcons.ui(emblem), getX() + 3, getY() + 3, iconSize, -1)) {
                SkillIcons.draw(g, SkillIcons.ui("unknown"), getX() + 3, getY() + 3, iconSize, -1);
            }
            MenuArt.label(g, node.name(), getX() + 22, getY() + 3, getWidth() - 26, MenuArt.TEXT);
            Component status;
            if (!node.implemented()) { status = Component.translatable("ui.covenant.state.planned"); }
            else if (rank >= node.ranks().size()) { status = Component.translatable("ui.covenant.rank_max", rank); }
            else {
                var next = node.ranks().get(rank);
                status = Component.translatable(pending ? "ui.covenant.rank_pending" : canAdd(node) ? "ui.covenant.rank_next" : "ui.covenant.rank_locked",
                        rank, rank + 1, next.cost(), next.level());
            }
            if (getHeight() >= 24) { MenuArt.label(g, status, getX() + 22, getBottom() - 11, getWidth() - 26, edge); }
        }
    }
    @Override public void removed() { drafts.clear(); super.removed(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private static Component text(String key, Object... args) { return Component.translatable("screen.covenant.professions." + key, args); }
}
