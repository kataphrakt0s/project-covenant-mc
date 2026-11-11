package dev.covenant.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Shared dark iron, crimson inlay and violet selection treatment; widgets retain native input and narration. */
final class MenuArt {
    static final int TEXT = 0xFFEAE7EF, MUTED = 0xFFB2AEBB, EDGE = 0xFF786780;
    static final int GOLD = 0xFFD1AFDF, PENDING = 0xFFBFD8ED;
    private MenuArt() {}

    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFA1D1825);
        g.outline(x, y, w, h, 0xFF111117);
        g.outline(x + 1, y + 1, w - 2, h - 2, EDGE);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, 0xFF79233E);
        g.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, 0xFF392449);
        rivet(g, x + 4, y + 5); rivet(g, x + w - 6, y + 5);
        rivet(g, x + 4, y + h - 7); rivet(g, x + w - 6, y + h - 7);
    }

    private static void rivet(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 2, y + 2, 0xFFAEA6B8);
        g.fill(x, y, x + 1, y + 1, 0xFFE3DED7);
    }

    static void badge(GuiGraphicsExtractor g, String label, int right, int y, int color) {
        var font = Minecraft.getInstance().font;
        int w = font.width(label) + 2;
        g.fill(right - w, y, right, y + 9, 0xF0101017);
        g.text(font, label, right - w + 1, y, color);
    }

    /** Clip labels to their own click area; full text stays in narration and tooltips. */
    static void label(GuiGraphicsExtractor g, Component label, int x, int y, int width, int color) {
        if (width < 6) { return; }
        var font = Minecraft.getInstance().font;
        String value = label.getString();
        if (font.width(value) > width) { value = font.plainSubstrByWidth(value, Math.max(0, width - font.width("…"))) + "…"; }
        g.text(font, value, x, y, color);
    }

    static final class IconButton extends Button {
        private final Identifier icon;
        IconButton(int x, int y, int w, int h, Component label, Identifier icon, OnPress press) {
            super(x, y, w, h, label, press, DEFAULT_NARRATION);
            this.icon = icon;
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
            g.fill(getX(), getY(), getRight(), getBottom(), active ? 0xFF35243D : 0xFF25212C);
            g.outline(getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused() ? GOLD : EDGE);
            int size = Math.min(16, getHeight() - 4);
            SkillIcons.draw(g, icon, getX() + 3, getY() + (getHeight() - size) / 2, size, active ? 0xFFFFFFFF : 0xFFAAA4B2);
            label(g, getMessage(), getX() + size + 7, getY() + (getHeight() - 8) / 2,
                    getWidth() - size - 11, active ? TEXT : MUTED);
        }
    }
}
