package dev.covenant.rpg.progression;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Server-datapack harvesting matches. Array order resolves overlapping ore/tree definitions. */
public record HarvestDefinitions(List<Ore> ores, List<Crop> crops, List<Tree> trees, Match ground) {
    public static final HarvestDefinitions EMPTY = new HarvestDefinitions(List.of(), List.of(), List.of(), Match.parse("#minecraft:dirt"));
    public HarvestDefinitions { ores = List.copyOf(ores); crops = List.copyOf(crops); trees = List.copyOf(trees); }

    /** A block ID or #block-tag ID; unknown block IDs fail reload, while tags may be supplied by packs. */
    public record Match(Identifier id, TagKey<Block> tag, Block block) {
        public static Match parse(String value) {
            boolean tag = value.startsWith("#");
            var id = Identifier.parse(tag ? value.substring(1) : value);
            if (!tag && !BuiltInRegistries.BLOCK.containsKey(id)) { throw new IllegalArgumentException("Unknown harvest block: " + id); }
            return new Match(id, tag ? TagKey.create(Registries.BLOCK, id) : null, tag ? null : BuiltInRegistries.BLOCK.getValue(id));
        }
        public boolean test(BlockState state) {
            return tag != null ? state.is(tag) : state.is(block);
        }
    }
    public record Ore(Match blocks, Identifier activity) {}
    public record Tree(Match logs, Match leaves) {}
    public record Crop(Identifier block, IntegerProperty age, int mature) {
        public boolean test(BlockState state) {
            return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(block) && state.getValue(age) == mature;
        }
    }

    public static HarvestDefinitions parse(JsonObject json) {
        if (json == null) { return EMPTY; }
        var ores = new ArrayList<Ore>(); var crops = new ArrayList<Crop>(); var trees = new ArrayList<Tree>();
        if (json.getAsJsonArray("ores").size() > 256 || json.getAsJsonArray("crops").size() > 256
                || json.getAsJsonArray("trees").size() > 128) { throw new IllegalArgumentException("Too many harvest definitions"); }
        for (var element : json.getAsJsonArray("ores")) {
            var object = element.getAsJsonObject();
            ores.add(new Ore(Match.parse(object.get("blocks").getAsString()), Identifier.parse(object.get("activity").getAsString())));
        }
        var cropIds = new HashSet<Identifier>();
        for (var element : json.getAsJsonArray("crops")) {
            var object = element.getAsJsonObject();
            var id = Identifier.parse(object.get("block").getAsString());
            if (!BuiltInRegistries.BLOCK.containsKey(id) || !cropIds.add(id)) { throw new IllegalArgumentException("Unknown/duplicate crop: " + id); }
            var block = BuiltInRegistries.BLOCK.getValue(id);
            var property = block.getStateDefinition().getProperty(object.get("age_property").getAsString());
            int mature = object.get("mature_age").getAsBigDecimal().intValueExact();
            if (!(property instanceof IntegerProperty age) || !age.getPossibleValues().contains(mature)) {
                throw new IllegalArgumentException("Invalid crop maturity: " + id);
            }
            crops.add(new Crop(id, age, mature));
        }
        for (var element : json.getAsJsonArray("trees")) {
            var object = element.getAsJsonObject();
            trees.add(new Tree(Match.parse(object.get("logs").getAsString()), Match.parse(object.get("leaves").getAsString())));
        }
        return new HarvestDefinitions(ores, crops, trees, Match.parse(json.get("tree_ground").getAsString()));
    }
}
