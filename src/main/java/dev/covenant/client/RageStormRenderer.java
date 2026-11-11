package dev.covenant.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.covenant.rpg.skills.RageStorm;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;

/** Thin, depth-tested hollow bands survive Minimal particles and follow the native tracked storm. */
final class RageStormRenderer extends EntityRenderer<RageStorm, RageStormRenderer.State> {
    RageStormRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public State createRenderState() { return new State(); }
    @Override protected AABB getBoundingBoxForCulling(RageStorm storm) {
        return storm.getBoundingBox().inflate(storm.visualRadius());
    }
    @Override public void extractRenderState(RageStorm storm, State state, float partialTick) {
        super.extractRenderState(storm, state, partialTick);
        state.radius = storm.isRemoved() ? 0 : storm.visualRadius();
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float radius = state.radius, phase = state.ageInTicks * .27f;
        if (radius > 0) {
            collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (matrix, buffer) ->
                    RageStormShape.emit(radius, phase, (x1,y1,z1,x2,y2,z2,x3,y3,z3,x4,y4,z4,steel) -> {
                        int color = steel ? 0xA0CFDCE8 : 0xB8E53354;
                        buffer.addVertex(matrix, x1, y1, z1).setColor(color);
                        buffer.addVertex(matrix, x2, y2, z2).setColor(color);
                        buffer.addVertex(matrix, x3, y3, z3).setColor(color);
                        buffer.addVertex(matrix, x4, y4, z4).setColor(color);
                    }));
        }
        super.submit(state, pose, collector, camera);
    }
    static final class State extends EntityRenderState { float radius; }
}
