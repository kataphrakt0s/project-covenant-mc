package dev.covenant.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.covenant.rpg.skills.TrapEntity;
import dev.covenant.rpg.skills.TrapKind;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Original native item models and supported footprints; no extra gameplay geometry or saved items. */
final class TrapRenderer extends EntityRenderer<TrapEntity, TrapRenderer.State> {
    private final ItemModelResolver items;
    // Renderer factories run during startup before item components are bound in 26.1.
    // Create this visual only while extracting an actual in-world trap.
    private final ItemStack[] devices = new ItemStack[TrapKind.values().length];
    // Native render states are recreated each frame. Weak UUID keys avoid retaining deployments
    // and avoid Entity.equals(), whose numeric IDs can be reused by another client world.
    private final java.util.Map<java.util.UUID, TrapBoundary> boundaries = new java.util.WeakHashMap<>();

    TrapRenderer(EntityRendererProvider.Context context) {
        super(context);
        items = context.getItemModelResolver();
    }

    private ItemStack visual(TrapKind kind) {
        var device = devices[kind.ordinal()];
        if (device == null) {
            device = new ItemStack(kind.visual);
            device.set(DataComponents.ITEM_MODEL, kind.skillId);
            devices[kind.ordinal()] = device;
        }
        return device;
    }

    @Override public State createRenderState() { return new State(); }

    @Override protected AABB getBoundingBoxForCulling(TrapEntity trap) {
        return trap.getBoundingBox().inflate(trap.phase() == TrapEntity.Phase.TRIGGERED
                ? trap.kind().radius : TrapEntity.CONTACT_RADIUS);
    }

    @Override public void extractRenderState(TrapEntity trap, State state, float partialTick) {
        super.extractRenderState(trap, state, partialTick);
        state.flying = trap.phase() == TrapEntity.Phase.FLIGHT;
        state.visible = trap.phase() != TrapEntity.Phase.REMOVED && trap.phase() != TrapEntity.Phase.TRIGGERED;
        state.arming = trap.phase() == TrapEntity.Phase.ARMING;
        state.halfHeight = switch (trap.kind()) {
            case BEAR -> 4f / 32f;
            case FROST, BLAST -> 5f / 32f;
            case FLAME -> 6f / 32f;
        };
        state.boundary = boundaries.computeIfAbsent(trap.getUUID(), ignored -> new TrapBoundary());
        state.boundary.update(trap);
        if (state.visible) { items.updateForNonLiving(state.device, visual(trap.kind()), ItemDisplayContext.NONE, trap); }
        else { state.device.clear(); }
    }

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        state.boundary.submit(pose, collector, state.arming);
        if (state.visible && !state.device.isEmpty()) {
            pose.pushPose();
            pose.translate(0, .01, 0);
            if (state.flying) {
                pose.translate(0, state.halfHeight, 0);
                pose.mulPose(Axis.XP.rotationDegrees(state.ageInTicks * 18));
                pose.scale(.65f, .65f, .65f);
                pose.translate(0, .5 - state.halfHeight, 0);
            } else {
                if (state.arming) { pose.scale(1, .7f, 1); }
                pose.translate(0, .5, 0);
            }
            state.device.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }

    static final class State extends EntityRenderState {
        final ItemStackRenderState device = new ItemStackRenderState();
        TrapBoundary boundary;
        float halfHeight;
        boolean flying, visible, arming;
    }
}
