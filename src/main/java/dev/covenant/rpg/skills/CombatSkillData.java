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

/** Rank 1 server datapack values. Times are game ticks, distances blocks, damage/healing HP.
 * All fields are required and bounded; invalid reloads retain the previous immutable balance. */
public final class CombatSkillData extends SimplePreparableReloadListener<CombatSkillData.Balance> {
    private static final ListenerKey<CombatSkillData> KEY = ListenerKey.create(Identifier.fromNamespaceAndPath("covenant", "combat_skill_tuning"));
    private Balance balance;
    /** Focus per arrow, arrows per second (1..20), native arrow base coefficient (0..100). */
    public record Barrage(double cost, double rate, double coefficient) {}
    /** Mana, cooldown/cast/duration/hit/burn ticks, range/radii/height blocks, impacts/second, damage HP. */
    public record Firestorm(double cost, int cooldown, int cast, double range, double radius, int duration,
            double rate, double damage, double splash, int hitTicks, int burnTicks, double height) {}
    /** Rage, cooldown/lifetime ticks, spherical radius blocks, HP healed per second (0..100). */
    public record Banner(double cost, int cooldown, int duration, double radius, double healing) {}
    public record Balance(Barrage barrage, Firestorm firestorm, Banner banner) {}
    public static void register(AddServerReloadListenersEvent event) { event.addRetainedListener(KEY, new CombatSkillData()); }
    public static Balance get(MinecraftServer server) { return server.getServerResources().managers().getListener(KEY).balance; }
    static SkillDefinition resolve(ServerPlayer player, SkillDefinition skill) {
        var b = get(player.level().getServer());
        return switch (skill.effect()) {
            case CHANNEL -> new SkillDefinition(skill.id(), skill.allowedClasses(), b.barrage.cost, 0, 0, skill.effect(), b.barrage.coefficient / 2, 0);
            case FIRESTORM -> new SkillDefinition(skill.id(), skill.allowedClasses(), b.firestorm.cost, b.firestorm.cooldown, b.firestorm.cast, skill.effect(), b.firestorm.damage, b.firestorm.range);
            case WAR_BANNER -> new SkillDefinition(skill.id(), skill.allowedClasses(), b.banner.cost, b.banner.cooldown, 0, skill.effect(), b.banner.healing, b.banner.radius);
            default -> skill;
        };
    }
    @Override protected Balance prepare(ResourceManager resources, ProfilerFiller profiler) {
        var root = new JsonObject();
        for (String name : new String[] {"stalker", "weaver", "reaver"}) {
            var file = Identifier.fromNamespaceAndPath("covenant", "skills/" + name + ".json");
            try (var reader = resources.getResourceOrThrow(file).openAsReader()) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                String skill = name.equals("stalker") ? "barrage" : name.equals("weaver") ? "firestorm" : "war_banner";
                root.add(skill, json.get(skill));
            } catch (IOException | RuntimeException error) { throw new IllegalArgumentException("Invalid " + file + ": " + error.getMessage(), error); }
        }
        return parse(root);
    }
    @Override protected void apply(Balance prepared, ResourceManager resources, ProfilerFiller profiler) { balance = prepared; }
    public static Balance parse(JsonObject root) {
        var b = root.getAsJsonObject("barrage"); var f = root.getAsJsonObject("firestorm"); var w = root.getAsJsonObject("war_banner");
        return new Balance(new Barrage(n(b,"cost",0,10000), n(b,"arrows_per_second",1,20), n(b,"arrow_coefficient",0,100)),
                new Firestorm(n(f,"cost",0,10000), i(f,"cooldown_ticks",0,72000), i(f,"cast_ticks",0,72000),
                        n(f,"range",.1,32), n(f,"radius",.1,16), i(f,"duration_ticks",1,72000), n(f,"impacts_per_second",1,20),
                        n(f,"damage",0,10000), n(f,"splash_radius",.1,4), i(f,"hit_interval_ticks",1,1200), i(f,"burn_ticks",1,72000), n(f,"fall_height",.25,8)),
                new Banner(n(w,"cost",0,10000), i(w,"cooldown_ticks",0,72000), i(w,"duration_ticks",1,72000), n(w,"radius",.1,16), n(w,"healing_per_second",0,100)));
    }
    private static double n(JsonObject object, String key, double min, double max) {
        var e = object == null ? null : object.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) { throw new IllegalArgumentException("Missing numeric " + key); }
        double value = e.getAsDouble();
        if (!Double.isFinite(value) || value < min || value > max) { throw new IllegalArgumentException("Out of bounds: " + key); }
        return value;
    }
    private static int i(JsonObject o, String key, int min, int max) {
        double value = n(o,key,min,max);
        if (value != Math.rint(value)) { throw new IllegalArgumentException("Expected whole ticks: " + key); }
        return (int) value;
    }
}
