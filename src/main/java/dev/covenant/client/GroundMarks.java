package dev.covenant.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Same-elevation footprint helpers. Auras deliberately omit the traps' center-to-edge cover ray. */
final class GroundMarks {
    private GroundMarks() {}

    static boolean supported(Entity source, Vec3 offset, boolean requireCover) {
        var point = source.position().add(offset);
        var center = source.position().add(0, .1, 0);
        var target = point.add(0, .1, 0);
        var level = source.level();
        var bounds = new AABB(center, target).inflate(.05);
        if (!level.hasChunksAt(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) { return false; }
        if (!level.getFluidState(BlockPos.containing(point.add(0, -.01, 0))).isEmpty()) { return false; }
        var support = level.clip(new ClipContext(point.add(0, .04, 0), point.add(0, -.04, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        return support.getType() == HitResult.Type.BLOCK && support.getDirection() == Direction.UP
                && Math.abs(support.getLocation().y - point.y) < .02
                && (!requireCover || level.clip(new ClipContext(center, target, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, source)).getType() == HitResult.Type.MISS);
    }

    static void strip(PoseStack.Pose pose, VertexConsumer buffer, float ax, float az,
                      float bx, float bz, double width, float y, int color) {
        double length = Math.hypot(bx - ax, bz - az);
        if (length == 0) { return; }
        float nx = (float) (-(bz - az) / length * width * .5);
        float nz = (float) ((bx - ax) / length * width * .5);
        buffer.addVertex(pose, ax + nx, y, az + nz).setColor(color);
        buffer.addVertex(pose, bx + nx, y, bz + nz).setColor(color);
        buffer.addVertex(pose, bx - nx, y, bz - nz).setColor(color);
        buffer.addVertex(pose, ax - nx, y, az - nz).setColor(color);
    }
}
