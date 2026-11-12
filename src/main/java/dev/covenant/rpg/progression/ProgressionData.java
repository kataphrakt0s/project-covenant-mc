package dev.covenant.rpg.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.ListenerKey;

/** One immutable, resource-scoped balance snapshot; a failed reload never replaces the live snapshot. */
public final class ProgressionData extends SimplePreparableReloadListener<ProgressionData.Balance> {
    public static final Identifier FILE = Identifier.fromNamespaceAndPath("covenant", "progression/balance.json");
    public static final Identifier CHARACTER = Identifier.fromNamespaceAndPath("covenant", "character");
    public static final Identifier BUILDING = id("building");
    public static final Identifier FISHING = id("fishing"), EXPLORATION = id("exploration");
    public static final List<Identifier> PROFESSIONS = List.of(id("mining"), id("logging"), id("farming"), id("crafting"), BUILDING, FISHING, EXPLORATION);
    public static final List<Identifier> XP_PROFESSIONS = PROFESSIONS.stream().filter(id -> !id.equals(EXPLORATION)).toList();
    private static final ListenerKey<ProgressionData> KEY = ListenerKey.create(id("progression"));
    private Balance balance;

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("covenant", path); }

    public static void register(AddServerReloadListenersEvent event) {
        event.addRetainedListener(KEY, new ProgressionData());
    }

    public static Balance get(MinecraftServer server) {
        return server.getServerResources().managers().getListener(KEY).balance;
    }

    @Override protected Balance prepare(ResourceManager resources, ProfilerFiller profiler) {
        try (var reader = resources.getResourceOrThrow(FILE).openAsReader()) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException error) {
            throw new IllegalArgumentException("Invalid " + FILE + ": " + error.getMessage(), error);
        }
    }

    @Override protected void apply(Balance prepared, ResourceManager resources, ProfilerFiller profiler) {
        balance = prepared;
    }

    /** Caps are levels; requirements and rewards are whole Covenant XP, unrelated to vanilla orbs. */
    public record Balance(Map<Identifier, Integer> caps, List<Long> characterRequirements,
            List<Long> professionRequirements, long hostileReward, Map<Identifier, Long> entityRewards,
            List<RewardRule> rewardTags, List<DimensionRule> dimensions, Map<Identifier, Integer> fallback) {
        public Balance {
            caps = Map.copyOf(caps); characterRequirements = List.copyOf(characterRequirements);
            professionRequirements = List.copyOf(professionRequirements); entityRewards = Map.copyOf(entityRewards);
            rewardTags = List.copyOf(rewardTags); dimensions = List.copyOf(dimensions); fallback = Map.copyOf(fallback);
        }
        public int maximum(Identifier track) { return caps.getOrDefault(track, 0); }
        public long required(Identifier track, int level) {
            if (track.equals(EXPLORATION)) { return 0; }
            if (level >= maximum(track)) { return 0; }
            var requirements = track.equals(CHARACTER) ? characterRequirements : professionRequirements;
            int index = track.equals(CHARACTER) ? level - 1 : level;
            return index >= 0 && index < requirements.size() ? requirements.get(index) : 0;
        }
        public int gate(ServerLevel level, Identifier track) {
            if (track.equals(EXPLORATION)) { return maximum(track); }
            DimensionRule chosen = null;
            for (var rule : dimensions) {
                if (!rule.caps.containsKey(track)) { continue; }
                if (!rule.tag && rule.selector.equals(level.dimension().identifier())) {
                    return rule.caps.get(track);
                }
                if (rule.tag && level.dimensionTypeRegistration().is(TagKey.create(Registries.DIMENSION_TYPE, rule.selector))
                        && (chosen == null || rule.priority > chosen.priority)) { chosen = rule; }
            }
            return chosen == null ? fallback.getOrDefault(track, maximum(track)) : chosen.caps.get(track);
        }
        public long reward(LivingEntity target) {
            if (target instanceof Player) { return 0; }
            var type = target.getType();
            var exact = entityRewards.get(type.builtInRegistryHolder().key().identifier());
            if (exact != null) { return exact; }
            RewardRule selected = null;
            for (var rule : rewardTags) {
                if (type.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, rule.tag))
                        && (selected == null || rule.priority > selected.priority)) { selected = rule; }
            }
            if (selected != null) { return selected.xp; }
            return target instanceof Enemy && !(target instanceof NeutralMob) ? hostileReward : 0;
        }
    }

    public record RewardRule(Identifier tag, int priority, long xp) {}
    public record DimensionRule(Identifier selector, boolean tag, int priority, Map<Identifier, Integer> caps) {
        public DimensionRule { caps = Map.copyOf(caps); }
    }

    /** Strict, bounded input validation. Additional professions/content can be added without a reward engine rewrite. */
    public static Balance parse(JsonObject json) {
        var caps = caps(json.getAsJsonObject("track_caps"));
        if (!caps.containsKey(CHARACTER) || !caps.keySet().containsAll(PROFESSIONS)) {
            throw new IllegalArgumentException("track_caps must include character and all professions");
        }
        var character = requirements(json.getAsJsonArray("character_requirements"));
        var profession = requirements(json.getAsJsonArray("profession_requirements"));
        if (character.size() < caps.get(CHARACTER) - 1) { throw new IllegalArgumentException("Character curve does not cover its cap"); }
        int largestProfession = XP_PROFESSIONS.stream().mapToInt(caps::get).max().orElseThrow();
        if (!profession.isEmpty() && profession.size() < largestProfession) {
            throw new IllegalArgumentException("Profession curve does not cover profession caps");
        }
        long hostile = nonnegative(json.get("hostile_reward"), "hostile_reward");
        var rewards = new HashMap<Identifier, Long>();
        for (var entry : json.getAsJsonObject("entity_rewards").entrySet()) {
            rewards.put(Identifier.parse(entry.getKey()), nonnegative(entry.getValue(), entry.getKey()));
        }
        var rewardTags = new ArrayList<RewardRule>();
        var rewardPriorities = new HashSet<Integer>();
        for (var element : json.getAsJsonArray("reward_tags")) {
            var rule = element.getAsJsonObject();
            int priority = integer(rule.get("priority"));
            if (!rewardPriorities.add(priority)) { throw new IllegalArgumentException("Duplicate reward tag priority: " + priority); }
            rewardTags.add(new RewardRule(Identifier.parse(rule.get("tag").getAsString()), priority, nonnegative(rule.get("xp"), "tag xp")));
        }
        var fallback = checkedCaps(json.getAsJsonObject("fallback_caps"), caps);
        var dimensions = new ArrayList<DimensionRule>();
        var selectors = new HashSet<String>();
        var priorities = new HashSet<Integer>();
        for (var element : json.getAsJsonArray("dimensions")) {
            var rule = element.getAsJsonObject();
            String selector = rule.get("match").getAsString();
            if (!selectors.add(selector)) { throw new IllegalArgumentException("Duplicate dimension rule: " + selector); }
            boolean tag = selector.startsWith("#");
            int priority = integer(rule.get("priority"));
            if (tag && !priorities.add(priority)) { throw new IllegalArgumentException("Duplicate dimension tag priority: " + priority); }
            dimensions.add(new DimensionRule(Identifier.parse(tag ? selector.substring(1) : selector), tag, priority,
                    checkedCaps(rule.getAsJsonObject("caps"), caps)));
        }
        if (rewards.size() > 4096 || rewardTags.size() > 256 || dimensions.size() > 1024) {
            throw new IllegalArgumentException("Too many progression rules");
        }
        return new Balance(caps, character, profession, hostile, rewards, rewardTags, dimensions, fallback);
    }

    private static Map<Identifier, Integer> caps(JsonObject object) {
        var result = new HashMap<Identifier, Integer>();
        for (var entry : object.entrySet()) {
            var key = Identifier.parse(entry.getKey());
            if (!key.equals(CHARACTER) && !PROFESSIONS.contains(key)) { throw new IllegalArgumentException("Unknown track: " + key); }
            int value = integer(entry.getValue());
            if (value < (key.equals(CHARACTER) ? 1 : 0) || value > 1000) { throw new IllegalArgumentException("Invalid cap: " + key); }
            result.put(key, value);
        }
        return result;
    }
    private static Map<Identifier, Integer> checkedCaps(JsonObject object, Map<Identifier, Integer> global) {
        var result = caps(object);
        result.forEach((id, cap) -> { if (cap > global.get(id)) { throw new IllegalArgumentException("Gate exceeds global cap: " + id); } });
        return result;
    }
    private static List<Long> requirements(JsonArray array) {
        if (array.size() > 1000) { throw new IllegalArgumentException("XP curve is too long"); }
        var result = new ArrayList<Long>();
        for (var entry : array) {
            long xp = nonnegative(entry, "XP requirement");
            if (xp == 0) { throw new IllegalArgumentException("XP requirements must be positive"); }
            result.add(xp);
        }
        return result;
    }
    private static long nonnegative(JsonElement value, String name) {
        long result = value.getAsBigDecimal().longValueExact();
        if (result < 0) { throw new IllegalArgumentException(name + " must be nonnegative"); }
        return result;
    }
    private static int integer(JsonElement value) { return value.getAsBigDecimal().intValueExact(); }
}
