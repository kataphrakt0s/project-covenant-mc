package dev.covenant.example.gecko;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.object.EasingType;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.state.AnimationPoint;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.animation.state.ControllerState;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.animation.Keyframe;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.loading.math.MathParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.minecraft.resources.Identifier;

/** Checks actual MCP smoke exports through the pinned runtime's parser, baker and interpolation. */
public final class GeckoAssetTest {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Pass the gecko-smoke assets/covenant directory via -PgeckoAssets=...");
        }
        Path assets = Path.of(args[0]);
        GeckoLibGsonLoader loader = new GeckoLibGsonLoader();
        for (String kind : new String[] {"entity", "block", "item"}) {
            String asset = "gecko_probe_" + kind;
            String modelPath = "geckolib/models/" + kind + "/" + asset + ".geo.json";
            BakedGeoModel model = loader.bakeGeckoLibModelFile(id(modelPath), json(assets.resolve(modelPath)));
            require(model.getBone("root").isPresent() && model.getBone("hinge").isPresent(), "Baked rig bones: " + kind);
            String animationPath = "geckolib/animations/" + kind + "/" + asset + ".animation.json";
            BakedAnimations clips = loader.bakeGeckoLibAnimationsFile(id(animationPath),
                    json(assets.resolve(animationPath)), MathParser.createWithDeduplication());
            require(clips.animations().size() == 3, "All three clips survived baking: " + kind);
            Animation idle = clip(clips, asset, "idle", LoopType.LOOP);
            near(sample(model, idle, .125, AnimationPoint.Axis.Z), 15, "Linear intermediate pose");
            near(sample(model, idle, 0, AnimationPoint.Axis.Z), 0, "Loop start");
            near(sample(model, idle, 1, AnimationPoint.Axis.Z), 0, "Loop end");
            Animation step = clip(clips, asset, "step", LoopType.PLAY_ONCE);
            near(sample(model, step, .2, AnimationPoint.Axis.X), 0, "Step before boundary");
            near(sample(model, step, .3, AnimationPoint.Axis.X), 20, "Step after boundary");
            near(sample(model, step, .7, AnimationPoint.Axis.X), 20, "Step remains held");
            near(sample(model, step, .8, AnimationPoint.Axis.X), -20, "Second step");
            Animation spline = clip(clips, asset, "catmullrom", LoopType.HOLD_ON_LAST_FRAME);
            Keyframe[] keys = spline.boneAnimations()[0].rotationKeyFrames().xKeyframes();
            require(Arrays.stream(keys).allMatch(key -> key.easingType() == EasingType.CATMULLROM),
                    "Catmull-Rom must survive export/baking, never silently become linear");
            near(sample(model, spline, .125, AnimationPoint.Axis.X), 12.5, "Spline intermediate pose");
            // 5.5.2 duplicates the starting control point here; Blockbench's spline differs.
            near(sample(model, spline, .375, AnimationPoint.Axis.X), 11.40625, "Pinned runtime spline interior");
            near(sample(model, spline, 1, AnimationPoint.Axis.X), 0, "Held final pose");
            require(Files.size(assets.resolve("textures/" + kind + "/" + asset + ".png")) > 0, "Texture exists");
        }
        JsonObject item = json(assets.resolve("items/gecko_probe_item.json")).getAsJsonObject("model");
        require(item.get("type").getAsString().equals("minecraft:special"), "Native special item model");
        require(item.get("base").getAsString().equals("covenant:item/gecko_probe_item"), "Item display base");
        require(item.getAsJsonObject("model").get("type").getAsString().equals("geckolib:geckolib"), "GeckoLib special renderer id");
        System.out.println("GECKOLIB ASSET TEST PASSED: 3 models, 9 clips, loop/step/Catmull-Rom runtime samples and item references");
    }

    private static Animation clip(BakedAnimations clips, String asset, String name, LoopType loop) {
        Animation animation = clips.getAnimation("animation." + asset + "." + name);
        require(animation != null, "Missing baked animation " + name);
        require(animation.loopType() == loop && animation.length() == 1, "Clip duration/loop " + name);
        require(animation.boneAnimations().length == 1 && animation.boneAnimations()[0].boneName().equals("hinge"), "Animated bone " + name);
        require(animation.usedVariables().isEmpty(), "Smoke curves must be numerical");
        return animation;
    }

    private static double sample(BakedGeoModel model, Animation animation, double seconds, AnimationPoint.Axis axis) {
        AnimationPoint point = AnimationPoint.createFor(animation, null, animation.loopType(), seconds);
        // Numerical fixtures require no world, Molang queries, render state or GPU.
        ControllerState state = new ControllerState(point, null, -1, 0, false, null, null,
                new Reference2DoubleOpenHashMap<>());
        return Math.toDegrees(AnimationProcessor.findAnimationPointValue(
                BoneSnapshot.create(model.getBone("hinge").orElseThrow()), state, point, null,
                0, AnimationPoint.Transform.ROTATION, axis, null));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("covenant", path);
    }

    private static JsonObject json(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void near(double actual, double expected, String label) {
        require(Math.abs(actual - expected) < .001, label + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
