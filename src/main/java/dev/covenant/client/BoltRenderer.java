package dev.covenant.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Thin energy filaments keep the moving head readable when decorative particles are culled. */
final class BoltRenderer<T extends Entity> extends EntityRenderer<T, BoltRenderer.State> {
    enum Kind { ARCANE, FIRE, ICE }
    private final Kind kind;

    BoltRenderer(EntityRendererProvider.Context context, Kind kind) {
        super(context);
        this.kind = kind;
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(T bolt, State state, float partialTick) {
        super.extractRenderState(bolt, state, partialTick);
        var motion = bolt.getDeltaMovement();
        state.dx = (float) motion.x; state.dy = (float) motion.y; state.dz = (float) motion.z;
    }

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var forward = new Vector3f(state.dx, state.dy, state.dz);
        if (forward.lengthSquared() < .000001f) { forward.set(0, 0, 1); }
        else { forward.normalize(); }
        pose.pushPose();
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), forward)
                .rotateZ(state.ageInTicks * .35f));
        int color = switch (kind) {
            case FIRE -> 0xD0FF5270;
            case ICE -> 0xC0A4E9FF;
            case ARCANE -> 0xC0B994FF;
        };
        float width = .035f + .008f * (float) Math.sin(state.ageInTicks * 2.4f);
        // Capture scalars, not mutable render state, for deferred geometry submission.
        collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (matrix, buffer) -> {
            filament(matrix, buffer, width, -.52f, .08f, color);
            filament(matrix, buffer, width * .45f, -.32f, .09f, 0xE8EFFAFF);
        });
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    private static void filament(PoseStack.Pose pose, VertexConsumer buffer, float width,
                                 float tail, float tip, int color) {
        // The native pipeline disables culling. Crossed ribbons plus a tiny end cap stay visible end-on.
        for (int plane = 0; plane < 2; plane++) {
            float x = plane == 0 ? width : 0, y = plane == 1 ? width : 0;
            buffer.addVertex(pose, 0, 0, tail).setColor(color & 0xFFFFFF);
            buffer.addVertex(pose, x, y, -.08f).setColor(color);
            buffer.addVertex(pose, 0, 0, tip).setColor(color);
            buffer.addVertex(pose, -x, -y, -.08f).setColor(color);
        }
        buffer.addVertex(pose, -width, 0, 0).setColor(color);
        buffer.addVertex(pose, 0, width, 0).setColor(color);
        buffer.addVertex(pose, width, 0, 0).setColor(color);
        buffer.addVertex(pose, 0, -width, 0).setColor(color);
    }

    static final class State extends EntityRenderState {
        float dx, dy, dz;
    }
}
