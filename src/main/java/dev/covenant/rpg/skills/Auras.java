package dev.covenant.rpg.skills;

import dev.covenant.Covenant;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.stats.RpgStats;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Transient source-owned coverage. Only assigned sources query space; recipients age their own grants. */
public final class Auras {
    /** Radius in blocks, refresh interval and passive grace in game ticks; none scale with duration. */
    public static final double RADIUS = 6;
    public static final int REFRESH_TICKS = 10, COVERAGE_TICKS = 20;

    public enum Kind {
        DODGE("dodge_aura", 60, false, false), SWIFTNESS("swiftness_aura", 80, false, false),
        RIPPING("ripping_aura", 100, false, false), FORTITUDE("fortitude_aura", 140, false, false),
        RAGING("raging_aura", 120, true, false), ICY_VEINS("icy_veins", 80, true, false),
        FLAME_CLOAK("flame_cloak", 160, true, false), FREEZING("freezing_aura", 30, false, true),
        SHOCK("shock_aura", 140, false, true), IMMOLATION("immolation_aura", 120, false, true),
        ENCHANTING("enchanting_aura", 120, false, false);
        public final Identifier id;
        final SkillDefinition definition;
        public final int duration;
        final boolean selfOnly, offensive;
        Kind(String name, int duration, boolean selfOnly, boolean offensive) {
            this.id = Identifier.fromNamespaceAndPath(Covenant.MOD_ID, name);
            this.definition = SkillDefinition.find(id).orElseThrow();
            this.duration = duration; this.selfOnly = selfOnly; this.offensive = offensive;
        }
        public static Kind of(SkillDefinition definition) {
            for (var kind : values()) { if (kind.id.equals(definition.id())) { return kind; } }
            throw new IllegalArgumentException("Not an aura: " + definition.id());
        }
    }

    /** No codec or death copy: grants and reverse references never enter player/entity saves. */
    public static final class State {
        final EnumMap<Kind, Map<UUID, Long>> passive = new EnumMap<>(Kind.class);
        final EnumMap<Kind, Map<UUID, Long>> active = new EnumMap<>(Kind.class);
        final EnumSet<Kind> assigned = EnumSet.noneOf(Kind.class);
        final Set<LivingEntity> recipients = new HashSet<>();
        UUID shieldOwner;
        long shieldUntil, nextImmolation = -1;
        float shield;
        long nextExpiry = Long.MAX_VALUE;
    }

    private Auras() {}
    public static void register() {
        NeoForge.EVENT_BUS.addListener(Auras::tick);
        NeoForge.EVENT_BUS.addListener(Auras::left);
        NeoForge.EVENT_BUS.addListener(Auras::died);
        NeoForge.EVENT_BUS.addListener(Auras::logout);
        NeoForge.EVENT_BUS.addListener(Auras::dimension);
        AuraDamage.register();
    }

    static long now(LivingEntity entity) {
        return entity.level() instanceof ServerLevel level ? level.getServer().overworld().getGameTime() : entity.level().getGameTime();
    }

    /** Single ally policy, including teammate-owned pets; future team integrations belong here. */
    public static boolean allied(LivingEntity caster, LivingEntity target) {
        if (caster == target || caster.isAlliedTo(target)) { return true; }
        if (target instanceof OwnableEntity pet && pet.getOwner() instanceof LivingEntity owner) {
            return owner == caster || caster.isAlliedTo(owner);
        }
        return false;
    }

    public static boolean assigned(ServerPlayer player, Kind kind) {
        var state = player.getData(RpgData.SKILL_LOADOUT);
        return player.isAlive() && !player.isSpectator() && state.learned().contains(kind.id)
                && state.bindings().containsValue(kind.id)
                && kind.definition.allowedClasses().contains(PlayerProfiles.getSelectedClass(player).orElse(null));
    }

    /** Called immediately by loadout mutations as well as player ticks; duplicate slots collapse here. */
    public static void reconcile(ServerPlayer player) {
        var state = player.hasData(RpgData.AURAS) ? player.getData(RpgData.AURAS) : null;
        for (var kind : Kind.values()) {
            boolean eligible = assigned(player, kind);
            if (!eligible && state != null && state.assigned.remove(kind)) { removeSource(player, kind); }
            if (eligible) {
                if (state == null) { state = player.getData(RpgData.AURAS); }
                if (state.assigned.add(kind) && kind.selfOnly) { grant(player, player, kind, false, Long.MAX_VALUE); }
            }
        }
    }

    private static List<LivingEntity> nearby(ServerPlayer source) {
        return source.level().getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(RADIUS),
                target -> target.isAlive() && !target.isSpectator() && source.distanceToSqr(target) <= RADIUS * RADIUS);
    }

    static void activate(ServerPlayer source, SkillDefinition definition) {
        var kind = Kind.of(definition);
        long until = now(source) + Math.max(1, (long) Math.ceil(RpgStats.scaleSkillValue(source, RpgStats.SKILL_DURATION, kind.duration)));
        boolean self = kind.selfOnly || kind == Kind.SHOCK || kind == Kind.IMMOLATION;
        for (var target : self ? List.<LivingEntity>of(source) : nearby(source)) {
            if (!self && !(kind.offensive ? Skills.canHit(source, target) : allied(source, target))) { continue; }
            if (kind == Kind.FREEZING && target.isInvulnerableTo(source.level(), source.damageSources().freeze())) { continue; }
            grant(source, target, kind, true, until);
            if (kind == Kind.SWIFTNESS) { cleanse(target); }
            if (kind == Kind.FREEZING) {
                target.stopUsingItem();
                if (target instanceof ServerPlayer player) {
                    player.getData(RpgData.SKILL_RUNTIME).cancelCast(player);
                    Combos.interruptMovement(player, false);
                }
            }
            source.level().sendParticles(kind == Kind.FREEZING ? ParticleTypes.SNOWFLAKE : ParticleTypes.ENCHANT,
                    target.getX(), target.getY() + 1, target.getZ(), 12, .4, .5, .4, .02);
        }
    }

    private static void grant(ServerPlayer source, LivingEntity target, Kind kind, boolean active, long until) {
        var state = target.getData(RpgData.AURAS);
        boolean wasPresent = has(target, kind, active);
        if (active && kind == Kind.FORTITUDE) {
            var old = state.shieldOwner;
            state.shieldOwner = source.getUUID(); state.shieldUntil = until; state.shield = 6;
            if (old != null && !old.equals(source.getUUID())) { unlink(target, old); }
        } else {
            (active ? state.active : state.passive).computeIfAbsent(kind, ignored -> new HashMap<>())
                    .merge(source.getUUID(), until, Math::max);
        }
        source.getData(RpgData.AURAS).recipients.add(target);
        state.nextExpiry = Math.min(state.nextExpiry, until);
        if (!wasPresent || (active && kind == Kind.FORTITUDE)) { update(target); }
    }

    public static boolean passive(LivingEntity target, Kind kind) { return has(target, kind, false); }
    public static boolean active(LivingEntity target, Kind kind) { return has(target, kind, true); }
    private static boolean has(LivingEntity target, Kind kind, boolean active) {
        if (!target.hasData(RpgData.AURAS)) { return false; }
        var grants = (active ? target.getData(RpgData.AURAS).active : target.getData(RpgData.AURAS).passive).get(kind);
        if (grants == null) { return false; }
        long now = now(target);
        for (long until : grants.values()) { if (until > now) { return true; } }
        return false;
    }

    public static boolean frozen(LivingEntity target) {
        return target.level().isClientSide() ? target.getData(RpgData.AURA_FROZEN) : active(target, Kind.FREEZING);
    }

    public static boolean consumeDodge(LivingEntity target) {
        if (!active(target, Kind.DODGE)) { return false; }
        var grants = target.getData(RpgData.AURAS).active.remove(Kind.DODGE);
        grants.keySet().forEach(owner -> unlink(target, owner));
        return true;
    }

    public static float shield(LivingEntity target) {
        if (!target.hasData(RpgData.AURAS)) { return 0; }
        var state = target.getData(RpgData.AURAS);
        return state.shieldUntil > now(target) ? state.shield : 0;
    }

    static float absorb(LivingEntity target, float damage) {
        float absorbed = Math.min(shield(target), Math.max(0, damage));
        if (absorbed > 0) { target.getData(RpgData.AURAS).shield -= absorbed; sync(target); }
        return absorbed;
    }

    private static void modifier(LivingEntity target, Holder<Attribute> attribute, String name, double amount,
            AttributeModifier.Operation operation) {
        var instance = target.getAttribute(attribute);
        if (instance == null) { return; }
        var id = Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "aura_" + name);
        var old = instance.getModifier(id);
        if (amount == 0) { if (old != null) { instance.removeModifier(id); } }
        else if (old == null || old.amount() != amount) { instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation)); }
    }

    private static void update(LivingEntity target) {
        var add = AttributeModifier.Operation.ADD_VALUE;
        var base = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        var total = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        modifier(target, Attributes.ARMOR, "armor", passive(target, Kind.FORTITUDE) ? 4 : 0, add);
        modifier(target, Attributes.ARMOR_TOUGHNESS, "toughness", passive(target, Kind.FORTITUDE) ? 2 : 0, add);
        modifier(target, Attributes.MOVEMENT_SPEED, "swiftness", (passive(target, Kind.SWIFTNESS) ? .1 : 0) + (active(target, Kind.SWIFTNESS) ? .2 : 0), base);
        modifier(target, Attributes.STEP_HEIGHT, "step", active(target, Kind.SWIFTNESS) ? .5 : 0, add);
        modifier(target, RpgStats.DODGE, "dodge", passive(target, Kind.DODGE) ? .1 : 0, add);
        modifier(target, RpgStats.MANA_REGENERATION, "mana", passive(target, Kind.ENCHANTING) ? .2 : 0, total);
        modifier(target, RpgStats.COOLDOWN, "cooldown", passive(target, Kind.ICY_VEINS) ? -.1 : 0, total);
        CovenantSlows.update(target);
        sync(target);
    }

    private static void sync(LivingEntity target) {
        boolean freeze = active(target, Kind.FREEZING);
        if (target.getData(RpgData.AURA_FROZEN) != freeze) { target.setData(RpgData.AURA_FROZEN, freeze); }
        float shield = shield(target);
        if (target instanceof ServerPlayer && target.getData(RpgData.FORTITUDE_CAPACITY) != shield) {
            target.setData(RpgData.FORTITUDE_CAPACITY, shield);
        }
    }

    public static void cleanse(LivingEntity target) {
        target.removeEffect(MobEffects.SLOWNESS);
        target.removeEffect(Chilled.EFFECT);
        FrostSlows.cleanse(target);
        if (target.hasData(RpgData.AURAS)) {
            var grants = target.getData(RpgData.AURAS).passive.remove(Kind.FREEZING);
            if (grants != null) { grants.keySet().forEach(owner -> unlink(target, owner)); }
        }
        CovenantSlows.update(target);
    }

    private static boolean belongs(State state, UUID owner) {
        if (owner.equals(state.shieldOwner)) { return true; }
        for (var maps : List.of(state.passive, state.active)) {
            for (var grants : maps.values()) { if (grants.containsKey(owner)) { return true; } }
        }
        return false;
    }

    private static void unlink(LivingEntity target, UUID owner) {
        if (!(target.level() instanceof ServerLevel level) || belongs(target.getData(RpgData.AURAS), owner)) { return; }
        var source = level.getServer().getPlayerList().getPlayer(owner);
        if (source != null && source.hasData(RpgData.AURAS)) { source.getData(RpgData.AURAS).recipients.remove(target); }
    }

    private static void removeSource(ServerPlayer source, Kind kind) {
        var state = source.getData(RpgData.AURAS);
        for (var target : state.recipients.toArray(LivingEntity[]::new)) {
            var recipient = target.getData(RpgData.AURAS);
            for (var maps : List.of(recipient.passive, recipient.active)) {
                for (var entry : maps.entrySet()) { if (kind == null || entry.getKey() == kind) { entry.getValue().remove(source.getUUID()); } }
                maps.values().removeIf(Map::isEmpty);
            }
            if ((kind == null || kind == Kind.FORTITUDE) && source.getUUID().equals(recipient.shieldOwner)) {
                recipient.shieldOwner = null; recipient.shield = 0;
            }
            update(target);
            if (!belongs(recipient, source.getUUID())) { state.recipients.remove(target); }
        }
    }

    /** Clear source ownership and recipient grants without touching cooldowns, bleeds, or committed shots. */
    public static void clear(LivingEntity target) {
        if (!target.hasData(RpgData.AURAS)) { return; }
        if (target instanceof ServerPlayer source) { removeSource(source, null); }
        var state = target.getData(RpgData.AURAS);
        var owners = new HashSet<UUID>();
        for (var maps : List.of(state.passive, state.active)) { maps.values().forEach(grants -> owners.addAll(grants.keySet())); maps.clear(); }
        if (state.shieldOwner != null) { owners.add(state.shieldOwner); }
        state.shieldOwner = null; state.shield = 0; state.assigned.clear();
        owners.forEach(owner -> unlink(target, owner));
        update(target);
        target.removeData(RpgData.AURAS);
    }

    private static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target) || !(target.level() instanceof ServerLevel)) { return; }
        if (target instanceof ServerPlayer source) {
            reconcile(source);
            if (now(source) % REFRESH_TICKS == 0) { refresh(source); }
        }
        if (!target.hasData(RpgData.AURAS)) { return; }
        if (!target.isAlive()) { clear(target); return; }
        var state = target.getData(RpgData.AURAS);
        if (state.passive.isEmpty() && state.active.isEmpty() && state.shieldOwner == null
                && state.assigned.isEmpty() && state.recipients.isEmpty()) {
            target.removeData(RpgData.AURAS); return;
        }
        long now = now(target);
        if (now < state.nextExpiry && !(state.shieldOwner != null && state.shield <= 0)) {
            AuraDamage.immolate(target, state, now); return;
        }
        var removedOwners = new HashSet<UUID>();
        boolean changed = false;
        state.nextExpiry = Long.MAX_VALUE;
        for (var maps : List.of(state.passive, state.active)) {
            for (var grants : maps.values()) {
                var iterator = grants.entrySet().iterator();
                while (iterator.hasNext()) { var grant = iterator.next(); if (grant.getValue() <= now) { removedOwners.add(grant.getKey()); iterator.remove(); changed = true; }
                    else { state.nextExpiry = Math.min(state.nextExpiry, grant.getValue()); } }
            }
            maps.values().removeIf(Map::isEmpty);
        }
        if (state.shieldOwner != null && (state.shieldUntil <= now || state.shield <= 0)) {
            removedOwners.add(state.shieldOwner); state.shieldOwner = null; state.shield = 0; changed = true;
        }
        if (state.shieldOwner != null) { state.nextExpiry = Math.min(state.nextExpiry, state.shieldUntil); }
        removedOwners.forEach(owner -> unlink(target, owner));
        if (changed) { update(target); }
        AuraDamage.immolate(target, state, now);
    }

    /** One shared spatial query for this source's assigned range passives. */
    public static void refresh(ServerPlayer source) {
        ClassResources.requireServer(source);
        if (!source.hasData(RpgData.AURAS)) { return; }
        var assigned = source.getData(RpgData.AURAS).assigned;
        boolean area = false;
        for (var kind : assigned) { if (!kind.selfOnly) { area = true; break; } }
        if (!area) { return; }
        var nearby = nearby(source);
        for (var kind : assigned) {
            if (kind.selfOnly) { continue; }
            for (var recipient : nearby) {
                if (kind.offensive ? Skills.canHit(source, recipient) : allied(source, recipient)) {
                    grant(source, recipient, kind, false, now(source) + COVERAGE_TICKS);
                }
            }
        }
    }

    private static void left(EntityLeaveLevelEvent event) { if (event.getEntity() instanceof LivingEntity target && !target.level().isClientSide()) { clear(target); } }
    private static void died(LivingDeathEvent event) { if (!event.getEntity().level().isClientSide()) { clear(event.getEntity()); } }
    private static void logout(PlayerEvent.PlayerLoggedOutEvent event) { clear(event.getEntity()); }
    private static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { clear(event.getEntity()); }
}
