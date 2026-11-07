package dev.covenant.client;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.progression.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Classless owner view. Each tab keeps its own draft; only Apply submits server intent. */
final class ProfessionsScreen extends Screen {
    private static final class Draft {
        final Map<Identifier, ProfessionState.Allocation> baseline;
        final Map<Identifier, Integer> desired;
        Draft(ProfessionState.Track track) { baseline = track.allocations(); desired = new HashMap<>(track.ranks()); }
    }
    private record NodeButton(ProfessionData.Node node, Button button) {}
    private final Map<Identifier, Draft> drafts = new HashMap<>();
    private final List<NodeButton> nodes = new ArrayList<>();
    private ProfessionView catalogue = ProfessionView.EMPTY;
    private int selected;
    private int left, top, panelWidth, panelHeight, bodyTop, bodyHeight;
    private Button apply;

    ProfessionsScreen() { super(text("title")); }
    private Identifier profession() { return ProgressionData.PROFESSIONS.get(selected); }
    private ProfessionState.Track track() { return minecraft.player.getData(RpgData.PROFESSIONS).tracks().get(profession()); }
    private Draft draft() { return drafts.get(profession()); }
    private static Component professionName(Identifier id) { return Component.translatable("profession." + id.getNamespace() + "." + id.getPath()); }

    @Override protected void init() {
        catalogue = minecraft.player.getData(RpgData.PROFESSION_VIEW);
        reconcileDrafts();
        nodes.clear();
        panelWidth = Math.min(760, width - 16); panelHeight = Math.min(440, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        bodyTop = top + 94; bodyHeight = panelHeight - 128;
        int tabWidth = (panelWidth - 16) / 4;
        for (int index = 0; index < 4; index++) {
            int tab = index;
            var button = Button.builder(professionName(ProgressionData.PROFESSIONS.get(index)), b -> { selected = tab; rebuildWidgets(); })
                    .bounds(left + 8 + index * tabWidth, top + 29, tabWidth - 2, 20).build();
            button.active = selected != index;
            addRenderableWidget(button);
        }
        var choices = catalogue.nodes(profession());
        var layout = new ProfessionTreeLayout(panelWidth - 24, bodyHeight, choices.size());
        for (int index = 0; index < choices.size(); index++) {
            var node = choices.get(index);
            var button = Button.builder(node.name(), b -> {
                if (canAdd(node)) { draft().desired.merge(node.id(), 1, Integer::sum); refreshButtons(); }
            }).bounds(left + 12 + layout.x(index), bodyTop + layout.y(index), layout.width(), layout.height()).build();
            nodes.add(new NodeButton(node, button));
            addRenderableWidget(button);
        }
        apply = addRenderableWidget(Button.builder(text("apply"), b -> {
            ClientPacketDistributor.sendToServer(new ProfessionNetworking.Apply(profession(), catalogue.revision(),
                    draft().baseline, draft().desired));
        }).bounds(left + panelWidth - 83, top + panelHeight - 26, 75, 20).build());
        refreshButtons();
    }

    private void reconcileDrafts() {
        var state = minecraft.player.getData(RpgData.PROFESSIONS);
        for (var id : ProgressionData.PROFESSIONS) {
            var previous = drafts.get(id); var track = state.tracks().get(id);
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
            entry.button().setMessage(node.name().copy().append("  " + rank + "/" + node.ranks().size() + (rank > old ? " *" : "")));
            // Keep pending ranks hit-testable for undo even when another purchase is blocked.
            entry.button().active = canAdd(node) || rank > old;
            var tip = node.name().copy().withStyle(ChatFormatting.GOLD).append("\n").append(node.description().copy().withStyle(ChatFormatting.GRAY));
            tip.append("\n").append(text(node.implemented() ? "implemented" : "planned"));
            for (int index = 0; index < node.ranks().size(); index++) {
                var value = node.ranks().get(index);
                tip.append("\n").append(text("rank", index + 1, value.level(), value.cost()));
                if (node.id().equals(ProfessionData.OFFENSIVE_MASTERY)) {
                    tip.append(" · " + String.format(Locale.ROOT, "+%.2f%%", value.bonus() * 100));
                }
            }
            node.prerequisite().map(catalogue.abilities()::get).ifPresent(parent -> tip.append("\n").append(text("requires", parent.name())));
            tip.append("\n").append(text("node_help"));
            entry.button().setTooltip(Tooltip.create(tip));
        }
        boolean combat = minecraft.player.getData(RpgData.PROGRESSION_VIEW).inCombat();
        apply.active = pendingCost() > 0 && !combat && catalogue.revision() > 0;
        apply.setTooltip(Tooltip.create(text(combat ? "combat" : "apply_help")));
    }
    private void undo(ProfessionData.Node node) {
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
        refreshButtons();
    }
    @Override public void tick() {
        if (minecraft.player == null || !minecraft.player.isAlive() || minecraft.player.isSpectator()
                || minecraft.level == null || !minecraft.isWindowActive()) { onClose(); return; }
        var view = minecraft.player.getData(RpgData.PROFESSION_VIEW);
        if (view.revision() != catalogue.revision()) { catalogue = view; drafts.clear(); rebuildWidgets(); return; }
        catalogue = view;
        reconcileDrafts(); refreshButtons();
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
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF018202B);
        graphics.outline(left, top, panelWidth, panelHeight, 0xFF8E7851);
        graphics.centeredText(font, title, left + panelWidth / 2, top + 10, 0xFFF5DBA6);
        var track = track(); var view = catalogue.tracks().get(profession());
        if (view != null) {
            var progress = track.level() >= view.maximum()
                    ? text("progress_max", track.level(), view.maximum(), view.dimensionCap())
                    : text("progress", track.level(), view.maximum(), track.experience(), view.requiredXp(), view.dimensionCap());
            graphics.text(font, progress, left + 10, top + 59, 0xFFE2E8ED);
        }
        graphics.text(font, text("points", track.points() - pendingCost(), pendingCost()), left + 10, top + 73, 0xFFF5DBA6);
        graphics.fill(left + 8, bodyTop - 4, left + panelWidth - 8, bodyTop + bodyHeight, 0xFF0F1620);
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
        if (nodes.isEmpty()) { graphics.centeredText(font, text("loading"), left + panelWidth / 2, bodyTop + 10, 0xFFB6C3D1); }
        graphics.text(font, text("provisional"), left + 10, top + panelHeight - 20, 0xFFB6C3D1);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void removed() { drafts.clear(); super.removed(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private static Component text(String key, Object... args) { return Component.translatable("screen.covenant.professions." + key, args); }
}
