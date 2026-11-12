package dev.covenant.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.covenant.rpg.skills.GroundSkillEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;

/** Native banner item model and quiet depth-tested footprints. Core area information survives Minimal particles. */
final class GroundSkillRenderer extends EntityRenderer<GroundSkillEntity,GroundSkillRenderer.State> {
    private static final int SEGMENTS=64;
    private final ItemModelResolver items;
    private ItemStack banner;
    private final java.util.Map<java.util.UUID,Boundary> boundaries=new java.util.WeakHashMap<>();
    GroundSkillRenderer(EntityRendererProvider.Context context) { super(context); items=context.getItemModelResolver(); }
    @Override public State createRenderState() { return new State(); }
    @Override protected AABB getBoundingBoxForCulling(GroundSkillEntity e) { return e.getBoundingBox().inflate(e.radius()); }
    @Override public void extractRenderState(GroundSkillEntity e, State state, float partialTick) {
        super.extractRenderState(e,state,partialTick);
        state.radius=(float)e.radius(); state.banner=e.banner(); state.yaw=e.getYRot();
        var boundary=boundaries.computeIfAbsent(e.getUUID(), ignored->new Boundary());
        if (!state.banner && e.impactSequence()!=boundary.impactSequence) {
            boundary.impactSequence=e.impactSequence();
            boundary.impacts.add(new Impact(e.impactFrom(),e.impactTo(),e.tickCount,e.fallTicks(),e.splashRadius()));
        }
        boundary.impacts.removeIf(impact->e.tickCount-impact.start>impact.ticks+2);
        state.impacts=java.util.List.copyOf(boundary.impacts);
        if (boundary.tick!=e.tickCount/10) {
            boundary.tick=e.tickCount/10;
            for (int i=0;i<SEGMENTS;i++) {
                double angle=(i+.5)*Math.PI*2/SEGMENTS;
                var point=e.position().add(Math.cos(angle)*state.radius,0,Math.sin(angle)*state.radius);
                var box=new AABB(point.add(-.03,-.05,-.03),point.add(.03,.005,.03));
                boundary.supported[i]=GroundSkillEntity.loaded(e.level(),box) && e.level().getBlockCollisions(e,box).iterator().hasNext();
            }
        }
        System.arraycopy(boundary.supported,0,state.supported,0,SEGMENTS);
        if (state.banner) {
            if (banner==null) { banner=new ItemStack(Items.RED_BANNER); }
            items.updateForNonLiving(state.item,banner,ItemDisplayContext.NONE,e);
        } else { state.item.clear(); }
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.banner) {
            pose.pushPose(); pose.translate(0,.5,0); pose.mulPose(Axis.YP.rotationDegrees(-state.yaw));
            state.item.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,state.outlineColor); pose.popPose();
        }
        collector.submitCustomGeometry(pose,RenderTypes.debugQuads(),(matrix,buffer)->{
            for (int i=0;i<SEGMENTS;i++) {
                if (!state.supported[i] || (i%4==3)) { continue; }
                double a=i*Math.PI*2/SEGMENTS,b=(i+1)*Math.PI*2/SEGMENTS;
                float inner=state.radius-.025f,outer=state.radius+.025f;
                int color=state.banner?0x9078BB91:0xA0D64458;
                buffer.addVertex(matrix,(float)Math.cos(a)*inner,.025f,(float)Math.sin(a)*inner).setColor(color);
                buffer.addVertex(matrix,(float)Math.cos(b)*inner,.025f,(float)Math.sin(b)*inner).setColor(color);
                buffer.addVertex(matrix,(float)Math.cos(b)*outer,.025f,(float)Math.sin(b)*outer).setColor(color);
                buffer.addVertex(matrix,(float)Math.cos(a)*outer,.025f,(float)Math.sin(a)*outer).setColor(color);
            }
            for (var impact:state.impacts) {
                double t=Math.clamp((state.ageInTicks-impact.start)/impact.ticks,0,1);
                var point=impact.from.lerp(impact.to,t);
                float x=(float)point.x,y=(float)point.y,z=(float)point.z;
                buffer.addVertex(matrix,x-.025f,y,z).setColor(0xE0FF8291);
                buffer.addVertex(matrix,x+.025f,y,z).setColor(0xE0FF8291);
                buffer.addVertex(matrix,x+.025f,y+.35f,z).setColor(0xE0FF8291);
                buffer.addVertex(matrix,x-.025f,y+.35f,z).setColor(0xE0FF8291);
                buffer.addVertex(matrix,x,y,z-.025f).setColor(0xE0FF8291);
                buffer.addVertex(matrix,x,y,z+.025f).setColor(0xE0FF8291);
                buffer.addVertex(matrix,x,y+.35f,z+.025f).setColor(0xE0FF8291);
                buffer.addVertex(matrix,x,y+.35f,z-.025f).setColor(0xE0FF8291);
                for(int i=0;i<24;i++) {
                    double a=i*Math.PI/12,b=(i+1)*Math.PI/12;
                    float inner=impact.radius-.018f,outer=impact.radius+.018f,px=(float)impact.to.x,py=(float)impact.to.y+.025f,pz=(float)impact.to.z;
                    buffer.addVertex(matrix,px+(float)Math.cos(a)*inner,py,pz+(float)Math.sin(a)*inner).setColor(0xA0D64458);
                    buffer.addVertex(matrix,px+(float)Math.cos(b)*inner,py,pz+(float)Math.sin(b)*inner).setColor(0xA0D64458);
                    buffer.addVertex(matrix,px+(float)Math.cos(b)*outer,py,pz+(float)Math.sin(b)*outer).setColor(0xA0D64458);
                    buffer.addVertex(matrix,px+(float)Math.cos(a)*outer,py,pz+(float)Math.sin(a)*outer).setColor(0xA0D64458);
                }
            }
        });
        super.submit(state,pose,collector,camera);
    }
    private record Impact(net.minecraft.world.phys.Vec3 from,net.minecraft.world.phys.Vec3 to,float start,int ticks,float radius) {}
    private static final class Boundary {
        int tick=-1,impactSequence;
        final boolean[] supported=new boolean[SEGMENTS];
        final java.util.List<Impact> impacts=new java.util.ArrayList<>();
    }
    static final class State extends EntityRenderState {
        float radius,yaw; boolean banner;
        final boolean[] supported=new boolean[SEGMENTS];
        final ItemStackRenderState item=new ItemStackRenderState();
        java.util.List<Impact> impacts=java.util.List.of();
    }
}
