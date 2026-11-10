package dev.covenant.example.gecko;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;

/** Compile-only weapon/tool rendering pattern; native item properties still own gameplay behavior. */
public final class ExampleItem extends Item implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gecko_probe_item.idle");
    private static final RawAnimation ACTION = RawAnimation.begin().thenPlay("animation.gecko_probe_item.step");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ExampleItem(Properties properties) {
        super(properties.stacksTo(1));
        GeoItem.registerSyncedAnimatable(this);
    }

    /** GeckoLib's instance cache invokes this lazily on the physical client only. */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        dev.covenant.example.gecko.client.ExampleRenderers.createItemRenderer(consumer);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ExampleItem>("main", 5,
                state -> state.setAndContinue(IDLE)).triggerableAnim("action", ACTION));
    }

    /** Assign the stack's animation identity only on the server after validating the held action. */
    public void playAcceptedAction(LivingEntity holder, ItemStack stack) {
        if (holder.level() instanceof ServerLevel level && stack.is(this)) {
            triggerAnim(holder, GeoItem.getOrAssignId(stack, level), "main", "action");
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
