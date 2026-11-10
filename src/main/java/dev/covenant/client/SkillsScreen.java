package dev.covenant.client;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.SkillLoadout;
import dev.covenant.rpg.skills.SkillNetworking;
import dev.covenant.rpg.skills.SkillTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.profiling.Profiler;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Current-class tree and a separate right-hand Keybindings tab. Edits are server requests. */
final class SkillsScreen extends Screen {
    private final List<NodeButton> nodes = new ArrayList<>();
    private final List<Button> slots = new ArrayList<>();
    private PlayerClass currentClass;
    private SkillTree.Node selected;
    private boolean bindingsTab;
    private SkillLoadout previous;
    private int previousLevel = -1;
    private java.util.Map<net.minecraft.resources.Identifier, Integer> previousDraft = java.util.Map.of();
    private SkillTreeLayout treeLayout;
    private List<Integer> treeColumns = List.of();
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int viewLeft;
    private int viewTop;
    private int viewWidth;
    private int viewHeight;
    private int bindingScroll;
    private List<SkillTree.Node> bindingChoices = List.of();
    private java.util.Map<net.minecraft.resources.Identifier, Integer> baseline;
    private java.util.Map<net.minecraft.resources.Identifier, Integer> draft;
    private Button apply;

    SkillsScreen() { super(text("title")); }

    @Override protected void init() {
        nodes.clear();
        slots.clear();
        panelWidth = Math.min(880, width - 16);
        panelHeight = Math.min(490, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        viewLeft = left + 8;
        viewTop = top + 43;
        viewWidth = panelWidth - 100;
        viewHeight = panelHeight - (bindingsTab ? 99 : 73);
        currentClass = PlayerProfiles.getSelectedClass(minecraft.player).orElse(null);
        int tabX = left + panelWidth - 85;
        addTab("tree_tab", tabX, top + 43, false);
        addTab("bindings_tab", tabX, top + 68, true);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(tabX, top + panelHeight - 51, 77, 20).build());
        apply = addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            ClientPacketDistributor.sendToServer(new SkillNetworking.Apply(baseline, draft));
        }).bounds(tabX, top + panelHeight - 27, 77, 20).build());
        if (baseline == null || !baseline.equals(loadout().ranks())) {
            baseline = loadout().ranks(); draft = new java.util.HashMap<>(baseline);
        }
        if (bindingsTab) { initBindings(); }
        else {
            var tree = SkillTree.forClass(currentClass);
            treeColumns = tree.stream().map(SkillTree.Node::column).distinct().sorted().toList();
            treeLayout = new SkillTreeLayout(viewLeft, viewTop, viewWidth, viewHeight,
                    Math.max(1, treeColumns.size()), SkillTree.LEVELS.size());
            for (var node : tree) {
                nodes.add(addRenderableWidget(new NodeButton(node)));
            }
        }
        previous = null;
        refresh();
    }

    private void addTab(String key, int x, int y, boolean bindings) {
        var tab = Button.builder(text(key), b -> { bindingsTab = bindings; rebuildWidgets(); })
                .bounds(x, y, 77, 22).build();
        tab.active = bindingsTab != bindings;
        addRenderableWidget(tab);
    }

    private void initBindings() {
        bindingChoices = SkillTree.forClass(currentClass).stream().filter(n -> loadout().learned().contains(n.id())).toList();
        int pickerWidth = Math.max(72, viewWidth / 2 - 5);
        int rows = Math.max(1, (viewHeight - 17) / 24);
        bindingScroll = Math.clamp(bindingScroll, 0, Math.max(0, bindingChoices.size() - rows));
        for (int i = bindingScroll; i < Math.min(bindingChoices.size(), bindingScroll + rows); i++) {
            var node = bindingChoices.get(i);
            var button = Button.builder(node.displayName(), b -> { selected = node; rebuildWidgets(); })
                    .bounds(viewLeft, viewTop + 17 + (i - bindingScroll) * 24, pickerWidth, 22)
                    .tooltip(Tooltip.create(nodeTooltip(node))).build();
            button.setFGColor(node == selected ? 0xFFF5DBA6 : 0xFFE2E8ED);
            addRenderableWidget(button);
        }
        int slotX = viewLeft + pickerWidth + 8;
        int slotWidth = (viewWidth - pickerWidth - 8) / 2;
        int rowHeight = Math.max(23, Math.min(43, (viewHeight - 17) / 4));
        for (int slot = 0; slot < SkillLoadout.SLOT_COUNT; slot++) {
            final int index = slot;
            var button = Button.builder(Component.empty(), b -> {
                if (selected != null && loadout().learned().contains(selected.id())) {
                    ClientPacketDistributor.sendToServer(new SkillNetworking.Bind(index, Optional.of(selected.id())));
                }
            }).bounds(slotX + (slot % 2) * slotWidth, viewTop + 17 + (slot / 2) * rowHeight,
                    slotWidth - 3, rowHeight - 3).build();
            slots.add(addRenderableWidget(button));
        }
    }

    private SkillLoadout loadout() { return minecraft.player.getData(RpgData.SKILL_LOADOUT); }

    private void refresh() {
        var state = loadout();
        if (!state.ranks().equals(baseline)) { baseline = state.ranks(); draft = new java.util.HashMap<>(baseline); }
        apply.active = !draft.equals(baseline) && !minecraft.player.getData(RpgData.PROGRESSION_VIEW).inCombat();
        int level = PlayerProfiles.get(minecraft.player).level();
        if (state.equals(previous) && level == previousLevel && draft.equals(previousDraft)) { return; }
        previousLevel = level;
        previousDraft = java.util.Map.copyOf(draft);
        var pending = new SkillLoadout(draft, java.util.Map.of());
        if (selected != null && (!state.learned().contains(selected.id()) || selected.playerClass() != currentClass)) { selected = null; }
        previous = state;
        for (var button : nodes) {
            var node = button.node;
            button.learned = pending.rank(node.id()) > 0;
            boolean enabled = canAdd(node);
            button.locked = !enabled;
            // Native hit-testing ignores inactive widgets, including our right-click undo.
            button.active = enabled || draft.getOrDefault(node.id(), 0) > baseline.getOrDefault(node.id(), 0);
            button.setTooltip(Tooltip.create(nodeTooltip(node)));
        }
        for (int i = 0; i < slots.size(); i++) {
            var id = state.bindings().get(i);
            var node = id == null ? null : SkillTree.find(id).orElse(null);
            // Inactive-class assignments persist but their skill names are not exposed on this class's page.
            var name = id == null ? text("empty") : node == null || node.playerClass() != currentClass
                    ? text("other_class") : node.displayName();
            var label = SkillKeyMappings.label(i).copy().append(": ").append(name);
            slots.get(i).setMessage(label);
            slots.get(i).setTooltip(Tooltip.create(label.copy().append("\n").append(text("slot_hint"))));
        }
    }

    private Component nodeTooltip(SkillTree.Node node) {
        var tip = node.displayName().copy().withStyle(ChatFormatting.GOLD).append("\n")
                .append(node.description().copy().withStyle(ChatFormatting.GRAY));
        tip.append("\nLevel " + node.diagramLevel() + (node.implemented()
                ? " · Rank " + draft.getOrDefault(node.id(), 0) + "/" + node.availableMaximum() + " (planned " + node.plannedMaximum() + ")"
                : " · Rank cap TBD"));
        int pending = draft.getOrDefault(node.id(), 0) - baseline.getOrDefault(node.id(), 0);
        if (pending > 0) { tip.append("\nPending: +" + pending).withStyle(ChatFormatting.AQUA); }
        if (node.availableMaximum() == 8) { tip.append("\n+2% launch velocity per rank after Rank 1; native impact damage scales with speed."); }
        node.prerequisite().ifPresent(id -> tip.append("\n").append(text("requires", SkillTree.find(id).orElseThrow().displayName())));
        node.exclusive().ifPresent(id -> tip.append("\n").append(text("exclusive", SkillTree.find(id).orElseThrow().displayName())));
        tip.append("\n").append(text(node.implemented() ? "implemented" : "planned").withStyle(ChatFormatting.AQUA));
        SkillTree.blocked(node, new SkillLoadout(draft, java.util.Map.of())).ifPresent(reason -> tip.append("\n").append(reason.copy().withStyle(ChatFormatting.RED)));
        return tip.append("\nLeft click: pending upgrade. Right click: remove pending.").withStyle(ChatFormatting.GRAY);
    }

    private boolean canAdd(SkillTree.Node node) {
        return node.playerClass() == currentClass && (baseline.containsKey(node.id()) || PlayerProfiles.get(minecraft.player).level() >= node.diagramLevel())
                && draft.getOrDefault(node.id(), 0) < node.availableMaximum()
                && draft.values().stream().mapToInt(Integer::intValue).sum() < PlayerProfiles.get(minecraft.player).level()
                && (draft.containsKey(node.id()) || draft.size() < 8)
                && SkillTree.blocked(node, new SkillLoadout(draft, java.util.Map.of())).isEmpty();
    }

    private void removePending(SkillTree.Node node) {
        int rank = draft.getOrDefault(node.id(), 0);
        if (rank <= baseline.getOrDefault(node.id(), 0)) { return; }
        if (rank == 1) { draft.remove(node.id()); } else { draft.put(node.id(), rank - 1); }
        boolean changed;
        do {
            changed = false;
            for (var candidate : SkillTree.ALL) {
                if (!baseline.containsKey(candidate.id()) && draft.containsKey(candidate.id())
                        && candidate.prerequisite().filter(id -> !draft.containsKey(id)).isPresent()) {
                    draft.remove(candidate.id()); changed = true;
                }
            }
        } while (changed);
        refresh();
    }

    @Override public void tick() {
        try (var zone = Profiler.get().zone("covenant:skills_screen_tick")) {
            if (minecraft.player == null || !minecraft.player.isAlive() || minecraft.player.isSpectator()
                    || minecraft.level == null || !minecraft.isWindowActive()) { onClose(); return; }
            if (PlayerProfiles.getSelectedClass(minecraft.player).orElse(null) != currentClass) {
                baseline = null;
                selected = null;
                bindingScroll = 0;
                rebuildWidgets();
            } else if (bindingsTab && previous != null && !loadout().learned().equals(previous.learned())) {
                rebuildWidgets();
            } else { refresh(); }
        }
    }

    private int nodeX(SkillTree.Node node) { return treeLayout.x(treeColumns.indexOf(node.column())); }
    private int nodeY(SkillTree.Node node) { return treeLayout.y(SkillTree.LEVELS.indexOf(node.diagramLevel())); }
    private boolean inView(double x, double y) {
        return x >= viewLeft && x < viewLeft + viewWidth && y >= viewTop && y < viewTop + viewHeight;
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (bindingsTab && inView(x, y)) {
            bindingScroll -= (int) Math.signum(vertical);
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            for (var button : nodes) {
                if (button.visible && button.isMouseOver(event.x(), event.y())) {
                    removePending(button.node);
                    return true;
                }
            }
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i).isMouseOver(event.x(), event.y())) {
                    ClientPacketDistributor.sendToServer(new SkillNetworking.Bind(i, Optional.empty()));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x95080D14);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        try (var zone = Profiler.get().zone("covenant:skills_screen")) {
            graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF018202B);
            graphics.outline(left, top, panelWidth, panelHeight, 0xFF8E7851);
            graphics.centeredText(font, title, left + panelWidth / 2, top + 8, 0xFFF5DBA6);
            graphics.centeredText(font, currentClass == null ? text("no_class") : currentClass.displayName(),
                    left + panelWidth / 2, top + 24, 0xFFE2E8ED);
            graphics.fill(viewLeft, viewTop, viewLeft + viewWidth, viewTop + viewHeight, 0xFF0F1620);
            if (!bindingsTab) { extractTree(graphics); }
            else {
                graphics.text(font, text("learned_picker"), viewLeft + 2, viewTop + 3, 0xFFF5DBA6);
                if (bindingChoices.isEmpty()) {
                    graphics.text(font, text("no_learned"), viewLeft + 2, viewTop + 21, 0xFF94A3B3);
                }
            }
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            int remaining = PlayerProfiles.get(minecraft.player).level() - draft.values().stream().mapToInt(Integer::intValue).sum();
            var hint = currentClass == null ? text("choose_class") : bindingsTab ? text("binding_help")
                    : Component.literal(remaining + " points · Click to draft upgrades. Apply out of combat.");
            int hintY = top + panelHeight - (bindingsTab ? 47 : 23);
            var lines = font.split(hint, viewWidth - 4);
            for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                graphics.text(font, lines.get(i), viewLeft + 2, hintY + i * 10, 0xFFB6C3D1);
            }
        }
    }

    private void extractTree(GuiGraphicsExtractor graphics) {
        graphics.enableScissor(viewLeft, viewTop, viewLeft + viewWidth, viewTop + viewHeight);
        for (int i = 0; i < SkillTree.LEVELS.size(); i++) {
            int y = treeLayout.y(i) + treeLayout.nodeSize() / 2 - 4;
            graphics.text(font, Component.literal("L" + SkillTree.LEVELS.get(i)), viewLeft + 3, y, 0xFF77828E);
        }
        for (var button : nodes) {
            var node = button.node;
            int size = treeLayout.nodeSize();
            int x = nodeX(node) + size / 2;
            int y = nodeY(node);
            node.prerequisite().ifPresent(id -> {
                var prerequisite = SkillTree.find(id).orElseThrow();
                int color = loadout().learned().contains(prerequisite.id()) ? 0xFFE5C279 : 0xFF576474;
                graphics.fill(x - 1, y + size, x + 1, nodeY(prerequisite), color);
            });
            node.exclusive().filter(id -> node.id().compareTo(id) < 0).ifPresent(id -> {
                var other = SkillTree.find(id).orElseThrow();
                int x0 = Math.min(nodeX(node), nodeX(other)) + size;
                int x1 = Math.max(nodeX(node), nodeX(other));
                graphics.fill(x0, y + size / 2, x1, y + size / 2 + 1, 0xFFBF7373);
                graphics.centeredText(font, "x", (x0 + x1) / 2, y + size / 2 - 4, 0xFFFF9292);
            });
        }
        graphics.disableScissor();
    }

    private final class NodeButton extends Button {
        private final SkillTree.Node node;
        private boolean learned;
        private boolean locked;
        private final String initials;
        private NodeButton(SkillTree.Node node) {
            super(nodeX(node), nodeY(node), treeLayout.nodeSize(), treeLayout.nodeSize(), node.displayName(), b -> {
                selected = node;
                if (canAdd(node)) { draft.merge(node.id(), 1, Integer::sum); refresh(); }
            }, DEFAULT_NARRATION);
            this.node = node;
            var words = node.displayName().getString().split(" ");
            initials = words[0].substring(0, 1) + (words.length > 1 ? words[words.length - 1].substring(0, 1) : "");
        }

        @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int border = draft.getOrDefault(node.id(), 0) > baseline.getOrDefault(node.id(), 0) ? 0xFF66D9E8
                    : learned ? 0xFFE5C279 : locked ? 0xFF576474 : 0xFF8E9CAF;
            graphics.fill(getX(), getY(), getRight(), getBottom(), learned ? 0xFF443927 : 0xFF202C3B);
            graphics.outline(getX(), getY(), getWidth(), getHeight(), border);
            if (isHoveredOrFocused() || selected == node) {
                graphics.outline(getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 0xFFF5DBA6);
            }
            // Initials stand in for future icons; full names and rules remain in native hover/focus tooltips.
            String label = font.width(initials) <= getWidth() - 4 ? initials : initials.substring(0, 1);
            graphics.centeredText(font, label, getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2,
                    learned ? 0xFFF5DBA6 : locked ? 0xFF77828E : 0xFFE2E8ED);
        }
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private static MutableComponent text(String key, Object... args) {
        return Component.translatable("screen.covenant.skills." + key, args);
    }
}
