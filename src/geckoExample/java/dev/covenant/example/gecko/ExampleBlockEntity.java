package dev.covenant.example.gecko;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;

/** Compile-only block animation host; no ticker is needed solely to animate the model. */
public final class ExampleBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gecko_probe_block.idle");
    private static final RawAnimation ACTION = RawAnimation.begin().thenPlay("animation.gecko_probe_block.step");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ExampleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ExampleBlockEntity>("main", 5,
                state -> state.setAndContinue(IDLE)).triggerableAnim("action", ACTION));
    }

    /** Server-accepted visual event; persist machine state separately when the block needs it. */
    public void playAcceptedAction() {
        if (getLevel() != null && !getLevel().isClientSide()) {
            triggerAnim("main", "action");
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
