package dev.covenant.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.covenant.rpg.skills.TrapEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.phys.Vec3;

/** Bounded same-elevation footprint marks. Never projects onto a lower floor or disables depth testing. */
final class TrapBoundary {
    private static final int SEGMENTS = 64;
    private final float[] ends = new float[SEGMENTS * 4];
    private int count;
    private int checkedTick = Integer.MIN_VALUE;
    private TrapEntity.Phase phase;
    private int edgeColor, backingColor;

    void update(TrapEntity trap) {
        if (trap.phase() == TrapEntity.Phase.FLIGHT
                || trap.phase() == TrapEntity.Phase.REMOVED) { count = 0; phase = null; return; }
        if (phase == trap.phase() && trap.tickCount - checkedTick < 5) { return; }
        try (var zone = Profiler.get().zone("covenant:trap_boundary")) {
            phase = trap.phase(); checkedTick = trap.tickCount; count = 0;
            edgeColor = switch (trap.kind()) {
                case BEAR -> 0xE6CFD3CC;
                case FROST -> 0xE6C9F0F0;
                case FLAME -> 0xE6ED4C68;
                case BLAST -> 0xE6D9BC8D;
            };
            backingColor = trap.kind() == dev.covenant.rpg.skills.TrapKind.FROST ? 0xD0263D50 : 0xD02C2928;
            boolean patch = phase == TrapEntity.Phase.TRIGGERED;
            for (int i = 0; i < SEGMENTS; i++) {
                // Incomplete corners during arming; only the armed device promises a full trigger square.
                if (phase == TrapEntity.Phase.ARMING && i % 16 >= 4 && i % 16 < 12) { continue; }
                var a = point(i, patch, trap.kind().radius);
                var b = point(i + 1, patch, trap.kind().radius);
                var mid = a.add(b).scale(.5);
                if (!GroundMarks.supported(trap, a, true) || !GroundMarks.supported(trap, b, true)
                        || !GroundMarks.supported(trap, mid, true)) { continue; }
                int offset = count++ * 4;
                ends[offset] = (float) a.x; ends[offset + 1] = (float) a.z;
                ends[offset + 2] = (float) b.x; ends[offset + 3] = (float) b.z;
            }
        }
    }

    private static Vec3 point(int index, boolean patch, double radius) {
        if (patch) {
            double angle = Math.PI * 2 * index / SEGMENTS;
            return new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
        }
        double r = TrapEntity.CONTACT_RADIUS;
        double along = -r + (index % 16) * (2 * r / 16);
        return switch ((index / 16) % 4) {
            case 0 -> new Vec3(along, 0, -r);
            case 1 -> new Vec3(r, 0, along);
            case 2 -> new Vec3(-along, 0, r);
            default -> new Vec3(-r, 0, -along);
        };
    }

    void submit(PoseStack pose, SubmitNodeCollector collector, boolean arming) {
        if (count == 0) { return; }
        collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (matrix, buffer) -> {
            for (int i = 0; i < count; i++) {
                int offset = i * 4;
                float ax = ends[offset], az = ends[offset + 1], bx = ends[offset + 2], bz = ends[offset + 3];
                // Dark backing keeps the cold edge visible on snow as well as dark stone.
                GroundMarks.strip(matrix, buffer, ax, az, bx, bz, .045, .025f, backingColor);
                GroundMarks.strip(matrix, buffer, ax, az, bx, bz, .018, .028f,
                        arming ? (edgeColor & 0xFFFFFF) | 0x90000000 : edgeColor);
            }
        });
    }

}
