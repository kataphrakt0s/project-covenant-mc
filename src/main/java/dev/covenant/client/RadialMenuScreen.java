package dev.covenant.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.profiling.Profiler;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

/** Hold-to-open client UI. Skills, Stats, and Professions have separate stable slots. */
final class RadialMenuScreen extends Screen {
    private static final Component PLACEHOLDER = Component.translatable("screen.covenant.radial.placeholder");
    private final Component[] options = new Component[RadialMenuLayout.SLOT_COUNT];
    private final Component releaseHint;
    private final boolean mouseBinding;
    private final int activationCode;

    RadialMenuScreen(KeyMapping binding, boolean mouseBinding, int activationCode) {
        super(Component.translatable("screen.covenant.radial.title"));
        this.mouseBinding = mouseBinding;
        this.activationCode = activationCode;
        releaseHint = Component.translatable("screen.covenant.radial.release", binding.getTranslatedKeyMessage());
        for (int slot = 0; slot < options.length; slot++) {
            options[slot] = Component.translatable("screen.covenant.radial.option", slot + 1);
        }
        options[0] = Component.translatable("screen.covenant.skills.title");
        options[1] = Component.translatable("screen.covenant.stats.title");
        options[2] = Component.translatable("screen.covenant.professions.title");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public void tick() {
        try (var zone = Profiler.get().zone("covenant:radial_menu_tick")) {
            // Opening a Screen clears KeyMapping state, so poll the physical input only while open.
            boolean held = mouseBinding
                    ? GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), activationCode) == GLFW.GLFW_PRESS
                    : activationCode != GLFW.GLFW_KEY_UNKNOWN
                        && InputConstants.isKeyDown(minecraft.getWindow(), activationCode);
            if (!minecraft.isWindowActive() || minecraft.level == null
                    || minecraft.player == null || !minecraft.player.isAlive()) {
                onClose();
            } else if (!held) {
                release(minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()),
                        minecraft.mouseHandler.getScaledYPos(minecraft.getWindow()));
            }
        }
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (!mouseBinding && event.key() == activationCode) {
            release(minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()),
                    minecraft.mouseHandler.getScaledYPos(minecraft.getWindow()));
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (mouseBinding && event.button() == activationCode) {
            release(event.x(), event.y());
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            openSlot(hovered(event.x(), event.y()));
        }
        return true;
    }

    private int hovered(double x, double y) {
        return RadialMenuLayout.hoveredSlot(x - width / 2, y - height / 2, RadialMenuLayout.radius(width, height));
    }

    private void release(double mouseX, double mouseY) {
        if (!openSlot(hovered(mouseX, mouseY))) { onClose(); }
    }

    private boolean openSlot(int slot) {
        switch (slot) {
            case 0 -> minecraft.setScreen(new SkillsScreen());
            case 1 -> minecraft.setScreen(new StatsScreen());
            case 2 -> minecraft.setScreen(new ProfessionsScreen());
            default -> { return false; }
        }
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x65080D14);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        try (var zone = Profiler.get().zone("covenant:radial_menu")) {
            int centerX = width / 2;
            int centerY = height / 2;
            int radius = RadialMenuLayout.radius(width, height);
            int hovered = RadialMenuLayout.hoveredSlot(mouseX - centerX, mouseY - centerY, radius);
            Matrix3x2f pose = new Matrix3x2f(graphics.pose());
            ScreenRectangle bounds = new ScreenRectangle(centerX - radius, centerY - radius,
                    radius * 2, radius * 2).transformMaxBounds(pose);
            graphics.submitGuiElementRenderState(new RadialMenuRenderState(pose, centerX, centerY, radius,
                    hovered, graphics.peekScissorStack(), bounds));

            for (int slot = 0; slot < options.length; slot++) {
                double angle = RadialMenuLayout.angle(slot);
                int x = centerX + (int) Math.round(Math.cos(angle) * radius * 0.72);
                int y = centerY + (int) Math.round(Math.sin(angle) * radius * 0.72);
                graphics.centeredText(font, Integer.toString(slot + 1), x, y - font.lineHeight / 2,
                        slot == hovered ? 0xFFFFD084 : 0xFFE2E8ED);
            }
            graphics.centeredText(font, hovered < 0 ? title : options[hovered], centerX, centerY - 10, 0xFFF5DBA6);
            graphics.centeredText(font, hovered >= 0 && hovered <= 2 ? Component.translatable("screen.covenant.radial.open") : PLACEHOLDER,
                    centerX, centerY + 4, 0xFFABB6C2);
            graphics.centeredText(font, releaseHint, centerX, centerY + radius + 10, 0xFFE2E8ED);
        }
    }
}
