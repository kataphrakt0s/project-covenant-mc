package dev.covenant.test;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTimeline;
import com.geckolib.cache.animation.Animation;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.loading.math.MathParser;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.google.gson.JsonParser;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.GeoAnimatable;
import net.minecraft.world.entity.LivingEntity;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

/** Advances the pinned controller with real clips, without a client clock or renderer. */
final class CompanionAnimationFixture {
    static <T extends LivingEntity & GeoEntity> void checkActionRecovery(T pet, AnimatableManager<T> manager, String asset) throws ReflectiveOperationException {
        String prefix = "animation." + asset + ".";
        var id = Identifier.fromNamespaceAndPath("covenant", "entity/" + asset);
        var resource = "/assets/covenant/geckolib/animations/entity/" + asset + ".animation.json";
        var stream = pet.getClass().getResourceAsStream(resource);
        if (stream == null) { throw new IllegalStateException("Missing shipped companion clips"); }
        final com.geckolib.cache.animation.BakedAnimations clips;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            clips = new GeckoLibGsonLoader().bakeGeckoLibAnimationsFile(id,
                    JsonParser.parseReader(reader).getAsJsonObject(), MathParser.createWithDeduplication());
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
        var model = new DefaultedEntityGeoModel<T>(Identifier.fromNamespaceAndPath("covenant", asset)) {
            @Override public Animation getBakedAnimation(T animatable, String name) { return clips.getAnimation(name); }
        };
        var controller = manager.getAnimationControllers().get("main");
        var advance = AnimationController.class.getDeclaredMethod("checkControllerState",
                com.geckolib.animatable.GeoAnimatable.class, GeoRenderState.class, AnimatableManager.class, GeoModel.class);
        advance.setAccessible(true);
        var state = new State(new HashMap<>());
        state.addGeckolibData(DataTickets.IS_MOVING, true);
        var walk = RawAnimation.begin().thenLoop(prefix + "walk");
        var idle = RawAnimation.begin().thenLoop(prefix + "idle");
        for (String action : new String[] {"attack", "hurt", "attack"}) {
            var raw = RawAnimation.begin().thenPlay(prefix + action);
            seedTriggered(controller, pet, model, raw, 0);
            for (int tick = 1; tick <= 50; tick++) {
                state.addGeckolibData(DataTickets.TICK, (double) tick);
                advance.invoke(controller, pet, state, manager, model);
                if (tick <= 4) { require(raw.equals(controller.getCurrentRawAnimation()), "Moving must not interrupt " + action); }
            }
            require(walk.equals(controller.getCurrentRawAnimation()), "Walk must resume after triggered " + action);
            state.addGeckolibData(DataTickets.IS_MOVING, false);
            state.addGeckolibData(DataTickets.TICK, 51d);
            advance.invoke(controller, pet, state, manager, model);
            require(idle.equals(controller.getCurrentRawAnimation()), "Idle must resume when stopped after " + action);
            state.addGeckolibData(DataTickets.IS_MOVING, true);
        }
        // 5.5.2's hasAnimationFinished() misses its negative finished-timeline sentinel.
        // A render resuming after that state must still release the action.
        for (boolean moving : new boolean[] {true, false}) {
            var raw = RawAnimation.begin().thenPlay(prefix + "attack");
            seedTriggered(controller, pet, model, raw,
                    clips.getAnimation(prefix + "attack").length() + controller.getTransitionTicks() / 20f);
            set(controller, "timelineTime", -2d);
            state.addGeckolibData(DataTickets.IS_MOVING, moving);
            state.addGeckolibData(DataTickets.TICK, 1d);
            advance.invoke(controller, pet, state, manager, model);
            require((moving ? walk : idle).equals(controller.getCurrentRawAnimation()),
                    "A fully finished trigger must release to " + (moving ? "walk" : "idle"));
        }
        seedTriggered(controller, pet, model, RawAnimation.begin().thenPlay(prefix + "attack"), 0);
        pet.setHealth(0);
        state.addGeckolibData(DataTickets.IS_MOVING, true);
        state.addGeckolibData(DataTickets.TICK, 1d);
        advance.invoke(controller, pet, state, manager, model);
        require(RawAnimation.begin().thenPlayAndHold(prefix + "death").equals(controller.getCurrentRawAnimation()),
                "Death must interrupt a triggered action");
        for (int tick = 2; tick <= 50; tick++) {
            state.addGeckolibData(DataTickets.TICK, (double) tick);
            advance.invoke(controller, pet, state, manager, model);
        }
        require(controller.getCurrentAnimationPoint().loopType() == com.geckolib.animation.object.LoopType.HOLD_ON_LAST_FRAME,
                "Death must hold instead of resuming locomotion");
    }

    private static <T extends GeoAnimatable> void seedTriggered(AnimationController<T> controller, T pet, GeoModel<T> model,
                                      RawAnimation raw, double time) throws ReflectiveOperationException {
        // Seed a triggered clip after client initialization, avoiding ClientUtil's physical-client clock.
        controller.reset();
        var timeline = AnimationTimeline.create(raw, pet, model, controller.getTransitionTicks());
        set(controller, "currentRawAnimation", raw);
        set(controller, "timeline", timeline);
        set(controller, "animationPoint", timeline.createAnimationPoint(time, null, null));
        set(controller, "timelineTime", time);
        set(controller, "triggeredAnimTime", 0d);
        set(controller, "lastAnimatableAge", 0d);
        set(controller, "playState", com.geckolib.animation.object.PlayState.CONTINUE);
    }

    private static void set(AnimationController<?> controller, String name, Object value) throws ReflectiveOperationException {
        var field = AnimationController.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private record State(Map<DataTicket<?>, Object> getDataMap) implements GeoRenderState {
        @Override public float getPartialTick() { return 0; }
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
    }
}
