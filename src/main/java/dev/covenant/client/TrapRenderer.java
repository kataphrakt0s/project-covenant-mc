package dev.covenant.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.covenant.rpg.skills.TrapEntity;
import dev.covenant.rpg.skills.TrapKind;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Native block models sit on the collision surface; no camera-facing billboard or gameplay geometry. */
final class TrapRenderer extends EntityRenderer<TrapEntity, TrapRenderer.State> {
    private static final BlockDisplayContext BLOCK_CONTEXT = BlockDisplayContext.create();
    /** Model occupies the existing contact square; vertical scale keeps the deployment unobtrusive. */
    private static final float WIDTH = (float) (TrapEntity.CONTACT_RADIUS * 2);
    private static final float HEIGHT = (float) TrapEntity.CONTACT_HEIGHT;
    private final BlockModelResolver models;

    TrapRenderer(EntityRendererProvider.Context context) {
        super(context);
        models = context.getBlockModelResolver();
    }

    @Override public State createRenderState() { return new State(); }

    @Override protected AABB getBoundingBoxForCulling(TrapEntity trap) {
        return trap.getBoundingBox().inflate(TrapEntity.CONTACT_RADIUS);
    }

    @Override public void extractRenderState(TrapEntity trap, State state, float partialTick) {
        super.extractRenderState(trap, state, partialTick);
        state.flying = trap.phase() == TrapEntity.Phase.FLIGHT;
        state.visible = trap.phase() != TrapEntity.Phase.REMOVED && trap.phase() != TrapEntity.Phase.TRIGGERED;
        state.bear = trap.kind() == TrapKind.BEAR;
        if (!state.visible) { state.model.clear(); return; }
        var block = switch (trap.kind()) {
            case BEAR -> Blocks.IRON_TRAPDOOR;
            case FROST -> Blocks.BLUE_ICE;
            case FLAME -> Blocks.MAGMA_BLOCK;
            case BLAST -> Blocks.TNT;
        };
        models.update(state.model, block.defaultBlockState(), BLOCK_CONTEXT);
    }

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.visible && !state.model.isEmpty()) {
            pose.pushPose();
            pose.translate(0, .01, 0);
            if (state.flying) {
                // A smaller thrown core tumbles in flight; the deployed plate stays fixed to the world.
                pose.mulPose(Axis.XP.rotationDegrees(state.ageInTicks * 18));
                pose.scale(.65f, .65f, .65f);
            }
            pose.scale(WIDTH, state.bear ? HEIGHT / (3f / 16f) : HEIGHT, WIDTH);
            pose.translate(-.5, 0, -.5);
            state.model.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }

    static final class State extends EntityRenderState {
        final BlockModelRenderState model = new BlockModelRenderState();
        boolean flying, visible, bear;
    }
}
