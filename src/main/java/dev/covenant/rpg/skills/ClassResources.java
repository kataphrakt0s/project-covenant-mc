package dev.covenant.rpg.skills;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.stats.RpgStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Shared bounded resource operations. Only the selected class recovers or depletes. */
public final class ClassResources {
    public static final int COMBAT_GRACE_TICKS = 140;
    private ClassResources() {}

    public static long now(ServerPlayer player) {
        return player.level().getServer().overworld().getGameTime();
    }

    public static void requireServer(ServerPlayer player) {
        if (!player.level().getServer().isSameThread()) {
            throw new IllegalStateException("Skill mutations require the logical server thread");
        }
    }

    public static String name(PlayerClass type) {
        return switch (type) { case STALKER -> "Focus"; case REAVER -> "Rage"; case WEAVER -> "Mana"; };
    }

    public static double maximum(Player player, PlayerClass type) {
        return switch (type) {
            case STALKER -> 100;
            case REAVER -> SkillConfig.RAGE_CAPACITY.get() * player.getAttributeValue(RpgStats.RAGE_CAPACITY);
            case WEAVER -> SkillConfig.MANA_CAPACITY.get() * player.getAttributeValue(RpgStats.MANA_CAPACITY);
        };
    }

    public static double regeneration(Player player, PlayerClass type) {
        return switch (type) {
            case STALKER -> SkillConfig.FOCUS_REGEN.get() * Math.min(1.25, player.getAttributeValue(RpgStats.FOCUS_REGENERATION));
            case REAVER -> SkillConfig.RAGE_GENERATION.get() * player.getAttributeValue(RpgStats.RAGE_GENERATION);
            case WEAVER -> SkillConfig.MANA_REGEN.get() * player.getAttributeValue(RpgStats.MANA_REGENERATION);
        };
    }

    /** Client reads existing owner-synced balances; first use starts Focus/Mana full, Rage empty. */
    public static double current(Player player, PlayerClass type) {
        var state = player.getData(RpgData.SKILLS);
        double maximum = maximum(player, type);
        var saved = state.balances().get(type.id());
        if (saved != null) { return Math.clamp(saved, 0, maximum); }
        double initial = type == PlayerClass.REAVER ? 0 : maximum;
        // One-time import of the old active-resource storage, when its ID matches this class.
        var legacy = PlayerProfiles.get(player);
        String resourceId = switch (type) { case STALKER -> "focus"; case REAVER -> "rage"; case WEAVER -> "mana"; };
        if (legacy.selectedClass().filter(type.id()::equals).isPresent()
                && legacy.resourceType().filter(id -> id.toString().equals("covenant:" + resourceId)).isPresent()) {
            initial = legacy.currentResource();
        }
        return Math.clamp(initial, 0, maximum);
    }

    /** Trusted debug/progression hook; changing a maximum never fills a previously initialized balance. */
    public static void set(ServerPlayer player, PlayerClass type, double amount) {
        requireServer(player);
        if (!Double.isFinite(amount) || amount < 0) { throw new IllegalArgumentException("Invalid resource amount"); }
        var previous = player.getData(RpgData.SKILLS);
        double bounded = Math.min(amount, maximum(player, type));
        if (previous.balances().containsKey(type.id()) && previous.balances().get(type.id()) == bounded) { return; }
        var next = previous.withBalance(type.id(), bounded);
        if (!previous.equals(next)) { player.setData(RpgData.SKILLS, next); }
    }

    public static boolean consume(ServerPlayer player, double cost) {
        requireServer(player);
        if (!Double.isFinite(cost) || cost < 0) { throw new IllegalArgumentException("Invalid resource cost"); }
        var type = PlayerProfiles.getSelectedClass(player).orElse(null);
        if (type == null || current(player, type) < cost) { return false; }
        set(player, type, current(player, type) - cost);
        return true;
    }

    public static boolean inCombat(ServerPlayer player) {
        return now(player) < player.getData(RpgData.SKILL_RUNTIME).combatUntil;
    }

    /** Four pulses/second. Timed generation avoids event-count/area-hit exploits and bounds sync traffic. */
    static void tick(ServerPlayer player, PlayerClass type) {
        // After native equipment updates: initialize missing balances and discard excess capacity,
        // including on inactive classes, without recovering or depleting their resources.
        var previous = player.getData(RpgData.SKILLS);
        var next = previous;
        double delta = type == PlayerClass.REAVER && !inCombat(player)
                ? -SkillConfig.RAGE_DEPLETION.get() : regeneration(player, type);
        for (var other : PlayerClass.all()) {
            double amount = current(player, other);
            if (other == type) { amount = Math.clamp(amount + delta / 4, 0, maximum(player, other)); }
            if (!previous.balances().containsKey(other.id()) || previous.balances().get(other.id()) != amount) {
                next = next.withBalance(other.id(), amount);
            }
        }
        if (next != previous) { player.setData(RpgData.SKILLS, next); }
    }

    /** Only positive confirmed damage involving another living actor counts, never self/environment hits. */
    public static void damaged(LivingDamageEvent.Post event) {
        if (!(event.getInflictedDamage() > 0) || !(event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker)
                || attacker == event.getEntity() || attacker.isAlliedTo(event.getEntity())) { return; }
        if (attacker instanceof ServerPlayer player) { markCombat(player); }
        if (event.getEntity() instanceof ServerPlayer player) { markCombat(player); }
    }

    private static void markCombat(ServerPlayer player) {
        requireServer(player);
        if (PlayerProfiles.getSelectedClass(player).orElse(null) == PlayerClass.REAVER && player.isAlive()) {
            player.getData(RpgData.SKILL_RUNTIME).combatUntil = now(player) + COMBAT_GRACE_TICKS;
        }
    }
}
