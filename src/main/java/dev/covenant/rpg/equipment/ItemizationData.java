package dev.covenant.rpg.equipment;

import com.google.gson.*;
import dev.covenant.rpg.progression.ProfessionData;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.ListenerKey;

/** Bounded server datapack catalogue. Saved rolled values are snapshots, never rerolled on reload. */
public final class ItemizationData extends SimplePreparableReloadListener<ItemizationData.Definitions> {
    public static final Identifier FILE = id("itemization/definitions.json");
    private static final ListenerKey<ItemizationData> KEY = ListenerKey.create(id("itemization"));
    private static final AtomicLong REVISIONS = new AtomicLong();
    private Snapshot snapshot;
    public static Identifier id(String path) { return ProfessionData.id(path); }
    public static void register(AddServerReloadListenersEvent event) { event.addRetainedListener(KEY, new ItemizationData()); }
    public static Snapshot snapshot(MinecraftServer server) { return server.getServerResources().managers().getListener(KEY).snapshot; }
    public static Definitions get(MinecraftServer server) { return snapshot(server).definitions(); }
    public record Snapshot(long revision, Definitions definitions) {}
    /** Capacities, numerical tier ceiling, starting Potential, sample count, magnitude, costs and XP. */
    public record Rarity(Identifier id, int order, int prefixes, int suffixes, int tier, int potential,
            int samples, double magnitude, int addCost, int rerollCost, long operationXp, long salvageXp, double weight) {}
    public record Range(double min, double max) {}
    /** Empty allowlists permit all participating gear; populated item/tag/category selectors form a union. */
    public record Selector(List<Identifier> items, List<Identifier> tags, List<Identifier> categories) {
        boolean matches(ItemStack stack, EquipmentCategory category) {
            return items.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                    || tags.stream().anyMatch(id -> stack.is(TagKey.create(Registries.ITEM, id)))
                    || category != null && categories.contains(category.id());
        }
        boolean unrestricted() { return items.isEmpty() && tags.isEmpty() && categories.isEmpty(); }
    }
    public record Family(Identifier id, boolean prefix, Identifier stat, RpgModifier.Operation operation,
            boolean reduction, List<Range> ranges, Selector allowed) {
        public boolean supports(ItemStack stack, EquipmentCategory category) { return allowed.unrestricted() || allowed.matches(stack, category); }
    }
    public record Participation(EquipmentCategory category, Selector selector) {}
    public record Refund(Selector selector, Identifier output, int count) {}
    /** All probabilities are fractions; componentRate is expected components per eligible affix. */
    public record Settings(int potentialCost, int commonAdd, int commonReroll, double genericChance,
            double componentRate, double affixChance, double mobBonusChance, int chestBonus,
            List<Double> recovery, List<Double> focusedRecovery, List<Integer> tierLevels) {}
    public record Definitions(List<Rarity> rarities, List<Family> families, List<Participation> participation,
            List<Refund> refunds, List<Identifier> bonusPool, Settings settings) {
        public Optional<EquipmentCategory> category(ItemStack stack) {
            return participation.stream().filter(p -> p.selector.matches(stack, p.category)).map(Participation::category).findFirst();
        }
        public Rarity rarity(Identifier id) { return rarities.stream().filter(r -> r.id.equals(id)).findFirst().orElse(null); }
        public Family family(Identifier id) { return families.stream().filter(f -> f.id.equals(id)).findFirst().orElse(null); }
        public Refund refund(ItemStack stack) { return refunds.stream().filter(r -> r.selector.matches(stack, null)).findFirst().orElse(null); }
    }
    public static Definitions parse(JsonObject root) {
        var rarities = new ArrayList<Rarity>(); var families = new ArrayList<Family>();
        var participation = new ArrayList<Participation>(); var refunds = new ArrayList<Refund>();
        var ids = new HashSet<Identifier>(); var orders = new HashSet<Integer>();
        for (var e : array(root, "rarities", 32)) {
            var j = e.getAsJsonObject(); var id = identifier(j, "id"); int order = integer(j, "order", 0, 1024);
            if (!ids.add(id) || !orders.add(order)) { throw new IllegalArgumentException("Duplicate rarity ID/order"); }
            rarities.add(new Rarity(id, order, integer(j, "prefixes", 0, 32), integer(j, "suffixes", 0, 32),
                    integer(j, "tier", 1, 64), integer(j, "potential", 0, 1_000_000), integer(j, "samples", 1, 32),
                    number(j, "magnitude", .001, 16), integer(j, "add_cost", 0, 64), integer(j, "reroll_cost", 0, 64),
                    integer(j, "operation_xp", 0, 1_000_000), integer(j, "salvage_xp", 0, 1_000_000), number(j, "weight", 0, 1_000_000)));
        }
        if (rarities.stream().mapToDouble(Rarity::weight).sum() <= 0 || !ids.contains(id("common"))) {
            throw new IllegalArgumentException("Rarities need positive total weight and covenant:common");
        }
        rarities.sort(Comparator.comparingInt(Rarity::order)); ids.clear();
        int maxTier = rarities.stream().mapToInt(Rarity::tier).max().orElseThrow();
        for (var e : array(root, "families", 64)) {
            var j = e.getAsJsonObject(); var id = identifier(j, "id"); var stat = identifier(j, "stat");
            if (!ids.add(id) || !BuiltInRegistries.ATTRIBUTE.containsKey(stat)) { throw new IllegalArgumentException("Duplicate family or unknown attribute: " + id); }
            var ranges = new ArrayList<Range>();
            for (var r : array(j, "ranges", 64)) {
                var range = r.getAsJsonObject(); double min = number(range, "min", 0, 1024), max = number(range, "max", min, 1024);
                ranges.add(new Range(min, max));
            }
            if (ranges.size() < maxTier) { throw new IllegalArgumentException("Missing family tier ranges: " + id); }
            families.add(new Family(id, j.get("prefix").getAsBoolean(), stat,
                    RpgModifier.Operation.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, j.get("operation")).getOrThrow(),
                    j.get("reduction").getAsBoolean(), List.copyOf(ranges), selector(j)));
        }
        for (var e : array(root, "participation", 64)) {
            var j = e.getAsJsonObject(); var category = new EquipmentCategory(identifier(j, "category"));
            var selector = selector(j);
            if (selector.unrestricted()) { throw new IllegalArgumentException("Empty equipment participation rule"); }
            participation.add(new Participation(category, selector));
        }
        for (var e : root.getAsJsonArray("refunds")) {
            if (refunds.size() >= 256) { throw new IllegalArgumentException("Too many refund rules"); }
            var j = e.getAsJsonObject(); var output = identifier(j, "output"); requireItem(output);
            var selector = selector(j);
            if (selector.unrestricted()) { throw new IllegalArgumentException("Empty refund selector"); }
            refunds.add(new Refund(selector, output, integer(j, "count", 1, 64)));
        }
        var pool = identifiers(root, "bonus_pool");
        if (pool.isEmpty()) { throw new IllegalArgumentException("Empty bonus equipment pool"); }
        pool.forEach(ItemizationData::requireItem);
        var j = root.getAsJsonObject("settings");
        var recovery = numbers(j, "recovery", 1); var focused = numbers(j, "focused_recovery", 16);
        if (recovery.size() != 3 || focused.size() != 3) { throw new IllegalArgumentException("Expected three specialization ranks"); }
        var levels = new ArrayList<Integer>();
        for (var e : array(j, "tier_levels", 64)) {
            int n = e.getAsBigDecimal().intValueExact();
            if (n < 1 || n > 1000 || !levels.isEmpty() && n < levels.getLast()) { throw new IllegalArgumentException("Invalid tier gates"); }
            levels.add(n);
        }
        if (levels.size() < maxTier) { throw new IllegalArgumentException("Missing crafting tier gates"); }
        var settings = new Settings(integer(j, "potential_cost", 1, 1_000_000), integer(j, "common_add", 1, 64),
                integer(j, "common_reroll", 1, 64), number(j, "generic_chance", 0, 1), number(j, "component_rate", 0, 16),
                number(j, "affix_chance", 0, 1), number(j, "mob_bonus_chance", 0, 1), integer(j, "chest_bonus", 0, 16),
                List.copyOf(recovery), List.copyOf(focused), List.copyOf(levels));
        return new Definitions(List.copyOf(rarities), List.copyOf(families), List.copyOf(participation), List.copyOf(refunds), pool, settings);
    }
    private static List<Double> numbers(JsonObject j, String key, double max) {
        var list = new ArrayList<Double>(); for (var e : array(j, key, 3)) { double n = e.getAsDouble();
            if (!Double.isFinite(n) || n < 0 || n > max) { throw new IllegalArgumentException("Invalid " + key); } list.add(n); }
        return list;
    }
    private static Selector selector(JsonObject j) { return new Selector(identifiers(j, "items"), identifiers(j, "tags"), identifiers(j, "categories")); }
    private static List<Identifier> identifiers(JsonObject j, String key) {
        if (!j.has(key)) { return List.of(); } var list = new ArrayList<Identifier>();
        var values = j.getAsJsonArray(key);
        if (values.size() > 1024) { throw new IllegalArgumentException("Too many " + key); }
        for (var e : values) { list.add(Identifier.parse(e.getAsString())); } return List.copyOf(list);
    }
    private static JsonArray array(JsonObject j, String key, int max) {
        var array = j.getAsJsonArray(key); if (array == null || array.isEmpty() || array.size() > max) { throw new IllegalArgumentException("Invalid " + key + " list"); } return array;
    }
    private static Identifier identifier(JsonObject j, String key) { return Identifier.parse(j.get(key).getAsString()); }
    private static int integer(JsonObject j, String key, int min, int max) {
        int n = j.get(key).getAsBigDecimal().intValueExact(); if (n < min || n > max) { throw new IllegalArgumentException("Invalid " + key); } return n;
    }
    private static double number(JsonObject j, String key, double min, double max) {
        double n = j.get(key).getAsDouble(); if (!Double.isFinite(n) || n < min || n > max) { throw new IllegalArgumentException("Invalid " + key); } return n;
    }
    private static void requireItem(Identifier id) {
        if (!BuiltInRegistries.ITEM.containsKey(id) || id.equals(Identifier.withDefaultNamespace("air"))) { throw new IllegalArgumentException("Unknown/empty item: " + id); }
    }
    @Override protected Definitions prepare(ResourceManager resources, ProfilerFiller profiler) {
        try (var reader = resources.getResourceOrThrow(FILE).openAsReader()) { return parse(JsonParser.parseReader(reader).getAsJsonObject()); }
        catch (java.io.IOException | RuntimeException e) { throw new IllegalArgumentException("Invalid " + FILE, e); }
    }
    @Override protected void apply(Definitions definitions, ResourceManager resources, ProfilerFiller profiler) {
        snapshot = new Snapshot(REVISIONS.incrementAndGet(), definitions);
    }
}
