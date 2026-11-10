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

/** Rank 1 server tuning; invalid reloads leave the retained live balance intact. */
public final class HunterPetData extends SimplePreparableReloadListener<HunterPetData.Balance> {
    public static final Identifier FILE = Identifier.fromNamespaceAndPath("covenant", "skills/hunters_pet.json");
    private static final ListenerKey<HunterPetData> KEY = ListenerKey.create(Identifier.fromNamespaceAndPath("covenant", "hunters_pet"));
    private Balance balance;

    /** Focus cost (default 35, 0–10000); defeat/dismissal cooldown in ticks (600, 0–72000);
     * health points (40, 1–1024); native attack damage (3, 0–2048);
     * interval between melee attempts in ticks (30, 1–72000).
     * Values snapshot on summon. Pet attributes are independent of owner equipment. */
    public record Balance(double cost, int cooldown, double health, double damage, int attackInterval) {}

    public static void register(AddServerReloadListenersEvent event) { event.addRetainedListener(KEY, new HunterPetData()); }
    public static Balance get(MinecraftServer server) { return server.getServerResources().managers().getListener(KEY).balance; }

    static SkillDefinition resolve(ServerPlayer player, SkillDefinition skill) {
        if (skill.effect() != SkillDefinition.Effect.HUNTERS_PET) { return skill; }
        var b = get(player.level().getServer());
        return new SkillDefinition(skill.id(), skill.allowedClasses(), b.cost, b.cooldown, 0, skill.effect(), 0, 0);
    }

    @Override protected Balance prepare(ResourceManager resources, ProfilerFiller profiler) {
        try (var reader = resources.getResourceOrThrow(FILE).openAsReader()) { return parse(JsonParser.parseReader(reader).getAsJsonObject()); }
        catch (IOException | RuntimeException error) { throw new IllegalArgumentException("Invalid " + FILE + ": " + error.getMessage(), error); }
    }
    @Override protected void apply(Balance prepared, ResourceManager resources, ProfilerFiller profiler) { balance = prepared; }

    public static Balance parse(JsonObject root) {
        return new Balance(number(root, "cost", 0, 10000), integer(root, "cooldown_ticks", 0, 72000),
                number(root, "health", 1, 1024), number(root, "damage", 0, 2048), integer(root, "attack_interval_ticks", 1, 72000));
    }
    private static double number(JsonObject root, String key, double min, double max) {
        var value = root.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) { throw new IllegalArgumentException("Missing numeric " + key); }
        double n = value.getAsDouble();
        if (!Double.isFinite(n) || n < min || n > max) { throw new IllegalArgumentException("Out of bounds: " + key); }
        return n;
    }
    private static int integer(JsonObject root, String key, int min, int max) {
        double n = number(root, key, min, max);
        if (n != Math.rint(n)) { throw new IllegalArgumentException("Expected whole ticks: " + key); }
        return (int) n;
    }
}
