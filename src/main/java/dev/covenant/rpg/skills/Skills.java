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

    private Skills() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        Bleeding.register();
        Rooted.register(modBus);
        Chilled.register(modBus);
        FrostSlows.register();
        Auras.register();
        ArmorShred.register(modBus);
        Combos.register();
        SkillControls.register();
        SkillLifecycle.register();
        NeoForge.EVENT_BUS.addListener(Skills::tick);
        NeoForge.EVENT_BUS.addListener(ClassResources::damaged);
        NeoForge.EVENT_BUS.addListener(SkillCommands::register);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SkillVisuals::arrowImpact);
    }

    /** Returns localized feedback and changes nothing on rejection. Casts spend only at completion. */
    public static Component activate(ServerPlayer player, SkillDefinition skill) {
        ClassResources.requireServer(player);
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        if (runtime.executing) { return message("busy"); }
        Auras.reconcile(player);
        Combos.expire(player);
        var failure = validate(player, skill);
        if (failure != null) { return failure; }
        if (runtime.cast != null) { return message("casting"); }
        if (ClassResources.now(player) < runtime.nextActivation) { return message("busy"); }
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

    private static Component validate(ServerPlayer player, SkillDefinition skill) {
        if (!SkillControls.canAct(player)) { return message("frozen"); }
        if (skill.effect() == SkillDefinition.Effect.AURA && !Auras.assigned(player, Auras.Kind.of(skill))) { return message("assigned"); }
        if (!player.isAlive() || player.isSpectator()) { return message("unavailable"); }
        var type = PlayerProfiles.getSelectedClass(player).orElse(null);
        if (type == null || !skill.allowedClasses().contains(type)) { return message("class"); }
        if (!player.getData(RpgData.SKILL_LOADOUT).learned().contains(skill.id())) { return message("unlearned"); }
        if (!ignoresCooldowns(player) && cooldownRemaining(player, skill) > 0) { return message("cooldown", cooldownRemaining(player, skill) / 20.0); }
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
        // Commit before any damaging effect. A rejected entity add never replaces the previous trap.
        if (!ClassResources.consume(player, cost)) {
            if (trap != null) { trap.discard(); }
            return message("resource");
        }
        long now = ClassResources.now(player);
        player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(skill.id(), now + Math.max(1, cooldown)));
        player.getData(RpgData.SKILL_RUNTIME).nextActivation = now + 1;
        switch (skill.effect()) {
            case COMBO -> throw new IllegalStateException("Combos use staged activation");
            case AURA -> Auras.activate(player, skill);
            case ARCANE_BOLT, ELEMENTAL_BOLT, LIGHTNING_BOLT -> SpellSkills.execute(player, skill);
            case TRAP -> Traps.accept(player, trap);
            case BOW_SHOT -> BowSkills.fire(player, skill, shot);
            case CLEAVE -> MeleeSkills.cleave(player, skill);
            case REND -> MeleeSkills.rend(player, rendTarget);
        }
        if (skill.effect() != SkillDefinition.Effect.BOW_SHOT) {
            player.swing(InteractionHand.MAIN_HAND, true);
        }
        return message("used", skill.displayName(), cost);
    }

    public static boolean canHit(LivingEntity owner, LivingEntity target) {
        return owner != target && target.isAlive() && !target.isSpectator() && !Auras.allied(owner, target)
                && (!(owner instanceof ServerPlayer player) || !(target instanceof Player other) || player.canHarmPlayer(other))
                && (!(target instanceof net.minecraft.world.entity.TamableAnimal pet) || !pet.isOwnedBy(owner));
    }

    private static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) { return; }
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        long now = ClassResources.now(player);
        if (runtime.lastTick == now) { return; }
        runtime.lastTick = now;
        var type = PlayerProfiles.getSelectedClass(player).orElse(null);
        var cast = runtime.cast;
        if (cast != null) {
            if (!player.isAlive() || player.isSpectator() || type != cast.playerClass() || !SkillControls.canAct(player)
                    || (cast.skill() == SkillDefinition.AIMED_SHOT && validate(player, cast.skill()) != null)) {
                runtime.cancelCast(player);
                player.sendSystemMessage(message("cancelled"));
            } else if (now >= cast.endsAt()) {
                runtime.cancelCast(player);
                player.sendSystemMessage(execute(player, cast.skill(), cast.cost(), cast.cooldown()));
            } else if (SpellSkills.isSpell(cast.skill())) {
                SkillVisuals.charge(player);
            }
        }
        if (type != null && player.isAlive() && !player.isSpectator() && now % 5 == 0) {
            ClassResources.tick(player, type);
        }
    }

    static Component message(String key, Object... args) { return Component.translatable("commands.covenant.skill." + key, args); }
}
