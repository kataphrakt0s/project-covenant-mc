package dev.covenant.rpg.skills;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.ListenerKey;

/** Server datapack tuning. Costs are Mana, times are ticks, distances are blocks, bonuses are fractions.
 * Bounds are enforced at reload; an invalid file never replaces the live balance. */
public final class WeaverSkillData extends SimplePreparableReloadListener<WeaverSkillData.Balance> {
    public static final Identifier FILE = Identifier.fromNamespaceAndPath("covenant", "skills/weaver.json");
    private static final ListenerKey<WeaverSkillData> KEY = ListenerKey.create(Identifier.fromNamespaceAndPath("covenant", "weaver_skills"));
    private Balance balance;

    /** Each element has its own charge cap; max-charge riders use this same threshold. */
    public record Discharge(double cost, int cooldown, int cast, double radius, int stacks, double damage,
            double slow, int slowTicks, int freezeTicks, int burnTicks, double explosion,
            double chainRange, int chainTargets, double chainMultiplier) {}
    /** A fixed 200-tick rolling window; fraction goes independently into each other elemental reserve. */
    public record Threefold(double cost, int cooldown, double fraction, double capacity, double bonus, int duration) {}
    /** Instant blink; a displacement shorter than 0.1 blocks is rejected without payment. */
    public record Teleport(double cost, int cooldown, double range) {}
    public record Balance(Discharge discharge, Threefold threefold, Teleport teleport) {}

    public static void register(AddServerReloadListenersEvent event) { event.addRetainedListener(KEY, new WeaverSkillData()); }
    public static Balance get(MinecraftServer server) { return server.getServerResources().managers().getListener(KEY).balance; }

    static SkillDefinition resolve(ServerPlayer player, SkillDefinition definition) {
        if (definition != SkillDefinition.DISCHARGE && definition != SkillDefinition.THREEFOLD_SYSTEM && definition != SkillDefinition.TELEPORT) { return definition; }
        var b = get(player.level().getServer());
        double cost = definition == SkillDefinition.DISCHARGE ? b.discharge.cost : definition == SkillDefinition.TELEPORT ? b.teleport.cost : b.threefold.cost;
        int cooldown = definition == SkillDefinition.DISCHARGE ? b.discharge.cooldown : definition == SkillDefinition.TELEPORT ? b.teleport.cooldown : b.threefold.cooldown;
        return new SkillDefinition(definition.id(), definition.allowedClasses(), cost, cooldown,
                definition == SkillDefinition.DISCHARGE ? b.discharge.cast : 0, definition.effect(), 0,
                definition == SkillDefinition.DISCHARGE ? b.discharge.radius : definition == SkillDefinition.TELEPORT ? b.teleport.range : 0);
    }

    @Override protected Balance prepare(ResourceManager resources, ProfilerFiller profiler) {
        try (var reader = resources.getResourceOrThrow(FILE).openAsReader()) { return parse(JsonParser.parseReader(reader).getAsJsonObject()); }
        catch (IOException | RuntimeException error) { throw new IllegalArgumentException("Invalid " + FILE + ": " + error.getMessage(), error); }
    }
    @Override protected void apply(Balance prepared, ResourceManager resources, ProfilerFiller profiler) { balance = prepared; }

    public static Balance parse(JsonObject root) {
        var d = root.getAsJsonObject("discharge"); var t = root.getAsJsonObject("threefold_system"); var p = root.getAsJsonObject("teleport");
        return new Balance(new Discharge(n(d,"cost",0,10000), i(d,"cooldown_ticks",0,72000), i(d,"cast_ticks",0,72000),
                n(d,"radius",.1,16), i(d,"max_stacks",1,32), n(d,"damage_per_stack",0,10000), n(d,"slow",0,1),
                i(d,"slow_ticks",1,72000), i(d,"freeze_ticks",1,72000), i(d,"burn_ticks",1,72000), n(d,"explosion_damage",0,10000),
                n(d,"chain_range",.1,16), i(d,"chain_targets",0,16), n(d,"chain_multiplier",0,1)),
                new Threefold(n(t,"cost",0,10000), i(t,"cooldown_ticks",0,72000), n(t,"stored_fraction",0,1),
                        n(t,"capacity",.01,10000), n(t,"max_bonus",0,10), i(t,"duration_ticks",1,72000)),
                new Teleport(n(p,"cost",0,10000), i(p,"cooldown_ticks",0,72000), n(p,"range",.1,32)));
    }
    private static double n(JsonObject object, String key, double min, double max) {
        var e = object == null ? null : object.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) { throw new IllegalArgumentException("Missing numeric " + key); }
        double value = e.getAsDouble();
        if (!Double.isFinite(value) || value < min || value > max) { throw new IllegalArgumentException("Out of bounds: " + key); }
        return value;
    }
    private static int i(JsonObject object, String key, int min, int max) {
        double value = n(object,key,min,max);
        if (value != Math.rint(value)) { throw new IllegalArgumentException("Expected whole ticks/count: " + key); }
        return (int) value;
    }
}
