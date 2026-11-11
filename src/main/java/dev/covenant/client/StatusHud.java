package dev.covenant.client;

import dev.covenant.Covenant;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.skills.BuffDisplay;
import dev.covenant.rpg.skills.ComboDisplay;
import dev.covenant.rpg.skills.WeaverDisplay;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Compact owner-only skill status. Native potion effects keep their own vanilla HUD. */
public final class StatusHud {
    private static final int CELL = 24, STEP = 26, ROW = 34, MARGIN = 5;
    private static final int PASSIVE = 0xFF93B6C8, ACTIVE = 0xFFFFD36B;
    private static final int FIRE = 0xFFED4C68, ICE = 0xFFD0E7F5, LIGHTNING = 0xFF55C5FF;
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/effect_background");
    private static final Identifier AMBIENT_BACKGROUND = Identifier.withDefaultNamespace("hud/effect_background_ambient");
    private static LocalPlayer owner;
    private static ComboDisplay comboView = ComboDisplay.EMPTY;
    private static WeaverDisplay weaverView = WeaverDisplay.EMPTY;
    private static BuffDisplay buffView = BuffDisplay.EMPTY;
    private static float shieldView;
    private static long nextRefresh;
    private static int rootSeconds, chillSeconds;
    private static boolean frozenView;
    /** Local-level deadline retained across packet ordering until Weaver confirms the buff ended. */
    private static long threefoldDeadline;
    private static List<List<Tile>> groups = List.of();

    /** Same original identity as the skill tree and assignment picker. */
    private enum Icon {
        LUNGE("lunging_combo"), RUSH("rip_and_rush"), DISCHARGE("discharge"), THREEFOLD("threefold_system"), GOLEM("elemental_golem"),
        FIRE("fire_bolt"), ICE("ice_bolt"), LIGHTNING("lightning_bolt"), DODGE("dodge_aura"), SPEED("swiftness_aura"),
        RIPPING("ripping_aura"), FORTITUDE("fortitude_aura"), SHIELD("shield", true), RAGE("raging_aura"),
        ICY_VEINS("icy_veins"), FIRE_CLOAK("flame_cloak"), FREEZING("freezing_aura"), FROZEN("frozen", true),
        SHOCK("shock_aura"), IMMOLATION("immolation_aura"), ENCHANTING("enchanting_aura"),
        BATTLE_CRY("battle_cry"), BLOOD_FEVER("blood_fever"), ROOTED("rooted", true), CHILLED("chilled", true), UNKNOWN("unknown", true);
        final Identifier id;
        Icon(String path) { this(path, false); }
        Icon(String path, boolean ui) { id = ui ? SkillIcons.ui(path) : Identifier.fromNamespaceAndPath("covenant", path); }
    }

    private record Tile(Icon icon, String badge, String footer, int color, boolean active, List<Component> tooltip) {}

    private StatusHud() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EFFECTS, Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "skill_status"),
                (graphics, delta) -> render(graphics));
    }

    private static void render(GuiGraphicsExtractor graphics) {
        try (var zone = Profiler.get().zone("covenant:status_hud")) {
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;
            if (player == null || minecraft.level == null || !player.isAlive() || player.isSpectator()) {
                owner = null; groups = List.of(); return;
            }
            if (minecraft.options.hideGui) { return; }
            var combos = player.getData(RpgData.COMBO_DISPLAY);
            var weaver = player.getData(RpgData.WEAVER_DISPLAY);
            var buffs = player.getData(RpgData.BUFF_DISPLAY);
            float shield = player.getData(RpgData.FORTITUDE_CAPACITY);
            long tick = minecraft.level.getGameTime();
            int root = controlSeconds(player, dev.covenant.rpg.skills.Rooted.EFFECT);
            int chill = controlSeconds(player, dev.covenant.rpg.skills.Chilled.EFFECT);
            boolean frozen = player.getData(RpgData.AURA_FROZEN);
            if (owner != player || !combos.equals(comboView) || !weaver.equals(weaverView)
                    || !buffs.equals(buffView) || shield != shieldView || tick >= nextRefresh
                    || root != rootSeconds || chill != chillSeconds || frozen != frozenView) {
                if (owner != player) { threefoldDeadline = 0; }
                owner = player; comboView = combos; weaverView = weaver; buffView = buffs; shieldView = shield;
                rootSeconds = root; chillSeconds = chill; frozenView = frozen;
                rebuild(tick);
            }
            int columns = Math.clamp((graphics.guiWidth() / 2 - MARGIN * 2) / STEP, 1, 8);
            int y = MARGIN;
            int mouseX = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
            int mouseY = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
            for (var group : groups) {
                int index = 0;
                for (var tile : group) {
                    int x = MARGIN + index % columns * STEP;
                    int tileY = y + index / columns * ROW;
                    draw(graphics, tile, x, tileY);
                    if (minecraft.screen != null && mouseX >= x && mouseX < x + CELL
                            && mouseY >= tileY && mouseY < tileY + ROW - 2) {
                        graphics.setComponentTooltipForNextFrame(minecraft.font, tile.tooltip, mouseX, mouseY);
                    }
                    index++;
                }
                y += ((group.size() + columns - 1) / columns) * ROW + 2;
            }
        }
    }

    private static void draw(GuiGraphicsExtractor graphics, Tile tile, int x, int y) {
        var font = Minecraft.getInstance().font;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, tile.active ? BACKGROUND : AMBIENT_BACKGROUND, x, y, CELL, CELL);
        SkillIcons.draw(graphics, tile.icon.id, x + 4, y + 3, 16, -1);
        graphics.fill(x + 2, y + CELL - 2, x + CELL - 2, y + CELL - 1, tile.color);
        // Solid diamond = active; hollow square = passive/reserve, independent of palette.
        if (tile.active) {
            graphics.fill(x + 1, y + 2, x + 4, y + 3, tile.color);
            graphics.fill(x + 2, y + 1, x + 3, y + 4, tile.color);
        } else { graphics.outline(x + 1, y + 1, 3, 3, tile.color); }
        if (!tile.badge.isEmpty()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + CELL - 2, y + 13);
            graphics.pose().scale(.75f, .75f);
            graphics.text(font, tile.badge, -font.width(tile.badge), 0, 0xFFFFFFFF);
            graphics.pose().popMatrix();
        }
        if (!tile.footer.isEmpty()) {
            float scale = Math.min(.8f, (CELL - 1f) / Math.max(1, font.width(tile.footer)));
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + CELL / 2f, y + CELL + 1);
            graphics.pose().scale(scale, scale);
            graphics.centeredText(font, tile.footer, 0, 0, tile.color);
            graphics.pose().popMatrix();
        }
    }

    /** Rebuild on state changes and displayed timer boundaries, never once per rendered frame. */
    private static void rebuild(long tick) {
        var next = new ArrayList<List<Tile>>(4);
        nextRefresh = tick + 20 - Math.floorMod(tick, 20);
        var controls = new ArrayList<Tile>(3);
        if (rootSeconds != 0) { controls.add(control(Icon.ROOTED, "rooted", rootSeconds)); }
        if (chillSeconds != 0) { controls.add(control(Icon.CHILLED, "chilled", chillSeconds)); }
        if (frozenView && buffView.rows().stream().noneMatch(row -> row.skill().getPath().equals("discharge_freeze"))) {
            controls.add(control(Icon.FROZEN, "frozen", -1));
        }
        if (!controls.isEmpty()) { next.add(List.copyOf(controls)); }
        var combos = new ArrayList<Tile>(2);
        long comboNow = tick + comboView.clockOffset();
        for (var row : comboView.rows()) {
            long remaining = row.deadline() - comboNow;
            if (!row.moving() && remaining <= 0) { continue; }
            String skill = row.combo() == 0 ? "lunging_combo" : "rip_and_rush";
            String time = row.moving() ? "" : String.format(Locale.ROOT, "%.1fs", remaining / 20.0);
            var action = Component.translatable("combo.covenant." + skill + "." + row.stage());
            combos.add(new Tile(row.combo() == 0 ? Icon.LUNGE : Icon.RUSH, Integer.toString(row.stage() + 1), time, ACTIVE, true,
                    List.of(skillName(skill), Component.translatable(row.moving() ? "hud.covenant.status.executing" : "hud.covenant.status.next", action))));
            if (!row.moving()) {
                nextRefresh = Math.min(nextRefresh, Math.min(tick + 2, row.deadline() - comboView.clockOffset()));
            }
        }
        if (!combos.isEmpty()) { next.add(List.copyOf(combos)); }

        if (weaverView.discharge()) {
            next.add(List.of(
                    new Tile(Icon.DISCHARGE, "", "", PASSIVE, false, List.of(skillName("discharge"), Component.translatable("hud.covenant.status.discharge"))),
                    charge(Icon.FIRE, "fire", weaverView.fire(), FIRE), charge(Icon.ICE, "ice", weaverView.ice(), ICE),
                    charge(Icon.LIGHTNING, "lightning", weaverView.lightning(), LIGHTNING)));
        }
        long buffNow = tick + buffView.clockOffset();
        if (!weaverView.threefold() || !weaverView.buff()) { threefoldDeadline = 0; }
        if (weaverView.threefold()) {
            String duration = "";
            for (var row : buffView.rows()) {
                if (row.skill().getPath().equals("threefold_system") && row.expiresAt() > 0) {
                    threefoldDeadline = row.expiresAt() - buffView.clockOffset();
                }
            }
            boolean expired = weaverView.buff() && threefoldDeadline > 0 && threefoldDeadline <= tick;
            boolean active = weaverView.buff() && !expired;
            if (active && threefoldDeadline > tick) {
                duration = seconds(threefoldDeadline - tick);
                nextRefresh = Math.min(nextRefresh, tick + Math.floorMod(threefoldDeadline - tick - 1, 20) + 1);
            }
            next.add(List.of(
                    new Tile(Icon.THREEFOLD, "", duration, active ? ACTIVE : PASSIVE, active,
                            List.of(skillName("threefold_system"), Component.translatable(active
                                    ? "hud.covenant.status.threefold.active" : "hud.covenant.status.threefold.reserve"))),
                    power(Icon.FIRE, "fire", expired ? 0 : weaverView.firePower(), FIRE, active),
                    power(Icon.ICE, "ice", expired ? 0 : weaverView.icePower(), ICE, active),
                    power(Icon.LIGHTNING, "lightning", expired ? 0 : weaverView.lightningPower(), LIGHTNING, active)));
        }
        var buffs = new ArrayList<Tile>();
        for (var row : buffView.rows()) {
            String skill = row.skill().getPath();
            if (skill.equals("threefold_system") && weaverView.threefold()) { continue; }
            boolean active = row.expiresAt() > buffNow;
            if (!active && !row.passive() && row.stacks() == 0) { continue; }
            String footer = active ? seconds(row.expiresAt() - buffNow) : "";
            String badge = row.stacks() > 0 ? Integer.toString(row.stacks()) : "";
            var tooltip = new ArrayList<Component>(4);
            if (skill.startsWith("elemental_golem_")) {
                tooltip.add(skillName("elemental_golem"));
                tooltip.add(Component.translatable("entity.covenant.elemental_golem.form."+skill.substring("elemental_golem_".length())));
                tooltip.add(Component.translatable("skill.covenant."+skill+".description"));
                buffs.add(new Tile(Icon.GOLEM,"","",PASSIVE,false,List.copyOf(tooltip)));
                continue;
            }
            tooltip.add(skill.equals("discharge_freeze") ? Component.translatable("hud.covenant.status.discharge_freeze") : skillName(skill));
            if (row.passive()) { tooltip.add(Component.translatable("hud.covenant.status.passive")); }
            if (active) {
                tooltip.add(Component.translatable("hud.covenant.status.remaining", footer)); expireAt(row.expiresAt(), tick);
            }
            if (row.stacks() > 0) { tooltip.add(Component.translatable(skill.equals("blood_fever")
                    ? "hud.covenant.status.bleeding_enemies" : "hud.covenant.status.stacks", row.stacks())); }
            boolean shield = skill.equals("fortitude_aura") && shieldView > 0 && active;
            if (shield) {
                badge = String.format(Locale.ROOT, "%.1f", shieldView);
                tooltip.add(Component.translatable("hud.covenant.fortitude", badge));
            }
            if (skill.equals("rip_and_rush")) { tooltip.add(Component.translatable("hud.covenant.status.rush_speed")); }
            buffs.add(new Tile(icon(skill, shield), badge, footer,
                    skill.equals("blood_fever") ? FIRE : active ? ACTIVE : PASSIVE, active, List.copyOf(tooltip)));
        }
        if (!buffs.isEmpty()) { next.add(List.copyOf(buffs)); }
        groups = List.copyOf(next);
    }

    private static int controlSeconds(LocalPlayer player, Holder<MobEffect> type) {
        var effect = player.getEffect(type);
        // Do not duplicate a native potion tile if a datapack/mod elects to show it there.
        return effect == null || effect.showIcon() ? 0 : effect.isInfiniteDuration() ? -1 : (effect.getDuration() + 19) / 20;
    }

    private static Tile control(Icon icon, String name, int remaining) {
        return new Tile(icon, "", remaining > 0 ? remaining + "s" : "", ICE, true,
                List.of(Component.translatable("ui.covenant.control." + name), Component.translatable("ui.covenant.control." + name + ".description")));
    }

    private static Tile charge(Icon icon, String element, int count, int color) {
        return new Tile(icon, Integer.toString(count), "", color, count > 0, List.of(skillName("discharge"),
                Component.translatable("hud.covenant.status." + element),
                Component.translatable("hud.covenant.status.charges", count, weaverView.maxCharges())));
    }

    private static Tile power(Icon icon, String element, int value, int color, boolean active) {
        String amount = (active ? "+" : "") + value + "%";
        return new Tile(icon, "", amount, color, active, List.of(skillName("threefold_system"),
                Component.translatable("hud.covenant.status." + element), Component.translatable(active
                        ? "hud.covenant.status.bonus" : "hud.covenant.status.stored", value)));
    }

    private static void expireAt(long deadline, long tick) {
        long localDeadline = deadline - buffView.clockOffset();
        if (localDeadline > tick) {
            long remaining = localDeadline - tick;
            nextRefresh = Math.min(nextRefresh, tick + Math.floorMod(remaining - 1, 20) + 1);
        }
    }

    private static String seconds(long ticks) { return ((ticks + 19) / 20) + "s"; }
    private static Component skillName(String path) { return Component.translatable("skill.covenant." + path); }

    private static Icon icon(String skill, boolean shield) {
        return switch (skill) {
            case "dodge_aura" -> Icon.DODGE;
            case "swiftness_aura", "rip_and_rush" -> Icon.SPEED;
            case "ripping_aura" -> Icon.RIPPING;
            case "fortitude_aura" -> shield ? Icon.SHIELD : Icon.FORTITUDE;
            case "raging_aura" -> Icon.RAGE;
            case "icy_veins" -> Icon.ICY_VEINS;
            case "flame_cloak" -> Icon.FIRE_CLOAK;
            case "freezing_aura" -> Icon.FREEZING;
            case "discharge_freeze" -> Icon.FROZEN;
            case "shock_aura" -> Icon.SHOCK;
            case "immolation_aura" -> Icon.IMMOLATION;
            case "enchanting_aura" -> Icon.ENCHANTING;
            case "battle_cry" -> Icon.BATTLE_CRY;
            case "blood_fever" -> Icon.BLOOD_FEVER;
            case "threefold_system" -> Icon.THREEFOLD;
            default -> Icon.UNKNOWN;
        };
    }
}
