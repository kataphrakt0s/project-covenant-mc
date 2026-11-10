package dev.covenant.client;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.Auras;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLoadout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** One quiet owner-only footprint for all assigned area auras, independent of decorative particles. */
public final class AuraRangeVisuals {
    private static final int SEGMENTS = 64;
    private static final ContextKey<Snapshot> RANGE = new ContextKey<>(Identifier.fromNamespaceAndPath("covenant", "aura_range"));
    private static final Auras.Kind[] KINDS = Auras.Kind.values();
    private static final float[] EMPTY = new float[0];
    private static LocalPlayer owner;
    private static ClientLevel viewLevel;
    private static SkillLoadout loadout;
    private static PlayerClass playerClass;
    private static double radius;
    private static Vec3 checkedPosition;
    private static int checkedTick;
    // Replaced after terrain sampling; queued geometry never reads a buffer being rewritten.
    private static float[] ends = EMPTY;

    private record Snapshot(float[] ends, Vec3 center) {}

    private AuraRangeVisuals() {}

    /** Only the local owner is visited. Moving fields refresh each tick, stationary fields every five. */
    public static void tick(ClientTickEvent.Post event) {
        try (var zone = Profiler.get().zone("covenant:aura_range_visuals")) {
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;
            if (player == null || minecraft.level == null || !player.isAlive() || player.isSpectator()) {
                clear(); return;
            }
            if (owner != player || viewLevel != minecraft.level) { clear(); owner = player; viewLevel = minecraft.level; }
            var nextLoadout = player.getData(RpgData.SKILL_LOADOUT);
            var nextClass = PlayerProfiles.getSelectedClass(player).orElse(null);
            if (loadout != nextLoadout || playerClass != nextClass) {
                loadout = nextLoadout; playerClass = nextClass; radius = 0; checkedPosition = null;
                for (var kind : KINDS) {
                    double candidate = kind.passiveRadius();
                    if (playerClass != null && candidate > 0 && loadout.learned().contains(kind.id)
                            && loadout.bindings().containsValue(kind.id)
                            && SkillDefinition.find(kind.id).orElseThrow().allowedClasses().contains(playerClass)) {
                        radius = Math.max(radius, candidate);
                    }
                }
            }
            if (radius == 0) { ends = EMPTY; return; }
            if (minecraft.isPaused()) { return; }
            var position = player.position();
            if (position.equals(checkedPosition) && player.tickCount - checkedTick < 5) { return; }
            checkedPosition = position; checkedTick = player.tickCount;
            var next = new float[SEGMENTS * 4];
            int count = 0;
            for (int i = 0; i < SEGMENTS; i++) {
                // Eight quiet breaks distinguish a passive field from a triggered trap patch.
                if (i % 8 == 7) { continue; }
                var a = point(i);
                var b = point(i + 1);
                if (!GroundMarks.supported(player, a, false) || !GroundMarks.supported(player, b, false)
                        || !GroundMarks.supported(player, a.add(b).scale(.5), false)) { continue; }
                next[count++] = (float) a.x; next[count++] = (float) a.z;
                next[count++] = (float) b.x; next[count++] = (float) b.z;
            }
            ends = java.util.Arrays.copyOf(next, count);
        }
    }

    private static Vec3 point(int index) {
        double angle = Math.PI * 2 * index / SEGMENTS;
        return new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    static void extract(ExtractLevelRenderStateEvent event) {
        if (owner == null || owner != Minecraft.getInstance().player || viewLevel != event.getLevel()
                || !owner.isAlive() || owner.isSpectator() || ends.length == 0) { return; }
        float partial = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        event.getRenderState().setRenderData(RANGE, new Snapshot(ends, owner.getPosition(partial)));
    }

    static void submit(SubmitCustomGeometryEvent event) {
        var range = event.getLevelRenderState().getRenderData(RANGE);
        if (range == null) { return; }
        var camera = event.getLevelRenderState().cameraRenderState.pos;
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(range.center.x - camera.x, range.center.y - camera.y, range.center.z - camera.z);
        event.getSubmitNodeCollector().submitCustomGeometry(pose, RenderTypes.debugQuads(), (matrix, buffer) -> {
            var points = range.ends;
            for (int i = 0; i < points.length; i += 4) {
                GroundMarks.strip(matrix, buffer, points[i], points[i + 1], points[i + 2], points[i + 3], .035, .025f, 0x90252C38);
                GroundMarks.strip(matrix, buffer, points[i], points[i + 1], points[i + 2], points[i + 3], .014, .028f, 0xA0BACDD8);
            }
        });
        pose.popPose();
    }

    private static void clear() {
        owner = null; viewLevel = null; loadout = null; playerClass = null; radius = 0; checkedPosition = null; ends = EMPTY;
    }
}
