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
    private final ApplyFeedback<java.util.Map<net.minecraft.resources.Identifier, Integer>> feedback = new ApplyFeedback<>();

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
        if (currentClass == null) {
            addRenderableWidget(Button.builder(Component.translatable("screen.covenant.stats.title"), b -> minecraft.setScreen(new StatsScreen()))
                    .bounds(tabX, top + 96, 77, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(tabX, top + panelHeight - 51, 77, 20).build());
        apply = addRenderableWidget(Button.builder(Component.literal("Apply"), b -> {
            feedback.submit(baseline, java.util.Map.copyOf(draft), minecraft.level.getGameTime());
            ClientPacketDistributor.sendToServer(new SkillNetworking.Apply(baseline, draft));
            refresh();
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
        int pickerWidth = Math.max(68, viewWidth / 3);
        int rows = Math.max(1, (viewHeight - 17) / 24);
        bindingScroll = Math.clamp(bindingScroll, 0, Math.max(0, bindingChoices.size() - rows));
        for (int i = bindingScroll; i < Math.min(bindingChoices.size(), bindingScroll + rows); i++) {
            var node = bindingChoices.get(i);
            var button = new MenuArt.IconButton(viewLeft, viewTop + 17 + (i - bindingScroll) * 24, pickerWidth, 22,
                    selected == node ? Component.literal("> ").append(node.displayName()) : node.displayName(),
                    node.id(), b -> { selected = node; rebuildWidgets(); });
            button.setTooltip(Tooltip.create(nodeTooltip(node)));
            addRenderableWidget(button);
        }
        int slotX = viewLeft + pickerWidth + 8;
        int slotWidth = (viewWidth - pickerWidth - 8) / 2;
        int rowHeight = Math.max(23, Math.min(43, (viewHeight - 17) / 4));
        for (int slot = 0; slot < SkillLoadout.SLOT_COUNT; slot++) {
            final int index = slot;
            var button = new BindingButton(index, slotX + (slot % 2) * slotWidth, viewTop + 17 + (slot / 2) * rowHeight,
                    slotWidth - 3, rowHeight - 3);
            slots.add(addRenderableWidget(button));
        }
    }

    private SkillLoadout loadout() { return minecraft.player.getData(RpgData.SKILL_LOADOUT); }

    private void refresh() {
        var state = loadout();
        feedback.observe(state.ranks(), minecraft.level.getGameTime());
        if (!state.ranks().equals(baseline)) { baseline = state.ranks(); draft = new java.util.HashMap<>(baseline); }
        boolean combat = minecraft.player.getData(RpgData.PROGRESSION_VIEW).inCombat();
        apply.active = !draft.equals(baseline) && !combat && !feedback.waiting();
        apply.setTooltip(Tooltip.create(Component.translatable(combat ? "ui.covenant.combat" : "ui.covenant.apply_help")));
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
            // Locked/planned nodes remain focusable for inspection; purchase is guarded in onPress.
            button.active = true;
            button.setMessage(node.displayName().copy().append(" · ").append(nodeState(node))
                    .append(" · " + draft.getOrDefault(node.id(), 0) + "/" + node.availableMaximum()));
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
        tip.append("\n").append(nodeState(node));
        loadout().bindings().forEach((slot, id) -> {
            if (id.equals(node.id())) { tip.append("\n").append(Component.translatable("ui.covenant.assigned", SkillKeyMappings.label(slot))); }
        });
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
        if (feedback.waiting()) { return; }
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
        feedback.edit(); refresh();
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
            MenuArt.panel(graphics, left, top, panelWidth, panelHeight);
            graphics.centeredText(font, title, left + panelWidth / 2, top + 8, 0xFFF5DBA6);
            graphics.centeredText(font, currentClass == null ? text("no_class") : currentClass.displayName(),
                    left + panelWidth / 2, top + 24, 0xFFE2E8ED);
            if (currentClass != null) { SkillIcons.draw(graphics, SkillIcons.ui(currentClass.id().getPath()), left + 9, top + 16, 16, -1); }
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
                    : Component.translatable("ui.covenant.skill_points", remaining,
                            draft.values().stream().mapToInt(Integer::intValue).sum() - loadout().spent());
            if (feedback.state() != ApplyFeedback.State.IDLE) { hint = Component.translatable(feedback.key()); }
            int hintY = top + panelHeight - (bindingsTab ? 47 : 23);
            var lines = font.split(hint, viewWidth - 4);
            for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                graphics.text(font, lines.get(i), viewLeft + 2, hintY + i * 10, 0xFFB6C3D1);
            }
        }
    }

    private Component nodeState(SkillTree.Node node) {
        String state = !node.implemented() ? "planned"
                : draft.getOrDefault(node.id(), 0) > baseline.getOrDefault(node.id(), 0) ? "pending"
                : baseline.containsKey(node.id()) ? "learned" : canAdd(node) ? "available" : "locked";
        return Component.translatable("ui.covenant.state." + state);
    }

    private final class BindingButton extends Button {
        private final int slot;
        BindingButton(int slot, int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), b -> {
                if (selected != null && loadout().learned().contains(selected.id())) {
                    ClientPacketDistributor.sendToServer(new SkillNetworking.Bind(slot, Optional.of(selected.id())));
                }
            }, DEFAULT_NARRATION);
            this.slot = slot;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            g.fill(getX(), getY(), getRight(), getBottom(), 0xFF35323E);
            g.outline(getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused() ? MenuArt.GOLD : MenuArt.EDGE);
            // Keys get the whole first line; skill art/name get the second line. Narration/tooltips retain both in full.
            MenuArt.label(g, SkillKeyMappings.label(slot), getX() + 3, getY() + 2, getWidth() - 6, MenuArt.GOLD);
            var id = loadout().bindings().get(slot);
            var node = id == null ? null : SkillTree.find(id).orElse(null);
            boolean local = node != null && node.playerClass() == currentClass;
            int size = Math.min(16, getHeight() - 13);
            SkillIcons.draw(g, local ? id : SkillIcons.ui("unknown"), getX() + 3, getY() + 12, size, -1);
            MenuArt.label(g, id == null ? text("empty") : local ? node.displayName() : text("other_class"),
                    getX() + Math.max(0, size) + 6, getY() + 12, getWidth() - Math.max(0, size) - 9, MenuArt.TEXT);
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
                if (canAdd(node) && !feedback.waiting()) { draft.merge(node.id(), 1, Integer::sum); feedback.edit(); refresh(); }
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
            boolean art = SkillIcons.draw(graphics, node.id(), getX() + 2, getY() + 2, getWidth() - 4,
                    locked && !learned ? 0xFF8997A5 : 0xFFFFFFFF);
            // Unauthored skills retain initials; names and rules remain in hover/focus tooltips.
            String label = font.width(initials) <= getWidth() - 4 ? initials : initials.substring(0, 1);
            if (!art) { graphics.centeredText(font, label, getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2,
                    learned ? 0xFFF5DBA6 : locked ? 0xFF77828E : 0xFFE2E8ED); }
            // Fixed corner shapes survive tiny nodes. Full state/rank/binding is in the focus tooltip.
            boolean pending = draft.getOrDefault(node.id(), 0) > baseline.getOrDefault(node.id(), 0);
            String badge = !node.implemented() ? "…" : pending ? "*" : learned ? Integer.toString(draft.get(node.id())) : locked ? "-" : "+";
            if (getWidth() >= 18) { MenuArt.badge(graphics, badge, getRight() - 1, getBottom() - 9, border); }
            else {
                int x = getRight() - 5, y = getBottom() - 6;
                graphics.fill(x - 1, y - 1, getRight() - 1, getBottom() - 1, 0xF0101017);
                if (!node.implemented()) {
                    for (int i = 0; i < 3; i++) { graphics.fill(x + i * 2 - 1, y + 3, x + i * 2, y + 4, MenuArt.MUTED); }
                } else if (pending) {
                    graphics.fill(x, y + 2, x + 3, y + 3, MenuArt.PENDING);
                    graphics.fill(x + 1, y + 1, x + 2, y + 4, MenuArt.PENDING);
                } else if (learned) {
                    graphics.fill(x, y + 2, x + 1, y + 3, MenuArt.GOLD);
                    graphics.fill(x + 1, y + 3, x + 2, y + 4, MenuArt.GOLD);
                    graphics.fill(x + 2, y + 1, x + 3, y + 3, MenuArt.GOLD);
                } else if (locked) {
                    graphics.outline(x, y, 3, 4, MenuArt.MUTED);
                    graphics.fill(x - 1, y + 2, x + 4, y + 5, MenuArt.MUTED);
                } else { graphics.outline(x, y + 1, 3, 3, MenuArt.TEXT); }
            }
            if (loadout().bindings().containsValue(node.id())) {
                graphics.fill(getX() + 1, getY() + 1, getX() + 4, getY() + 3, MenuArt.TEXT);
            }
        }
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private static MutableComponent text(String key, Object... args) {
        return Component.translatable("screen.covenant.skills." + key, args);
    }
}
