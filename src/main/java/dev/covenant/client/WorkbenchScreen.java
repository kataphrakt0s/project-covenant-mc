package dev.covenant.client;

import dev.covenant.rpg.equipment.WorkbenchMenu;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.player.Inventory;

/** Compact native-slot station screen. Costs, access and possible salvage outputs come from the server. */
public final class WorkbenchScreen extends AbstractContainerScreen<WorkbenchMenu> {
    private final List<Button> selectors = new ArrayList<>();
    private Button action;
    private WorkbenchMenu.View previous;
    public WorkbenchScreen(WorkbenchMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 304, 232); }
    @Override protected void init() {
        super.init(); selectors.clear(); previous = null;
        button("Affix Crafting", 8, 2, 140, 4); button("Salvaging", 156, 2, 140, 5);
        selectors.add(button("Operation", 56, 27, 100, 0)); selectors.add(button("Family", 164, 27, 132, 1));
        selectors.add(button("Rarity", 56, 51, 100, 2)); selectors.add(button("Target", 164, 51, 132, 3));
        action = addRenderableWidget(Button.builder(Component.literal("Craft"), b -> send(1000 + menu.view.token()))
                .bounds(leftPos + 220, topPos + 126, 76, 20).build());
        update();
    }
    private Button button(String text, int x, int y, int width, int id) {
        return addRenderableWidget(Button.builder(Component.literal(text), b -> send(id)).bounds(leftPos + x, topPos + y, width, 20).build());
    }
    private void send(int id) { if (minecraft.gameMode != null) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); } }
    private void update() {
        try (var zone = Profiler.get().zone("covenant:workbench_screen_update")) {
            var view = menu.view; if (view == previous) { return; } previous = view;
            var names = List.of(view.operation(), view.family(), view.rarity(), view.target());
            for (int index = 0; index < selectors.size(); index++) { var button = selectors.get(index);
                button.visible = view.tab() == 0 && (index == 0 || (index == 3 ? !view.operation().equals("Add") : view.operation().equals("Add")));
                button.setMessage(Component.literal(names.get(index)));
                button.setTooltip(Tooltip.create(Component.literal("Click to cycle: " + names.get(index))));
            }
            action.active = view.enabled(); action.setMessage(Component.literal(view.tab() == 0 ? "Craft" : "Salvage"));
            action.setTooltip(Tooltip.create(Component.literal(view.preview())));
        }
    }
    @Override protected void containerTick() { super.containerTick(); update(); }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        try (var zone = Profiler.get().zone("covenant:workbench_screen_background")) {
            super.extractBackground(graphics, mouseX, mouseY, a);
            graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF18202B);
            graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF8E7851);
            for (var slot : menu.slots) { if (slot.isActive()) {
                graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF0B1017);
                graphics.outline(leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, 0xFF657181);
            } }
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        try (var zone = Profiler.get().zone("covenant:workbench_screen_labels")) {
            if (menu.view.tab() == 1) {
                graphics.text(font, "To Be Salvaged", 8, 24, 0xFFE2E8ED); graphics.text(font, "Salvage Results", 112, 24, 0xFFE2E8ED);
            } else { graphics.text(font, "Gear", 17, 34, 0xFFE2E8ED); }
            int y = 80;
            for (var line : font.split(Component.literal(menu.view.preview()), 288)) {
                if (y > 117) { break; } graphics.text(font, line, 8, y, 0xFFCDD6E0); y += 9;
            }
            graphics.text(font, "Hover action for full details", 8, 129, 0xFF96A5B7);
            graphics.text(font, playerInventoryTitle, 8, 141, 0xFFE2E8ED);
        }
    }
}
