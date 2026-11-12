package dev.covenant.client;

import dev.covenant.rpg.skills.SkillDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Original native-pixel icons shared by trees, assignments, navigation and owner status. */
final class SkillIcons {
    private static final java.util.Map<Identifier, Identifier> ICONS = create();

    private SkillIcons() {}

    private static java.util.Map<Identifier, Identifier> create() {
        var icons = new java.util.HashMap<Identifier, Identifier>();
        for (var skill : SkillDefinition.ALL) { icons.put(skill.id(), texture("skills/" + skill.id().getPath())); }
        for (String name : new String[] {"stalker", "reaver", "weaver", "skills", "stats", "professions",
                "mining", "logging", "farming", "crafting", "shield", "frozen", "unknown"}) {
            icons.put(ui(name), texture("emblems/" + name));
        }
        for (String name : new String[] {"vein_mining", "tree_felling", "offensive_mastery", "scything",
                "auto_replant", "basic", "salvaging", "recovery", "focused"}) {
            icons.put(ui(name), texture("emblems/" + name));
        }
        for (String name : new String[] {"rooted", "chilled"}) {
            icons.put(ui(name), Identifier.fromNamespaceAndPath("covenant", "textures/mob_effect/" + name + ".png"));
        }
        icons.put(ui("building"), texture("emblems/professions"));
        icons.put(ui("fishing"), texture("emblems/professions"));
        icons.put(ui("exploration"), texture("emblems/professions"));
        return java.util.Map.copyOf(icons);
    }

    static Identifier ui(String name) { return Identifier.fromNamespaceAndPath("covenant", "ui/" + name); }

    static boolean draw(GuiGraphicsExtractor graphics, Identifier skill, int x, int y, int size, int tint) {
        var icon = ICONS.get(skill);
        if (size < 8 || icon == null) { return false; }
        // Keep whole source pixels at native size where space allows; small nodes use their available square.
        int display = Math.min(16, size);
        // Scale the full source into the destination; never sample only its upper-left pixels.
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, x + (size - display) / 2, y + (size - display) / 2,
                0, 0, display, display, 16, 16, 16, 16, tint);
        return true;
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("covenant", "textures/gui/" + name + ".png");
    }
}
