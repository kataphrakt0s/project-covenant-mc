package dev.covenant.test.assets;

import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.loading.math.MathParser;
import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.state.AnimationPoint;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.animation.state.ControllerState;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.model.BakedGeoModel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import net.minecraft.resources.Identifier;

/** Bakes all four golems and samples the shared body/core clips through the actual pinned runtime. */
public final class GolemAssetTest {
    public static void main(String[] args) throws Exception {
        Path assets = Path.of(args.length == 0 ? "src/main/resources/assets/covenant" : args[0]);
        var loader = new GeckoLibGsonLoader();
        BakedGeoModel model = null;
        for (String suffix : new String[] {"", "_fire", "_ice", "_lightning"}) {
            String geometry = "geckolib/models/entity/elemental_golem" + suffix + ".geo.json";
            var json = json(assets.resolve(geometry));
            var baked = loader.bakeGeckoLibModelFile(id(geometry), json);
            if (model == null) { model = baked; }
            require(baked.boneLookup().get().size() == 17, "All seventeen rig bones bake: " + suffix);
            for (String bone : new String[] {"rig_root", "pelvis", "torso", "head", "core", "left_upper_arm", "right_foot"}) {
                require(baked.getBone(bone).isPresent(), "Missing baked bone " + bone);
            }
            var body = json.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var description = body.getAsJsonObject("description");
            require(description.get("texture_width").getAsInt() == 128 && description.get("texture_height").getAsInt() == 128, "Native texture dimensions");
            int cubes = 0;
            for (var entry : body.getAsJsonArray("bones")) {
                var bone = entry.getAsJsonObject();
                if (bone.has("cubes")) { cubes += bone.getAsJsonArray("cubes").size(); }
                if (bone.get("name").getAsString().equals("core")) {
                    double z = bone.getAsJsonArray("pivot").get(2).getAsDouble();
                    require(z + Math.sqrt(12) < 2 && z - Math.sqrt(12) > -5, "Full rotating core remains inside chest depth");
                }
            }
            require(cubes == 34, "All approved form cubes export: " + suffix);
            var image = ImageIO.read(assets.resolve("textures/entity/elemental_golem" + suffix + ".png").toFile());
            require(image != null && image.getWidth() == 128 && image.getHeight() == 128, "Runtime PNG decodes at native resolution");
            for (int y = 0; y < 128; y++) { for (int x = 0; x < 128; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                require(alpha == 0 || alpha == 255, "No unintended partial-alpha texels");
            } }
        }
        String animation = "geckolib/animations/entity/elemental_golem.animation.json";
        var clips = loader.bakeGeckoLibAnimationsFile(id(animation), json(assets.resolve(animation)), MathParser.createWithDeduplication());
        require(clips.animations().size() == 6, "Five body clips plus one independent core loop");
        for (String name : new String[] {"idle", "walk", "attack", "hurt", "death", "core_spin"}) {
            var clip = clips.getAnimation("animation.elemental_golem." + name);
            require(clip != null && clip.usedVariables().isEmpty(), "Numeric clip exists: " + name);
            double duration = switch (name) { case "idle" -> 4; case "walk" -> 1; case "attack" -> .6; case "hurt" -> .35; case "death" -> .9; default -> 20; };
            near(clip.length(), duration, "Clip duration " + name);
            LoopType loop = switch (name) { case "idle", "walk", "core_spin" -> LoopType.LOOP; case "death" -> LoopType.HOLD_ON_LAST_FRAME; default -> LoopType.PLAY_ONCE; };
            require(clip.loopType() == loop, "Clip loop mode " + name);
            for (var track : clip.boneAnimations()) {
                require(model.getBone(track.boneName()).isPresent(), "Track targets a real bone");
                require(track.boneName().equals("core") == name.equals("core_spin"), "Body/core controllers have separate bone ownership");
                for (double t = 0; t < duration; t += .025) {
                    for (var axis : AnimationPoint.Axis.values()) {
                        double value = rotation(model, clip, track.boneName(), t, axis);
                        require(Double.isFinite(value), "Finite baked motion");
                    }
                }
                if (loop == LoopType.LOOP) {
                    for (var axis : AnimationPoint.Axis.values()) {
                        double a = rotation(model, clip, track.boneName(), 0, axis);
                        double b = rotation(model, clip, track.boneName(), duration - .0001, axis);
                        double difference = Math.abs(((a - b + 540) % 360) - 180);
                        require(difference < .02, "Continuous loop seam: " + name + "/" + track.boneName());
                    }
                }
            }
        }
        var walk = clips.getAnimation("animation.elemental_golem.walk");
        near(Math.abs(rotation(model, walk, "left_thigh", 0, AnimationPoint.Axis.X)), 12, "Simple stride extent");
        near(rotation(model, walk, "left_thigh", 0, AnimationPoint.Axis.X) + rotation(model, walk, "right_thigh", 0, AnimationPoint.Axis.X), 0, "Opposed leg phases");
        var attack = clips.getAnimation("animation.elemental_golem.attack");
        near(Math.abs(rotation(model, attack, "left_upper_arm", 0, AnimationPoint.Axis.X)), 65, "Contact-first attack pose");
        near(rotation(model, attack, "left_upper_arm", .6, AnimationPoint.Axis.X), 0, "Attack recovers to rest");
        var core = clips.getAnimation("animation.elemental_golem.core_spin");
        require(core.boneAnimations().length == 1, "Core loop only owns its own bone");
        near(Math.abs(rotation(model, core, "core", 5, AnimationPoint.Axis.Y)), 90, "Slow quarter turn");
        near(Math.abs(rotation(model, core, "core", 5, AnimationPoint.Axis.X)), 10, "Gyroscopic tilt");
        var death = clips.getAnimation("animation.elemental_golem.death");
        near(Math.abs(rotation(model, death, "left_thigh", .9, AnimationPoint.Axis.X)), 60, "Kneeling death");
        System.out.println("GOLEM ASSET TEST PASSED: four forms, 17 bones / 34 cubes each, five body clips, independent core loop and dense pinned-runtime samples");
    }
    private static double rotation(BakedGeoModel model, Animation clip, String bone, double time, AnimationPoint.Axis axis) {
        int index = -1;
        for (int i = 0; i < clip.boneAnimations().length; i++) { if (clip.boneAnimations()[i].boneName().equals(bone)) { index = i; break; } }
        require(index >= 0, "Runtime track exists: " + bone);
        var point = AnimationPoint.createFor(clip, null, clip.loopType(), time);
        var state = new ControllerState(point, null, -1, 0, false, null, null, new Reference2DoubleOpenHashMap<>());
        return Math.toDegrees(AnimationProcessor.findAnimationPointValue(BoneSnapshot.create(model.getBone(bone).orElseThrow()),
                state, point, null, index, AnimationPoint.Transform.ROTATION, axis, null));
    }
    private static void near(double actual, double expected, String message) { require(Math.abs(actual - expected) < .002, message + ": " + actual); }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("covenant", path); }
    private static JsonObject json(Path path) throws Exception {
        try (var reader = Files.newBufferedReader(path)) { return JsonParser.parseReader(reader).getAsJsonObject(); }
    }
    private static void require(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
}
