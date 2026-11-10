package dev.covenant.client;

import dev.covenant.rpg.RpgData;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;

/** Supplies a short native bow-use pose exclusively inside the vanilla rendering calls. */
public final class BowShotAnimation {
    private static final ThreadLocal<LivingEntity> RENDERING = new ThreadLocal<>();

    private BowShotAnimation() {}

    /** Preserve nested render contexts; real item use always takes priority over the cosmetic pose. */
    public static LivingEntity enter(LivingEntity entity) {
        try (var zone = Profiler.get().zone("covenant:bow_pose_enter")) {
            var previous = RENDERING.get();
            if (previous == entity) { return previous; }
            long remaining = entity.hasData(RpgData.BOW_DRAW_UNTIL)
                    ? entity.getData(RpgData.BOW_DRAW_UNTIL) - entity.level().getGameTime() : 0;
            boolean draw = entity.level().isClientSide() && entity.isAlive() && !entity.isUsingItem()
                    && entity.getMainHandItem().getItem() instanceof BowItem
                    && remaining > 0;
            RENDERING.set(draw ? entity : null);
            return previous;
        }
    }

    public static void exit(LivingEntity previous) {
        try (var zone = Profiler.get().zone("covenant:bow_pose_exit")) {
            // Retain the thread-local slot, but no entity, to avoid allocating an entry each frame.
            RENDERING.set(previous);
        }
    }

    public static boolean isDrawing(LivingEntity entity) {
        try (var zone = Profiler.get().zone("covenant:bow_pose_check")) {
            return RENDERING.get() == entity;
        }
    }

    public static int drawTicks(LivingEntity entity) {
        try (var zone = Profiler.get().zone("covenant:bow_pose_time")) {
            long remaining = entity.getData(RpgData.BOW_DRAW_UNTIL) - entity.level().getGameTime();
            long duration = Math.max(1, entity.getData(RpgData.BOW_DRAW_DURATION));
            return (int) Math.clamp((duration - remaining) * 24.0 / duration, 0, 20);
        }
    }
}
