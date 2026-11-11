package dev.covenant.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/** A single batched, untextured wheel submitted through Minecraft's GUI renderer. */
record RadialMenuRenderState(Matrix3x2fc pose, int centerX, int centerY, int radius, int hovered,
                             @Nullable ScreenRectangle scissorArea, ScreenRectangle bounds)
        implements GuiElementRenderState {
    private static final int ARC_STEPS = 16;
    private static final float[][] UNIT_ARCS = createArcs();

    private static float[][] createArcs() {
        float[][] arcs = new float[RadialMenuLayout.SLOT_COUNT][(ARC_STEPS + 1) * 2];
        for (int slot = 0; slot < arcs.length; slot++) {
            for (int step = 0; step <= ARC_STEPS; step++) {
                double angle = RadialMenuLayout.angle(slot) - RadialMenuLayout.SLOT_ANGLE / 2 + 0.012
                        + (RadialMenuLayout.SLOT_ANGLE - 0.024) * step / ARC_STEPS;
                arcs[slot][step * 2] = (float) Math.cos(angle);
                arcs[slot][step * 2 + 1] = (float) Math.sin(angle);
            }
        }
        return arcs;
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        for (int slot = 0; slot < RadialMenuLayout.SLOT_COUNT; slot++) {
            int fill = slot == hovered ? (slot < 3 ? 0xF04D4358 : 0xF02B2931) : slot < 3 ? 0xF0302C38 : 0xD51C1B22;
            int edge = slot == hovered ? MenuArt.GOLD : slot < 3 ? MenuArt.EDGE : 0xFF45424B;
            float inner = radius * RadialMenuLayout.INNER_RADIUS_RATIO;
            band(vertices, slot, inner, radius, fill);
            band(vertices, slot, radius - 1, radius, edge);
            band(vertices, slot, inner, inner + 1, edge);
        }
    }

    private void band(VertexConsumer vertices, int slot, float inner, float outer, int color) {
        float[] arc = UNIT_ARCS[slot];
        for (int step = 0; step < ARC_STEPS; step++) {
            // Match vanilla GUI quad winding so the default pipeline keeps the front face.
            vertex(vertices, arc, step, inner, color);
            vertex(vertices, arc, step + 1, inner, color);
            vertex(vertices, arc, step + 1, outer, color);
            vertex(vertices, arc, step, outer, color);
        }
    }

    private void vertex(VertexConsumer vertices, float[] arc, int step, float distance, int color) {
        vertices.addVertexWith2DPose(pose, centerX + arc[step * 2] * distance,
                centerY + arc[step * 2 + 1] * distance).setColor(color);
    }
}
