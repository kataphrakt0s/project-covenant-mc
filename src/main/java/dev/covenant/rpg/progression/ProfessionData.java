package dev.covenant.rpg.progression;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.ListenerKey;

/** Bounded server-authored profession content. Shipped values are provisional prototype tuning. */
public final class ProfessionData extends SimplePreparableReloadListener<ProfessionData.Catalogue> {
    public static final Identifier FILE = id("progression/professions.json");
    public static final Identifier OFFENSIVE_MASTERY = id("logging/offensive_mastery");
    public static final Identifier VEIN_MINING = id("mining/vein_mining");
    public static final Identifier TREE_FELLING = id("logging/tree_felling");
    public static final Identifier SCYTHING = id("farming/scything");
    public static final Identifier FARM_REPLANT = id("farming/auto_replant");
    private static final ListenerKey<ProfessionData> KEY = ListenerKey.create(id("professions"));
    // Retained listeners are replaced on reload. Tokens must distinguish those instances too.
    private static final AtomicLong REVISIONS = new AtomicLong();
    private Snapshot snapshot;

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("covenant", path); }
    public static void register(AddServerReloadListenersEvent event) { event.addRetainedListener(KEY, new ProfessionData()); }
    public static Snapshot snapshot(MinecraftServer server) { return server.getServerResources().managers().getListener(KEY).snapshot; }

    /** Level/cost are per rank. Blocks counts total gathered blocks (1-64); area is an odd Scything width (1-9).
     * Both gathering fields default to zero for unrelated abilities. Shipped Scything widths are 3/5/9 blocks. */
    public record Rank(int level, int cost, double bonus, int blocks, int area) {
        public static final Codec<Rank> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 1000).fieldOf("level").forGetter(Rank::level),
                Codec.intRange(1, 1000).fieldOf("cost").forGetter(Rank::cost),
                Codec.DOUBLE.optionalFieldOf("bonus", 0.0).forGetter(Rank::bonus),
                Codec.intRange(0, 64).optionalFieldOf("blocks", 0).forGetter(Rank::blocks),
                Codec.intRange(0, 9).optionalFieldOf("area", 0).forGetter(Rank::area)
        ).apply(i, Rank::new));
        public Rank {
            if (level < 1 || level > 1000 || cost < 1 || cost > 1000 || !Double.isFinite(bonus) || bonus < 0 || bonus > 1024
                    || blocks < 0 || blocks > 64 || area < 0 || area > 9 || (area != 0 && area % 2 == 0)) {
                throw new IllegalArgumentException("Invalid profession rank");
            }
        }
    }
    public record Node(Identifier id, Identifier profession, List<Rank> ranks, Optional<Identifier> prerequisite, int order) {
        public static final Codec<Node> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("id").forGetter(Node::id),
                Identifier.CODEC.fieldOf("profession").forGetter(Node::profession),
                Rank.CODEC.listOf().fieldOf("ranks").forGetter(Node::ranks),
                Identifier.CODEC.optionalFieldOf("prerequisite").forGetter(Node::prerequisite),
                Codec.intRange(0, 127).fieldOf("order").forGetter(Node::order)
        ).apply(i, Node::new));
        public Node {
            ranks = List.copyOf(ranks);
            if (!ProgressionData.PROFESSIONS.contains(profession) || ranks.isEmpty() || ranks.size() > 64
                    || order < 0 || order > 127 || !id.getPath().startsWith(profession.getPath() + "/")) {
                throw new IllegalArgumentException("Invalid profession ability: " + id);
            }
            int previous = 0;
            for (var rank : ranks) {
                if (rank.level() < previous) { throw new IllegalArgumentException("Rank gates must be ordered: " + id); }
                if ((id.equals(VEIN_MINING) || id.equals(TREE_FELLING)) && rank.blocks() < 1) {
                    throw new IllegalArgumentException("Gathering ranks require blocks in 1-64: " + id);
                }
                if (id.equals(SCYTHING) && rank.area() < 1) { throw new IllegalArgumentException("Scything requires an odd area width in 1-9"); }
                previous = rank.level();
            }
        }
        /** Availability is owned by runtime code, never enabled by trusting a datapack/client flag. */
        public boolean implemented() { return id.equals(OFFENSIVE_MASTERY) || id.equals(VEIN_MINING) || id.equals(TREE_FELLING)
                || id.equals(SCYTHING) || id.equals(FARM_REPLANT)
                || id.equals(dev.covenant.rpg.equipment.WorkbenchOperations.BASIC)
                || id.equals(dev.covenant.rpg.equipment.WorkbenchOperations.SALVAGING)
                || id.equals(dev.covenant.rpg.equipment.WorkbenchOperations.RECOVERY)
                || id.equals(dev.covenant.rpg.equipment.WorkbenchOperations.FOCUSED); }
        public Component name() { return Component.translatable("profession_ability." + id.getNamespace() + "." + id.getPath().replace('/', '.')); }
        public Component description() { return Component.translatable("profession_ability." + id.getNamespace() + "." + id.getPath().replace('/', '.') + ".description"); }
    }
    public record Catalogue(Map<Identifier, Node> abilities, Map<Identifier, Long> activityRewards, HarvestDefinitions harvesting) {
        public Catalogue { abilities = Map.copyOf(abilities); activityRewards = Map.copyOf(activityRewards); }
    }
    public record Snapshot(long revision, Catalogue catalogue) {}

    public static Catalogue parse(JsonObject json) {
        var nodes = new LinkedHashMap<Identifier, Node>();
        for (var element : json.getAsJsonArray("abilities")) {
            var object = element.getAsJsonObject();
            object.get("order").getAsBigDecimal().intValueExact();
            for (var rank : object.getAsJsonArray("ranks")) {
                rank.getAsJsonObject().get("level").getAsBigDecimal().intValueExact();
                rank.getAsJsonObject().get("cost").getAsBigDecimal().intValueExact();
                if (rank.getAsJsonObject().has("blocks")) { rank.getAsJsonObject().get("blocks").getAsBigDecimal().intValueExact(); }
                if (rank.getAsJsonObject().has("area")) { rank.getAsJsonObject().get("area").getAsBigDecimal().intValueExact(); }
            }
            var node = Node.CODEC.parse(JsonOps.INSTANCE, element).getOrThrow();
            if (nodes.put(node.id(), node) != null) { throw new IllegalArgumentException("Duplicate profession ability: " + node.id()); }
        }
        if (nodes.isEmpty() || nodes.size() > 128) { throw new IllegalArgumentException("Expected 1-128 profession abilities"); }
        for (var profession : ProgressionData.PROFESSIONS) {
            if (nodes.values().stream().filter(n -> n.profession().equals(profession)).count() > 32) {
                throw new IllegalArgumentException("Too many abilities in " + profession);
            }
        }
        for (var node : nodes.values()) {
            var seen = new HashSet<Identifier>();
            var current = node;
            while (current.prerequisite().isPresent()) {
                if (!seen.add(current.id())) { throw new IllegalArgumentException("Cyclic profession prerequisite: " + node.id()); }
                var parent = nodes.get(current.prerequisite().orElseThrow());
                if (parent == null || !parent.profession().equals(node.profession())) {
                    throw new IllegalArgumentException("Missing/cross-profession prerequisite: " + node.id());
                }
                current = parent;
            }
        }
        var rewards = new HashMap<Identifier, Long>();
        for (var entry : json.getAsJsonObject("activity_rewards").entrySet()) {
            long value = entry.getValue().getAsBigDecimal().longValueExact();
            if (value < 0) { throw new IllegalArgumentException("Negative activity XP: " + entry.getKey()); }
            rewards.put(Identifier.parse(entry.getKey()), value);
        }
        if (rewards.size() > 4096) { throw new IllegalArgumentException("Too many activity XP entries"); }
        var harvesting = HarvestDefinitions.parse(json.getAsJsonObject("harvesting"));
        for (var ore : harvesting.ores()) {
            if (!rewards.containsKey(ore.activity())) { throw new IllegalArgumentException("Missing ore activity reward: " + ore.activity()); }
        }
        return new Catalogue(nodes, rewards, harvesting);
    }
    @Override protected Catalogue prepare(ResourceManager resources, ProfilerFiller profiler) {
        try (var reader = resources.getResourceOrThrow(FILE).openAsReader()) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (java.io.IOException | RuntimeException error) {
            throw new IllegalArgumentException("Invalid " + FILE + ": " + error.getMessage(), error);
        }
    }
    @Override protected void apply(Catalogue prepared, ResourceManager resources, ProfilerFiller profiler) {
        snapshot = new Snapshot(REVISIONS.incrementAndGet(), prepared);
    }
}
