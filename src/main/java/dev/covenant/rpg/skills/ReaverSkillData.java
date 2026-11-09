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

/** Server resource-scoped tuning for the three new Rank 1 skills; invalid reloads preserve live data. */
public final class ReaverSkillData extends SimplePreparableReloadListener<ReaverSkillData.Balance> {
    public static final Identifier FILE = Identifier.fromNamespaceAndPath("covenant", "skills/reaver.json");
    private static final ListenerKey<ReaverSkillData> KEY = ListenerKey.create(Identifier.fromNamespaceAndPath("covenant", "reaver_skills"));
    private Balance balance;

    /** Costs are Rage; times are ticks; radii, distance and speed are blocks (speed per tick); bonuses are fractions. */
    public record Cry(double cost, int cooldown, int duration, double radius, double bonus) {}
    /** Per-target hit cap counts attempts, including defended hits; damage uses native armor mitigation. */
    public record Storm(double cost, int cooldown, int cast, double speed, double distance,
            double radius, double damage, int interval, int hits) {}
    /** Distinct enemies, base-relative speed fractions, and periodic local-query interval in ticks. */
    public record Fever(double radius, int interval, double movement, double attack, int enemies) {}
    public record Balance(Cry cry, Storm storm, Fever fever) {}

    public static void register(AddServerReloadListenersEvent event) { event.addRetainedListener(KEY, new ReaverSkillData()); }
    public static Balance get(MinecraftServer server) { return server.getServerResources().managers().getListener(KEY).balance; }

    static SkillDefinition resolve(ServerPlayer player, SkillDefinition definition) {
        if (definition != SkillDefinition.BATTLE_CRY && definition != SkillDefinition.RAGE_STORM) { return definition; }
        var data = get(player.level().getServer());
        boolean cry = definition == SkillDefinition.BATTLE_CRY;
        return new SkillDefinition(definition.id(), definition.allowedClasses(), cry ? data.cry.cost : data.storm.cost,
                cry ? data.cry.cooldown : data.storm.cooldown, cry ? 0 : data.storm.cast, definition.effect(),
                cry ? data.cry.bonus : data.storm.damage, cry ? data.cry.radius : data.storm.distance);
    }

    @Override protected Balance prepare(ResourceManager resources, ProfilerFiller profiler) {
        try (var reader = resources.getResourceOrThrow(FILE).openAsReader()) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException error) {
            throw new IllegalArgumentException("Invalid " + FILE + ": " + error.getMessage(), error);
        }
    }
    @Override protected void apply(Balance prepared, ResourceManager resources, ProfilerFiller profiler) { balance = prepared; }

    public static Balance parse(JsonObject root) {
        var c = root.getAsJsonObject("battle_cry");
        var s = root.getAsJsonObject("rage_storm");
        var f = root.getAsJsonObject("blood_fever");
        return new Balance(new Cry(number(c, "cost", 0, 10000), integer(c, "cooldown_ticks", 0, 72000),
                integer(c, "duration_ticks", 1, 72000), number(c, "radius", .1, 32), number(c, "damage_bonus", 0, 10)),
                new Storm(number(s, "cost", 0, 10000), integer(s, "cooldown_ticks", 0, 72000), integer(s, "cast_ticks", 0, 72000),
                        number(s, "speed", .01, 4), number(s, "distance", .1, 64), number(s, "radius", .1, 8),
                        number(s, "damage", 0, 10000), integer(s, "pulse_ticks", 1, 200), integer(s, "max_hits", 1, 64)),
                new Fever(number(f, "radius", .1, 16), integer(f, "update_ticks", 1, 200), number(f, "movement_per_enemy", 0, 1),
                        number(f, "attack_per_enemy", 0, 1), integer(f, "max_enemies", 1, 32)));
    }
    private static double number(JsonObject object, String key, double min, double max) {
        var element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Missing numeric " + key);
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value) || value < min || value > max) { throw new IllegalArgumentException("Out of bounds: " + key); }
        return value;
    }
    private static int integer(JsonObject object, String key, int min, int max) {
        double value = number(object, key, min, max);
        if (value != Math.rint(value)) { throw new IllegalArgumentException("Expected whole ticks/count: " + key); }
        return (int) value;
    }
}
