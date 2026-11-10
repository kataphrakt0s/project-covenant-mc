package dev.covenant.rpg.skills;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.stats.RpgStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;

/** Server-only elemental charges and fixed-size rolling reserves, driven by cast/damage events.
 * No saves, death copy, entity scans or per-hit collection allocation. */
public final class WeaverPower {
    public enum Element { FIRE, ICE, LIGHTNING }
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> ICE_BOLT =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, SkillDefinition.ICE_BOLT.id());
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> LIGHTNING_BOLT =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, SkillDefinition.LIGHTNING_BOLT.id());
    /** Ten seconds at 20 TPS; individual tick contributions expire at age 200. */
    public static final int WINDOW = 200;
    static final class State {
        final int[] charges = new int[3];
        final double[] bonus = new double[3];
        Reserves reserves;
        long until;
    }
    private static final class Reserves {
        final double[][] buckets = new double[3][WINDOW];
        final long[] stamps = new long[WINDOW];
        final double[] totals = new double[3];
        long lastExpiry = Long.MIN_VALUE;
        Reserves() { java.util.Arrays.fill(stamps, Long.MIN_VALUE); }
        void expire(long now) {
            if (lastExpiry == now) { return; }
            lastExpiry = now;
            for (int slot = 0; slot < WINDOW; slot++) {
                if (stamps[slot] == Long.MIN_VALUE || now - stamps[slot] < WINDOW) { continue; }
                for (int e = 0; e < 3; e++) { totals[e] = Math.max(0, totals[e] - buckets[e][slot]); buckets[e][slot] = 0; }
                stamps[slot] = Long.MIN_VALUE;
            }
        }
    }
    private WeaverPower() {}
    private static State state(ServerPlayer player) { return player.getData(RpgData.SKILL_RUNTIME).weaver; }
    private static State create(ServerPlayer player) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        if (runtime.weaver == null) { runtime.weaver = new State(); }
        return runtime.weaver;
    }
    private static boolean eligible(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && PlayerProfiles.getSelectedClass(player).orElse(null) == PlayerClass.WEAVER;
    }
    public static boolean assigned(ServerPlayer player) {
        var loadout = player.getData(RpgData.SKILL_LOADOUT);
        return eligible(player) && loadout.learned().contains(SkillDefinition.THREEFOLD_SYSTEM.id())
                && loadout.bindings().containsValue(SkillDefinition.THREEFOLD_SYSTEM.id());
    }
    public static Element element(SkillDefinition skill) {
        if (skill.id().equals(SkillDefinition.FIRE_BOLT.id())) { return Element.FIRE; }
        if (skill.id().equals(SkillDefinition.ICE_BOLT.id())) { return Element.ICE; }
        return skill.id().equals(SkillDefinition.LIGHTNING_BOLT.id()) ? Element.LIGHTNING : null;
    }
    /** Recognized elemental Covenant types plus attributed fire-tagged damage; arcane has no reserve. */
    public static Element element(DamageSource source) {
        if (source.is(Discharge.ICE) || source.is(ICE_BOLT)) { return Element.ICE; }
        if (source.is(Discharge.LIGHTNING) || source.is(LIGHTNING_BOLT)) { return Element.LIGHTNING; }
        return source.is(DamageTypeTags.IS_FIRE) ? Element.FIRE : null;
    }
    /** A completed paid elemental cast grants one charge even when its projectile later misses. */
    static void castCompleted(ServerPlayer player, SkillDefinition skill) {
        var element = element(skill);
        if (element == null || !eligible(player) || !player.getData(RpgData.SKILL_LOADOUT).learned().contains(SkillDefinition.DISCHARGE.id())) { return; }
        var s = create(player); int cap = WeaverSkillData.get(player.level().getServer()).discharge().stacks();
        s.charges[element.ordinal()] = Math.min(cap, s.charges[element.ordinal()] + 1);
        sync(player);
    }
    public static int charges(ServerPlayer player, Element element) {
        var s = state(player);
        return s == null ? 0 : Math.min(WeaverSkillData.get(player.level().getServer()).discharge().stacks(), s.charges[element.ordinal()]);
    }
    static boolean hasCharges(ServerPlayer player) {
        var s = state(player); return s != null && (s.charges[0] > 0 || s.charges[1] > 0 || s.charges[2] > 0);
    }
    static int[] consumeCharges(ServerPlayer player) {
        int[] result = { charges(player, Element.FIRE), charges(player, Element.ICE), charges(player, Element.LIGHTNING) };
        java.util.Arrays.fill(state(player).charges, 0); sync(player); return result;
    }
    /** Caller supplies actual health/absorption/Fortitude loss once, including lethal hits without overkill. */
    static void bank(ServerPlayer player, Element element, double damage) {
        if (element == null || !(damage > 0) || !Double.isFinite(damage) || !assigned(player) || active(player)) { return; }
        var s = create(player);
        if (s.reserves == null) { s.reserves = new Reserves(); }
        var r = s.reserves; long now = ClassResources.now(player); r.expire(now);
        var config = WeaverSkillData.get(player.level().getServer()).threefold();
        int slot = Math.floorMod(now, WINDOW); r.stamps[slot] = now;
        for (int e = 0; e < 3; e++) {
            if (e == element.ordinal()) { continue; }
            double add = Math.max(0, Math.min(config.capacity() - r.totals[e], damage * config.fraction()));
            r.totals[e] += add; r.buckets[e][slot] += add;
        }
    }
    public static double reserve(ServerPlayer player, Element element) {
        var s = state(player); if (s == null || s.reserves == null) { return 0; }
        s.reserves.expire(ClassResources.now(player));
        return Math.min(WeaverSkillData.get(player.level().getServer()).threefold().capacity(), s.reserves.totals[element.ordinal()]);
    }
    public static boolean active(ServerPlayer player) { var s = state(player); return s != null && s.until > ClassResources.now(player); }
    public static double bonus(net.minecraft.world.entity.LivingEntity owner, Element element) {
        if (!(owner instanceof ServerPlayer player) || element == null || !assigned(player) || !active(player)) { return 0; }
        return state(player).bonus[element.ordinal()];
    }
    static boolean canActivate(ServerPlayer player) {
        return assigned(player) && !active(player) && (reserve(player, Element.FIRE) + reserve(player, Element.ICE) + reserve(player, Element.LIGHTNING) > 1e-8);
    }
    static void activate(ServerPlayer player) {
        var s = state(player); var config = WeaverSkillData.get(player.level().getServer()).threefold();
        for (var e : Element.values()) { s.bonus[e.ordinal()] = config.bonus() * reserve(player, e) / config.capacity(); }
        s.reserves = null;
        s.until = ClassResources.now(player) + Math.max(1, (long) Math.ceil(RpgStats.scaleSkillValue(player, RpgStats.SKILL_DURATION, config.duration())));
        player.level().sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 24, .5, .7, .5, .03);
        sync(player);
    }
    static void clear(ServerPlayer player) {
        player.getData(RpgData.SKILL_RUNTIME).weaver = null;
        if (!player.getData(RpgData.WEAVER_DISPLAY).equals(WeaverDisplay.EMPTY)) { player.setData(RpgData.WEAVER_DISPLAY, WeaverDisplay.EMPTY); }
        BuffDisplay.sync(player);
    }
    static void reconcile(ServerPlayer player) {
        var s = state(player); if (s == null) { sync(player); return; }
        if (!eligible(player)) { clear(player); return; }
        if (!player.getData(RpgData.SKILL_LOADOUT).learned().contains(SkillDefinition.DISCHARGE.id())) { java.util.Arrays.fill(s.charges, 0); }
        if (!assigned(player)) { s.reserves = null; s.until = 0; java.util.Arrays.fill(s.bonus, 0); }
        sync(player);
    }
    /** Called at most four times per second from the existing player tick; only changed snapshots send. */
    static void sync(ServerPlayer player) {
        var view = WeaverDisplay.EMPTY;
        boolean discharge = eligible(player) && player.getData(RpgData.SKILL_LOADOUT).learned().contains(SkillDefinition.DISCHARGE.id());
        boolean threefold = assigned(player);
        if (discharge || threefold) {
            boolean buff = active(player); var config = WeaverSkillData.get(player.level().getServer());
            int f = 0, i = 0, l = 0;
            if (threefold) {
                f = (int) Math.round(buff ? bonus(player, Element.FIRE) * 100 : reserve(player, Element.FIRE) / config.threefold().capacity() * 100);
                i = (int) Math.round(buff ? bonus(player, Element.ICE) * 100 : reserve(player, Element.ICE) / config.threefold().capacity() * 100);
                l = (int) Math.round(buff ? bonus(player, Element.LIGHTNING) * 100 : reserve(player, Element.LIGHTNING) / config.threefold().capacity() * 100);
            }
            view = new WeaverDisplay(discharge, threefold, charges(player, Element.FIRE), charges(player, Element.ICE), charges(player, Element.LIGHTNING), config.discharge().stacks(), f, i, l, buff);
        }
        var previous = player.getData(RpgData.WEAVER_DISPLAY);
        if (!previous.equals(view)) { player.setData(RpgData.WEAVER_DISPLAY, view); }
        if (previous.buff() != view.buff()) { BuffDisplay.sync(player); }
    }
}
