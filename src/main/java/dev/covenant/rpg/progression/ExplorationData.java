package dev.covenant.rpg.progression;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.ListenerKey;

/** Bounded discovery goals resolved against the reload's staged registry/tag lookup, never client data. */
public final class ExplorationData extends SimplePreparableReloadListener<List<ExplorationData.Goal>> {
    public static final Identifier FILE = ProfessionData.id("progression/exploration.json");
    private static final ListenerKey<ExplorationData> KEY = ListenerKey.create(ProfessionData.id("exploration"));
    private static final AtomicLong REVISIONS = new AtomicLong();
    private Snapshot snapshot;
    public record Snapshot(long revision, List<Goal> goals) { public Snapshot { goals = List.copyOf(goals); } }
    public enum Kind { BIOME, STRUCTURE, DIMENSION }
    public record Goal(Identifier id, String title, String pool, Identifier dimension, Kind kind,
            Map<Identifier, Set<Identifier>> targets, int required) {
        public Goal {
            var copy = new LinkedHashMap<Identifier, Set<Identifier>>();
            targets.forEach((key, values) -> copy.put(key, Set.copyOf(values)));
            targets = Collections.unmodifiableMap(copy);
            if (title.isBlank() || title.length() > 120 || pool.isBlank() || pool.length() > 48 || required < 1
                    || required > (kind == Kind.DIMENSION ? 1 : targets.size())) { throw new IllegalArgumentException("Invalid discovery goal: " + id); }
        }
        public int progress(ExplorationState history) {
            if (kind == Kind.DIMENSION) { return history.dimensions().containsKey(dimension) ? 1 : 0; }
            var discoveries = history.in(dimension);
            var seen = kind == Kind.BIOME ? discoveries.biomes() : discoveries.structures();
            int count = 0;
            for (var members : targets.values()) {
                if (members.stream().anyMatch(seen::contains)) { count++; }
            }
            return Math.min(count, required);
        }
    }
    public static void register(AddServerReloadListenersEvent event) { event.addRetainedListener(KEY, new ExplorationData()); }
    public static Snapshot snapshot(MinecraftServer server) { return server.getServerResources().managers().getListener(KEY).snapshot; }

    public static List<Goal> parse(JsonObject json, HolderLookup.Provider lookup) {
        var entries = json.getAsJsonArray("milestones");
        if (entries == null || entries.isEmpty() || entries.size() > 128) { throw new IllegalArgumentException("Expected 1-128 exploration milestones"); }
        var goals = new ArrayList<Goal>(); var ids = new HashSet<Identifier>();
        for (var entry : entries) {
            var object = entry.getAsJsonObject();
            var id = Identifier.parse(object.get("id").getAsString());
            if (!ids.add(id)) { throw new IllegalArgumentException("Duplicate exploration milestone: " + id); }
            var kind = Kind.valueOf(object.get("kind").getAsString().toUpperCase(Locale.ROOT));
            var targets = new LinkedHashMap<Identifier, Set<Identifier>>();
            if (kind == Kind.BIOME) {
                for (var target : resolve(object.getAsJsonArray("targets"), Registries.BIOME, lookup)) { targets.put(target, Set.of(target)); }
            } else if (kind == Kind.STRUCTURE) {
                var families = object.getAsJsonObject("families");
                if (families == null || families.isEmpty() || families.size() > 256) { throw new IllegalArgumentException("Expected structure families: " + id); }
                for (var family : families.entrySet()) {
                    targets.put(Identifier.parse(family.getKey()), resolve(family.getValue().getAsJsonArray(), Registries.STRUCTURE, lookup));
                }
            }
            int required = 1;
            if (kind != Kind.DIMENSION) {
                int choices = (object.has("count") ? 1 : 0) + (object.has("percent") ? 1 : 0) + (object.has("all") ? 1 : 0);
                if (choices != 1) { throw new IllegalArgumentException("Choose exactly one of count, percent, all: " + id); }
                if (object.has("count")) { required = object.get("count").getAsBigDecimal().intValueExact(); }
                else if (object.has("percent")) {
                    int percent = object.get("percent").getAsBigDecimal().intValueExact();
                    if (percent < 1 || percent > 100) { throw new IllegalArgumentException("Percentage must be 1-100: " + id); }
                    required = (targets.size() * percent + 99) / 100;
                } else {
                    if (!object.get("all").getAsBoolean()) { throw new IllegalArgumentException("all must be true: " + id); }
                    required = targets.size();
                }
            } else if (object.has("count") || object.has("percent") || object.has("all") || object.has("targets") || object.has("families")) {
                throw new IllegalArgumentException("Dimension milestones only require entry: " + id);
            }
            goals.add(new Goal(id, object.get("title").getAsString(), object.get("pool").getAsString(),
                    Identifier.parse(object.get("dimension").getAsString()), kind, targets, required));
        }
        return List.copyOf(goals);
    }
    private static <T> Set<Identifier> resolve(JsonArray selectors, ResourceKey<? extends Registry<T>> registryKey, HolderLookup.Provider lookup) {
        if (selectors == null || selectors.isEmpty() || selectors.size() > 256) { throw new IllegalArgumentException("Expected 1-256 target selectors"); }
        var registry = lookup.lookupOrThrow(registryKey); var result = new LinkedHashSet<Identifier>();
        for (var selector : selectors) {
            String value = selector.getAsString();
            if (value.startsWith("#")) {
                registry.getOrThrow(TagKey.create(registryKey, Identifier.parse(value.substring(1)))).stream()
                        .forEach(holder -> result.add(holder.unwrapKey().orElseThrow().identifier()));
            } else {
                var id = Identifier.parse(value);
                registry.getOrThrow(ResourceKey.create(registryKey, id)); result.add(id);
            }
        }
        if (result.isEmpty() || result.size() > 8192) { throw new IllegalArgumentException("Expected 1-8192 resolved targets"); }
        return Collections.unmodifiableSet(result);
    }
    @Override protected List<Goal> prepare(ResourceManager resources, ProfilerFiller profiler) {
        try (var reader = resources.getResourceOrThrow(FILE).openAsReader()) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject(), getRegistryLookup());
        } catch (java.io.IOException | RuntimeException error) { throw new IllegalArgumentException("Invalid " + FILE + ": " + error.getMessage(), error); }
    }
    @Override protected void apply(List<Goal> goals, ResourceManager resources, ProfilerFiller profiler) {
        snapshot = new Snapshot(REVISIONS.incrementAndGet(), goals);
    }
}
