package dev.covenant.client;

import dev.covenant.rpg.equipment.WorkbenchMenu;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Native inventory slots with token-bound server choices and explicit transaction consequences. */
public final class WorkbenchScreen extends AbstractContainerScreen<WorkbenchMenu> {
    private final List<Button> operations = new ArrayList<>();
    private Button family, rarity, target, action, craftTab, salvageTab;
    private WorkbenchMenu.View previous;
    private boolean openingReview;
    public WorkbenchScreen(WorkbenchMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 304, 232); }
    @Override protected void init() {
        super.init(); operations.clear(); previous = null;
        craftTab = button("Affix Crafting", 8, 4, 140, () -> send(4));
        salvageTab = button("Salvaging", 156, 4, 140, () -> send(5));
        var names = List.of("Add", "Upgrade", "Reroll", "Remove");
        for (int n = 0; n < 4; n++) { int index = n;
            operations.add(button(names.get(n), 56 + n * 60, 28, 58, () -> choose(0, index, menu.view.token())));
        }
        family = button("Family", 56, 52, 132, () -> review(1));
        rarity = button("Rarity", 192, 52, 104, () -> review(2));
        target = button("Affix", 56, 52, 240, () -> review(3));
        button("Details", 171, 126, 46, () -> review(-1));
        action = button("Craft", 220, 126, 76, () -> { send(1000 + menu.view.token()); action.active = false; });
        update();
    }
    private Button button(String label, int x, int y, int w, Runnable press) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> press.run()).bounds(leftPos + x, topPos + y, w, 20).build());
    }
    private void send(int id) { if (minecraft.gameMode != null) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); } }
    private void choose(int kind, int index, int token) { ClientPacketDistributor.sendToServer(new WorkbenchMenu.Selection(menu.containerId, token, kind, index)); }
    private void review(int kind) { openingReview = true; minecraft.setScreen(new ReviewScreen(kind)); openingReview = false; }
    @Override public void removed() { if (!openingReview) { super.removed(); } }
    private void update() {
        try (var zone = Profiler.get().zone("covenant:workbench_screen_update")) {
            var view = menu.view; if (view == previous) { return; } previous = view;
            boolean crafting = view.tab() == 0, add = view.operation().equals("Add");
            craftTab.active = !crafting; salvageTab.active = crafting;
            for (var b : operations) { b.visible = crafting; b.active = !b.getMessage().getString().equals(view.operation()); }
            family.visible = rarity.visible = crafting && add; target.visible = crafting && !add;
            setChoice(family, "Family: " + view.family()); setChoice(rarity, view.rarity()); setChoice(target, view.target());
            target.active = !view.targets().isEmpty();
            action.active = view.enabled(); action.setMessage(Component.literal(crafting ? view.operation() : "Salvage"));
            action.setTooltip(Tooltip.create(Component.literal(view.preview())));
        }
    }
    private void setChoice(Button b, String name) { b.setMessage(Component.literal(name)); b.setTooltip(Tooltip.create(Component.literal(name + " · choose from list"))); }
    @Override protected void containerTick() { super.containerTick(); update(); }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        try (var zone = Profiler.get().zone("covenant:workbench_screen_background")) {
            super.extractBackground(g, mouseX, mouseY, a);
            MenuArt.panel(g, leftPos, topPos, imageWidth, imageHeight);
            for (var slot : menu.slots) { if (slot.isActive()) {
                g.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF111117);
                g.outline(leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, MenuArt.EDGE);
            } }
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        try (var zone = Profiler.get().zone("covenant:workbench_screen_labels")) {
            if (menu.view.tab() == 1) { g.text(font, "Consumes", 8, 26, MenuArt.TEXT); g.text(font, "Shared results", 112, 26, MenuArt.TEXT); }
            else { g.text(font, "Gear", 17, 35, MenuArt.TEXT); }
            var d = menu.view.details();
            row(g, "Cost: " + d.cost(), 79, MenuArt.TEXT);
            row(g, "Potential: " + d.potential(), 90, MenuArt.GOLD);
            row(g, "Result: " + d.result(), 101, MenuArt.TEXT);
            row(g, d.reason(), 113, menu.view.enabled() ? 0xFFB4D5A0 : 0xFFF2B89C);
            MenuArt.label(g, Component.literal(menu.view.receipt().isEmpty() ? "Review before confirming" : menu.view.receipt()), 8, 130, 158, MenuArt.PENDING);
            g.text(font, playerInventoryTitle, 8, 141, MenuArt.TEXT);
            int y = 154;
            for (var line : font.split(Component.literal("Shared station\nItems stay here.\n\nCrafting XP:\nEnchant at a table;\nthen learn Basic\nAffix Crafting."), 112)) { g.text(font, line, 181, y, MenuArt.MUTED); y += 9; }
        }
    }
    private void row(GuiGraphicsExtractor g, String value, int y, int color) { MenuArt.label(g, Component.literal(value), 8, y, 288, color); }

    /** Paged native buttons expose full datapack lists and details without intercepting inventory clicks. */
    private final class ReviewScreen extends Screen {
        private final int kind, token;
        private final WorkbenchMenu.View snapshot;
        private int page, left, top, pages;
        private List<net.minecraft.util.FormattedCharSequence> lines = List.of();
        ReviewScreen(int kind) { super(Component.literal(kind < 0 ? "Transaction details" : "Choose " + List.of("operation", "family", "rarity", "affix").get(kind)));
            this.kind = kind; snapshot = menu.view; token = snapshot.token(); }
        private List<String> choices() { return switch (kind) { case 1 -> snapshot.families(); case 2 -> snapshot.rarities(); case 3 -> snapshot.targets(); default -> List.of(); }; }
        @Override protected void init() {
            left = (width - 296) / 2; top = (height - 212) / 2;
            lines = font.split(Component.literal(snapshot.preview()), 276);
            pages = Math.max(1, ((kind < 0 ? lines.size() : choices().size()) + (kind < 0 ? 11 : 5)) / (kind < 0 ? 12 : 6));
            page = Math.min(page, pages - 1);
            if (kind >= 0) { for (int n = 0; n < 6 && page * 6 + n < choices().size(); n++) {
                int index = page * 6 + n;
                var label = Component.literal(choices().get(index));
                var b = addRenderableWidget(Button.builder(label, ignored -> { choose(kind, index, token); onClose(); }).bounds(left + 10, top + 31 + n * 23, 276, 20).build());
                b.setTooltip(Tooltip.create(label));
            } }
            var prev = addRenderableWidget(Button.builder(Component.literal("Previous"), b -> { page--; rebuildWidgets(); }).bounds(left + 10, top + 182, 86, 20).build()); prev.active = page > 0;
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(left + 105, top + 182, 86, 20).build());
            var next = addRenderableWidget(Button.builder(Component.literal("Next"), b -> { page++; rebuildWidgets(); }).bounds(left + 200, top + 182, 86, 20).build()); next.active = page + 1 < pages;
        }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            super.extractRenderState(g, mouseX, mouseY, partialTick);
            g.text(font, title, left + 10, top + 12, MenuArt.TEXT);
            if (kind < 0) { for (int n = 0; n < 12 && page * 12 + n < lines.size(); n++) { g.text(font, lines.get(page * 12 + n), left + 10, top + 33 + n * 11, MenuArt.TEXT); } }
            g.text(font, (page + 1) + " / " + pages, left + 136, top + 170, MenuArt.MUTED);
        }
        @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) { super.extractBackground(g, mouseX, mouseY, partialTick); MenuArt.panel(g, left, top, 296, 212); }
        @Override public void tick() {
            if (minecraft.player == null || minecraft.player.containerMenu != menu) { minecraft.setScreen(null); }
            else if (menu.view.token() != token) { onClose(); }
        }
        @Override public void onClose() { minecraft.setScreen(WorkbenchScreen.this); }
        @Override public boolean isPauseScreen() { return false; }
        @Override public boolean isInGameUi() { return true; }
    }
}
