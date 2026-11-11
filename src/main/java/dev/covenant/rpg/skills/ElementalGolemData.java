package dev.covenant.rpg.skills;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.ListenerKey;

/** Retained rank-one tuning. Invalid reloads keep the previous balance. */
public final class ElementalGolemData extends SimplePreparableReloadListener<ElementalGolemData.Balance> {
    public static final Identifier FILE = Identifier.fromNamespaceAndPath("covenant", "skills/elemental_golem.json");
    private static final ListenerKey<ElementalGolemData> KEY = ListenerKey.create(Identifier.fromNamespaceAndPath("covenant", "elemental_golem"));
    private Balance balance;
    /** Mana cost (default 40, [0,10000]), dismissal cooldown (600 ticks, [0,72000]),
     * health (60 HP, [1,1024]), physical damage (3 HP, [0,2048]) and attack interval
     * (40 ticks, [1,72000]) snapshot on summon. Owner range (12 blocks, [1,64])
     * and proportional bonuses update on reload: Fire damage .10, Ice cast/cooldown
     * reductions .20/.10, Lightning duration/Mana .20/.20, Arcane regeneration/cost
     * reduction .20/.10. Bonuses accept [0,1]; reductions accept [0,.99].
     * Repair is native iron-golem healing (25 HP/ingot).
     * Form cycling always takes five ticks and is unaffected by cooldown scaling. */
    public record Balance(double cost, int cooldown, double health, double damage, int attackInterval,
            double range, double fireDamage, double iceCast, double iceCooldown,
            double lightningDuration, double lightningMana, double arcaneRegen, double arcaneCost) {}
    public static void register(AddServerReloadListenersEvent event) { event.addRetainedListener(KEY, new ElementalGolemData()); }
    public static Balance get(MinecraftServer server) { return server.getServerResources().managers().getListener(KEY).balance; }
    static SkillDefinition resolve(ServerPlayer player, SkillDefinition skill) {
        if (skill.effect() != SkillDefinition.Effect.ELEMENTAL_GOLEM) { return skill; }
        var b = get(player.level().getServer());
        return new SkillDefinition(skill.id(), skill.allowedClasses(), b.cost, b.cooldown, 0, skill.effect(), 0, b.range);
    }
    @Override protected Balance prepare(ResourceManager resources, ProfilerFiller profiler) {
        try (var reader = resources.getResourceOrThrow(FILE).openAsReader()) { return parse(JsonParser.parseReader(reader).getAsJsonObject()); }
        catch (java.io.IOException | RuntimeException error) { throw new IllegalArgumentException("Invalid " + FILE, error); }
    }
    @Override protected void apply(Balance prepared, ResourceManager resources, ProfilerFiller profiler) { balance = prepared; }
    public static Balance parse(JsonObject root) {
        return new Balance(number(root,"cost",0,10000), integer(root,"cooldown_ticks",0,72000),
                number(root,"health",1,1024), number(root,"damage",0,2048), integer(root,"attack_interval_ticks",1,72000),
                number(root,"owner_range",1,64), number(root,"fire_spell_damage",0,1),
                number(root,"ice_cast_reduction",0,.99), number(root,"ice_cooldown_reduction",0,.99),
                number(root,"lightning_duration",0,1), number(root,"lightning_max_mana",0,1),
                number(root,"arcane_mana_regeneration",0,1), number(root,"arcane_cost_reduction",0,.99));
    }
    private static double number(JsonObject root, String key, double min, double max) {
        var value = root.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) { throw new IllegalArgumentException("Missing numeric " + key); }
        double n = value.getAsDouble();
        if (!Double.isFinite(n) || n < min || n > max) { throw new IllegalArgumentException("Out of bounds: " + key); }
        return n;
    }
    private static int integer(JsonObject root, String key, int min, int max) {
        double n = number(root,key,min,max);
        if (n != Math.rint(n)) { throw new IllegalArgumentException("Expected whole ticks: " + key); }
        return (int)n;
    }
}
