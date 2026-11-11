package dev.covenant.rpg.skills;

import dev.covenant.Covenant;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.stats.RpgStats;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Server activation boundary shared by commands and skill slots. No client-supplied costs or targets. */
public final class Skills {
    /** Prototype Cleave has a 120-degree horizontal sweep, independent of vertical aim. */
    public static final double CLEAVE_HALF_ANGLE_DEGREES = 60;
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Covenant.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<HunterPet>> HUNTER_PET = ENTITIES.register("hunters_pet", () ->
            EntityType.Builder.<HunterPet>of(HunterPet::new, MobCategory.CREATURE).sized(.6f, .85f)
                    .clientTrackingRange(10).updateInterval(3).noSave().noSummon()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "hunters_pet"))));
    public static final DeferredHolder<EntityType<?>, EntityType<ElementalBolt>> FIRE_BOLT = elementalBolt("fire_bolt");
    public static final DeferredHolder<EntityType<?>, EntityType<ElementalBolt>> ICE_BOLT = elementalBolt("ice_bolt");

    private static DeferredHolder<EntityType<?>, EntityType<ElementalBolt>> elementalBolt(String name) {
        return ENTITIES.register(name, () -> EntityType.Builder.<ElementalBolt>of(ElementalBolt::new, MobCategory.MISC)
                .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(1).noSave().noSummon()
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Covenant.MOD_ID, name))));
    }
    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneBolt>> ARCANE_BOLT = ENTITIES.register("arcane_bolt", () ->
            EntityType.Builder.<ArcaneBolt>of(ArcaneBolt::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(4).updateInterval(1).noSave()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "arcane_bolt"))));
    public static final DeferredHolder<EntityType<?>, EntityType<TrapEntity>> TRAP = ENTITIES.register("trap", () ->
            EntityType.Builder.<TrapEntity>of(TrapEntity::new, MobCategory.MISC).sized(0.3f, 0.15f)
                    .clientTrackingRange(8).updateInterval(1).noSave().noSummon().fireImmune()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "trap"))));
    public static final DeferredHolder<EntityType<?>, EntityType<RageStorm>> RAGE_STORM = ENTITIES.register("rage_storm", () ->
            EntityType.Builder.<RageStorm>of(RageStorm::new, MobCategory.MISC).sized(.25f, .25f)
                    .clientTrackingRange(8).updateInterval(1).noSave().noSummon().fireImmune()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "rage_storm"))));

    private Skills() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        HunterPets.register(modBus);
        Bleeding.register();
        Rooted.register(modBus);
        Chilled.register(modBus);
        DischargeSlow.register(modBus);
        FrostSlows.register();
        Auras.register();
        ArmorShred.register(modBus);
        Vulnerability.register(modBus);
        Combos.register();
        BloodFever.register();
        NeoForge.EVENT_BUS.addListener(ReaverSkillData::register);
        NeoForge.EVENT_BUS.addListener(WeaverSkillData::register);
        SkillControls.register();
        SkillLifecycle.register();
        NeoForge.EVENT_BUS.addListener(Skills::tick);
        NeoForge.EVENT_BUS.addListener(ClassResources::damaged);
        dev.covenant.rpg.progression.CombatProgression.register();
        NeoForge.EVENT_BUS.addListener(SkillCommands::register);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SkillVisuals::arrowImpact);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SkillVisuals::damageLanded);
    }

    /** Returns localized feedback and changes nothing on rejection. Casts spend only at completion. */
    public static Component activate(ServerPlayer player, SkillDefinition skill) {
        try (var zone = Profiler.get().zone("covenant:skill_activate")) {
            ClassResources.requireServer(player);
            skill = ReaverSkillData.resolve(player, skill);
            skill = WeaverSkillData.resolve(player, skill);
            skill = HunterPetData.resolve(player, skill);
            var runtime = player.getData(RpgData.SKILL_RUNTIME);
            if (runtime.executing) { return message("busy"); }
            Auras.reconcile(player);
            Combos.expire(player);
            var failure = validate(player, skill);
            if (failure != null) { return failure; }
            if (skill.effect() == SkillDefinition.Effect.PASSIVE) { return message("passive"); }
            if (runtime.cast != null) { return message("casting"); }
            if (ClassResources.now(player) < runtime.nextActivation) { return message("busy"); }
            if (skill.effect() == SkillDefinition.Effect.HUNTERS_PET) { return HunterPets.activate(player, skill); }
            if (skill.effect() == SkillDefinition.Effect.COMBO) { return Combos.activate(player, skill); }
            double cost = RpgStats.scaleSkillValue(player, RpgStats.RESOURCE_COST, skill.cost());
            if (!ClassResources.canAfford(player, cost)) { return message("resource"); }
            long cast = Auras.active(player, Auras.Kind.ICY_VEINS) && SpellSkills.isSpell(skill) ? 0 : (long) Math.ceil(RpgStats.scaleSkillValue(player, RpgStats.CAST_TIME, skill.castTicks()));
            long cooldown = (long) Math.ceil(RpgStats.scaleSkillValue(player, RpgStats.COOLDOWN, skill.cooldownTicks()));
            if (cast == 0) { return execute(player, skill, cost, cooldown); }
            runtime.cast = new PendingCast(skill, PlayerProfiles.getSelectedClass(player).orElseThrow(),
                    ClassResources.now(player) + cast, cost, cooldown);
            runtime.cast.begin(player, cast);
            return message("started", skill.displayName(), cast / 20.0);
        }
    }

    private static Component validate(ServerPlayer player, SkillDefinition skill) {
        if (!SkillControls.canAct(player)) { return message("frozen"); }
        if (skill.effect() == SkillDefinition.Effect.AURA && !Auras.assigned(player, Auras.Kind.of(skill))) { return message("assigned"); }
        if (!player.isAlive() || player.isSpectator()) { return message("unavailable"); }
        var type = PlayerProfiles.getSelectedClass(player).orElse(null);
        if (type == null || !skill.allowedClasses().contains(type)) { return message("class"); }
        if (!player.getData(RpgData.SKILL_LOADOUT).learned().contains(skill.id())) { return message("unlearned"); }
        if (skill.effect() == SkillDefinition.Effect.HUNTERS_PET && !HunterPets.assigned(player)) { return message("assigned"); }
        if (skill.effect() == SkillDefinition.Effect.DISCHARGE && !WeaverPower.hasCharges(player)) { return message("charges"); }
        if (skill.effect() == SkillDefinition.Effect.THREEFOLD) {
            if (!WeaverPower.assigned(player)) { return message("assigned"); }
            if (!WeaverPower.canActivate(player)) { return message("reserves"); }
        }
        if (!ignoresCooldowns(player) && cooldownRemaining(player, skill) > 0
                && !(skill.effect() == SkillDefinition.Effect.HUNTERS_PET && HunterPets.active(player))) { return message("cooldown", cooldownRemaining(player, skill) / 20.0); }
        if (skill.effect() == SkillDefinition.Effect.DISENGAGE || skill.effect() == SkillDefinition.Effect.TELEPORT) {
            if (Rooted.contains(player)) { return message("rooted"); }
            if (player.getData(RpgData.SKILL_RUNTIME).movement != null || player.isPassenger() || player.isSleeping()) { return message("busy"); }
        }
        if (skill.effect() == SkillDefinition.Effect.BOW_SHOT) {
            var weapon = player.getMainHandItem();
            if (!(weapon.getItem() instanceof BowItem)) { return message("bow"); }
            if (!(player.getProjectile(weapon).getItem() instanceof ArrowItem)) { return message("arrow"); }
        }
        return null;
    }

    /** Actual saved timer, including while the debug bypass is enabled. */
    public static long cooldownRemaining(ServerPlayer player, SkillDefinition skill) {
        return Math.max(0, player.getData(RpgData.SKILLS).cooldowns().getOrDefault(skill.id(), 0L) - ClassResources.now(player));
    }

    public static boolean ignoresCooldowns(ServerPlayer player) { return player.getData(RpgData.SKILL_RUNTIME).ignoreCooldowns; }

    /** Bypasses checks only; disabling immediately restores all remaining saved cooldowns. */
    public static void setIgnoreCooldowns(ServerPlayer player, boolean enabled) {
        ClassResources.requireServer(player);
        player.getData(RpgData.SKILL_RUNTIME).ignoreCooldowns = enabled;
    }

    public static boolean isCasting(ServerPlayer player) { return player.getData(RpgData.SKILL_RUNTIME).cast != null; }

    private static Component execute(ServerPlayer player, SkillDefinition skill, double cost, long cooldown) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        if (runtime.executing) { return message("busy"); }
        // Preparation invokes native/mod hooks too (ammo use, entity joins). Guard the whole operation.
        runtime.executing = true;
        try { return prepareAndExecute(player, skill, cost, cooldown); }
        finally { runtime.executing = false; }
    }

    private static Component prepareAndExecute(ServerPlayer player, SkillDefinition skill, double cost, long cooldown) {
        try (var zone = Profiler.get().zone("covenant:skill_execute")) {
            var failure = validate(player, skill);
            if (failure != null) { return failure; }
            var rendTarget = skill.effect() == SkillDefinition.Effect.REND ? MeleeSkills.rendTarget(player) : null;
            var shot = skill.effect() == SkillDefinition.Effect.BOW_SHOT ? BowSkills.prepare(player) : null;
            if (skill.effect() == SkillDefinition.Effect.BOW_SHOT && shot == null) { return message("arrow"); }
            TrapEntity trap = null;
            if (skill.effect() == SkillDefinition.Effect.TRAP) { trap = Traps.prepare(player, skill); }
            if (skill.effect() == SkillDefinition.Effect.TRAP && trap == null) { return message("unavailable"); }
            if (trap != null && (trap.isRemoved() || validate(player, skill) != null)) {
                trap.discard(); return message("unavailable");
            }
            var storm = skill.effect() == SkillDefinition.Effect.RAGE_STORM ? RageStorm.prepare(player) : null;
            if (skill.effect() == SkillDefinition.Effect.RAGE_STORM && storm == null) { return message("unavailable"); }
            if (storm != null && (storm.isRemoved() || validate(player, skill) != null)) {
                storm.discard(); return message("unavailable");
            }
            var destination = skill.effect() == SkillDefinition.Effect.TELEPORT ? Teleport.destination(player, skill.range()) : null;
            if (skill.effect() == SkillDefinition.Effect.TELEPORT && destination == null) { return message("destination"); }
            // Commit before any damaging effect. A rejected entity add never replaces the previous trap.
            if (!ClassResources.consume(player, cost)) {
                if (trap != null) { trap.discard(); }
                if (storm != null) { storm.discard(); }
                return message("resource");
            }
            long now = ClassResources.now(player);
            player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(skill.id(), now + Math.max(1, cooldown)));
            player.getData(RpgData.SKILL_RUNTIME).nextActivation = now + 1;
            // Bow/spell execution and teleport endpoints own their release cue; other skills share this commit boundary.
            switch (skill.effect()) {
                case BOW_SHOT, ARCANE_BOLT, ELEMENTAL_BOLT, LIGHTNING_BOLT, TELEPORT -> { }
                default -> SkillSounds.release(player, skill);
            }
            switch (skill.effect()) {
                case COMBO -> throw new IllegalStateException("Combos use staged activation");
                case HUNTERS_PET -> throw new IllegalStateException("Companions use their summon/dismiss boundary");
                case PASSIVE -> throw new IllegalStateException("Passives do not activate");
                case BATTLE_CRY -> Auras.battleCry(player);
                case RAGE_STORM -> storm.release();
                case AURA -> Auras.activate(player, skill);
                case ARCANE_BOLT, ELEMENTAL_BOLT, LIGHTNING_BOLT -> SpellSkills.execute(player, skill);
                case TRAP -> Traps.accept(player, trap);
                case BOW_SHOT -> BowSkills.fire(player, skill, shot);
                case DISENGAGE -> Combos.disengage(player);
                case CLEAVE -> MeleeSkills.cleave(player, skill);
                case REND -> MeleeSkills.rend(player, rendTarget);
                case DISCHARGE -> Discharge.execute(player);
                case THREEFOLD -> WeaverPower.activate(player);
                case TELEPORT -> Teleport.execute(player, destination);
            }
            WeaverPower.castCompleted(player, skill);
            if (skill.effect() != SkillDefinition.Effect.BOW_SHOT) {
                player.swing(InteractionHand.MAIN_HAND, true);
            }
            return message("used", skill.displayName(), cost);
        }
    }

    public static boolean canHit(LivingEntity owner, LivingEntity target) {
        return owner != target && target.isAlive() && !target.isSpectator() && !Auras.allied(owner, target)
                && (!(owner instanceof ServerPlayer player) || !(target instanceof Player other) || player.canHarmPlayer(other))
                && (!(target instanceof net.minecraft.world.entity.TamableAnimal pet) || !pet.isOwnedBy(owner));
    }

    private static void tick(PlayerTickEvent.Post event) {
        try (var zone = Profiler.get().zone("covenant:skills_tick")) {
            if (!(event.getEntity() instanceof ServerPlayer player)) { return; }
            var runtime = player.getData(RpgData.SKILL_RUNTIME);
            long now = ClassResources.now(player);
            if (runtime.lastTick == now) { return; }
            runtime.lastTick = now;
            if (now % 5 == 0) { HunterPets.tick(player); }
            if (now % 5 == 0) { dev.covenant.rpg.progression.Progression.refreshView(player); }
            var type = PlayerProfiles.getSelectedClass(player).orElse(null);
            var cast = runtime.cast;
            if (cast != null) {
                if (!player.isAlive() || player.isSpectator() || type != cast.playerClass() || !SkillControls.canAct(player)
                        || (cast.skill() == SkillDefinition.AIMED_SHOT && validate(player, cast.skill()) != null)) {
                    runtime.cancelCast(player);
                    player.sendOverlayMessage(message("cancelled"));
                } else if (now >= cast.endsAt()) {
                    runtime.cancelCast(player);
                    player.sendOverlayMessage(execute(player, cast.skill(), cast.cost(), cast.cooldown()));
                } else if (cast.skill().effect() == SkillDefinition.Effect.RAGE_STORM) {
                    SkillVisuals.rageCharge(player);
                } else if (cast.skill() == SkillDefinition.AIMED_SHOT) {
                    SkillVisuals.aimedCharge(player);
                } else if (SpellSkills.isSpell(cast.skill())) {
                    SkillVisuals.charge(player, cast.skill());
                }
            }
            if (type != null && player.isAlive() && !player.isSpectator() && now % 5 == 0) {
                ClassResources.tick(player, type);
                WeaverPower.reconcile(player);
            }
        }
    }

    static Component message(String key, Object... args) { return Component.translatable("commands.covenant.skill." + key, args); }
}
