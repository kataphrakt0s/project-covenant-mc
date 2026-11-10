package dev.covenant.client;

import dev.covenant.rpg.skills.SkillDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Incremental original skill art; callers retain their text fallback for unauthored skills. */
final class SkillIcons {
    private static final java.util.Map<Identifier, Identifier> ICONS = java.util.Map.of(
            SkillDefinition.BEAR_TRAP.id(), texture("bear_trap"),
            SkillDefinition.FROST_TRAP.id(), texture("frost_trap"),
            SkillDefinition.FLAME_TRAP.id(), texture("flame_trap"),
            SkillDefinition.BLAST_TRAP.id(), texture("blast_trap"));

    private SkillIcons() {}

    static boolean draw(GuiGraphicsExtractor graphics, Identifier skill, int x, int y, int size, int tint) {
        var icon = ICONS.get(skill);
        if (size < 8 || icon == null) { return false; }
        // Keep whole source pixels at native size where space allows; small nodes use their available square.
        int display = Math.min(16, size);
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, x + (size - display) / 2, y + (size - display) / 2,
                0, 0, display, display, 16, 16, 16, 16, tint);
        return true;
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("covenant", "textures/gui/skills/" + name + ".png");
    }
}
