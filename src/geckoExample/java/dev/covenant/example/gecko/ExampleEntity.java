package dev.covenant.example.gecko;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;

/** Compile-only starting point. No registry entry, spawning, attributes or gameplay is installed. */
public final class ExampleEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gecko_probe_entity.idle");
    private static final RawAnimation ACTION = RawAnimation.begin().thenPlay("animation.gecko_probe_entity.step");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ExampleEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ExampleEntity>("main", 5,
                state -> state.setAndContinue(IDLE)).triggerableAnim("action", ACTION));
    }

    /** Call after the logical server has accepted the actual gameplay action. */
    public void playAcceptedAction() {
        if (!level().isClientSide()) {
            triggerAnim("main", "action");
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
