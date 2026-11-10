package dev.covenant.example.gecko;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.state.AnimationPoint;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.animation.state.ControllerState;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.loading.math.MathParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.resources.Identifier;

/** Loads the shipped wolf through GeckoLib 5.5.2 and checks its actual baked motion. */
public final class WolfAnimationAssetTest {
    public static void main(String[] args) throws Exception {
        Path assets = Path.of(args.length == 0 ? "src/main/resources/assets/covenant" : args[0]);
        GeckoLibGsonLoader loader = new GeckoLibGsonLoader();
        String geometry = "geckolib/models/entity/original_wolf.geo.json";
        BakedGeoModel model = loader.bakeGeckoLibModelFile(id(geometry), json(assets.resolve(geometry)));
        require(model.boneLookup().get().size() == 11, "All eleven rig bones survive runtime baking");
        for (String bone : new String[] {"rig_root", "head", "jaw_bone", "ear_left_bone", "ear_right_bone"}) {
            require(model.getBone(bone).isPresent(), "Missing baked bone " + bone);
        }
        String animations = "geckolib/animations/entity/original_wolf.animation.json";
        BakedAnimations clips = loader.bakeGeckoLibAnimationsFile(id(animations), json(assets.resolve(animations)),
                MathParser.createWithDeduplication());
        require(clips.animations().size() == 5, "Exactly five playable clips");
        Map<String, Double> lengths = Map.of("idle", 4d, "walk", .8d, "attack", .65d, "hurt", .4d, "death", 1d);
        int samples = 0;
        for (var entry : lengths.entrySet()) {
            Animation clip = clip(clips, entry.getKey());
            near(clip.length(), entry.getValue(), "Duration " + entry.getKey());
            LoopType loop = switch (entry.getKey()) {
                case "idle", "walk" -> LoopType.LOOP;
                case "death" -> LoopType.HOLD_ON_LAST_FRAME;
                default -> LoopType.PLAY_ONCE;
            };
            require(clip.loopType() == loop, "Loop policy " + entry.getKey());
            require(clip.usedVariables().isEmpty(), "No unresolved Molang dependencies");
            for (var bone : clip.boneAnimations()) {
                require(model.getBone(bone.boneName()).isPresent(), "Animation targets a baked bone");
                // Includes positions between authored frames, endpoints and repeating seams.
                for (var transform : new AnimationPoint.Transform[] {AnimationPoint.Transform.ROTATION, AnimationPoint.Transform.TRANSLATION}) {
                    for (var axis : AnimationPoint.Axis.values()) {
                        for (int i = 0; i <= Math.round(clip.length() * 120); i++) {
                            double time = Math.min(clip.length(), i / 120d);
                            require(Double.isFinite(sample(model, clip, bone.boneName(), time, transform, axis)), "Finite runtime pose");
                            samples++;
                        }
                        if (loop == LoopType.LOOP) {
                            near(sample(model, clip, bone.boneName(), 0, transform, axis),
                                    sample(model, clip, bone.boneName(), clip.length(), transform, axis), "Loop pose seam");
                        } else if (loop == LoopType.PLAY_ONCE) {
                            near(sample(model, clip, bone.boneName(), clip.length(), transform, axis), 0, "Action recovers to neutral");
                        }
                    }
                }
            }
        }
        Animation walk = clip(clips, "walk");
        near(rotation(model, walk, "foreleg_left", 0, AnimationPoint.Axis.X), 24, "Leading foreleg");
        near(rotation(model, walk, "hindleg_right", 0, AnimationPoint.Axis.X), 24, "Diagonal pair");
        near(rotation(model, walk, "foreleg_right", 0, AnimationPoint.Axis.X), -24, "Opposing foreleg");
        near(rotation(model, walk, "foreleg_left", .4, AnimationPoint.Axis.X), -24, "Second half stride");
        Animation attack = clip(clips, "attack");
        near(rotation(model, attack, "jaw_bone", .25, AnimationPoint.Axis.X), -34, "Open jaw before bite");
        near(rotation(model, attack, "jaw_bone", .3, AnimationPoint.Axis.X), -2, "Bite closes");
        // Between two baked keys: verifies the exporter/runtime's numeric interpolation.
        near(rotation(model, attack, "jaw_bone", .2875, AnimationPoint.Axis.X), -10, "Bite interpolation");
        near(rotation(model, clip(clips, "hurt"), "head", .075, AnimationPoint.Axis.Y), -10, "Recoil direction");
        Animation death = clip(clips, "death");
        near(rotation(model, death, "rig_root", 1, AnimationPoint.Axis.Z), 90, "Death on its side");
        near(rotation(model, death, "rig_root", .9, AnimationPoint.Axis.Z), 90, "Death settles before end");
        require(sample(model, death, "rig_root", 1, AnimationPoint.Transform.TRANSLATION, AnimationPoint.Axis.Y) < 0,
                "Death lowers the visual root to keep the body grounded");
        var atlas = ImageIO.read(assets.resolve("textures/entity/original_wolf.png").toFile());
        require(atlas != null && atlas.getWidth() == 128 && atlas.getHeight() == 64, "Shipped native atlas");
        System.out.println("WOLF ANIMATION ASSET TEST PASSED: 11 bones, 5 clips, " + samples
                + " runtime samples, loop seams, bite/recoil/death poses and 128x64 atlas");
    }

    private static double rotation(BakedGeoModel model, Animation clip, String bone, double seconds, AnimationPoint.Axis axis) {
        return Math.toDegrees(sample(model, clip, bone, seconds, AnimationPoint.Transform.ROTATION, axis));
    }

    private static double sample(BakedGeoModel model, Animation clip, String bone, double seconds,
                                 AnimationPoint.Transform transform, AnimationPoint.Axis axis) {
        int index = -1;
        for (int i = 0; i < clip.boneAnimations().length; i++) {
            if (clip.boneAnimations()[i].boneName().equals(bone)) { index = i; break; }
        }
        require(index >= 0, "Bone has a runtime animation track: " + bone);
        AnimationPoint point = AnimationPoint.createFor(clip, null, clip.loopType(), seconds);
        ControllerState state = new ControllerState(point, null, -1, 0, false, null, null,
                new Reference2DoubleOpenHashMap<>());
        return AnimationProcessor.findAnimationPointValue(BoneSnapshot.create(model.getBone(bone).orElseThrow()),
                state, point, null, index, transform, axis, null);
    }

    private static Animation clip(BakedAnimations clips, String action) {
        Animation clip = clips.getAnimation("animation.original_wolf." + action);
        require(clip != null, "Missing clip " + action);
        return clip;
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("covenant", path); }

    private static JsonObject json(Path path) throws Exception {
        try (var reader = Files.newBufferedReader(path)) { return JsonParser.parseReader(reader).getAsJsonObject(); }
    }

    private static void near(double actual, double expected, String label) {
        require(Math.abs(actual - expected) < .001, label + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
