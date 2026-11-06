package dev.covenant.rpg.skills;

import dev.covenant.Covenant;
import dev.covenant.mixin.ArrowPiercingAccessor;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Server activation boundary shared by commands and skill slots. No client-supplied costs or targets. */
public final class Skills {
    /** Prototype Cleave has a 120-degree horizontal sweep, independent of vertical aim. */
    public static final double CLEAVE_HALF_ANGLE_DEGREES = 60;
    static final Identifier AIMED_SHOT_SLOW = Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "aimed_shot_slow");
    private static final AttributeModifier AIMED_SHOT_MOVEMENT = new AttributeModifier(
            AIMED_SHOT_SLOW, -0.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
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
        NeoForge.EVENT_BUS.addListener(Skills::tick);
        NeoForge.EVENT_BUS.addListener(ClassResources::damaged);
        NeoForge.EVENT_BUS.addListener(SkillCommands::register);
        NeoForge.EVENT_BUS.addListener(Skills::changedDimension);
        NeoForge.EVENT_BUS.addListener(Skills::loggedOut);
        NeoForge.EVENT_BUS.addListener(Skills::died);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SkillVisuals::arrowImpact);
    }

    /** Returns localized feedback and changes nothing on rejection. Casts spend only at completion. */
    public static Component activate(ServerPlayer player, SkillDefinition skill) {
        ClassResources.requireServer(player);
        Auras.reconcile(player);
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        if (runtime.deploying) { return message("busy"); }
        var failure = validate(player, skill);
        if (failure != null) { return failure; }
        if (runtime.casting != null) { return message("casting"); }
        if (ClassResources.now(player) < runtime.nextActivation) { return message("busy"); }
        double cost = RpgStats.scaleSkillValue(player, RpgStats.RESOURCE_COST, skill.cost());
        if (ClassResources.current(player, PlayerProfiles.getSelectedClass(player).orElseThrow()) < cost) { return message("resource"); }
        long cast = Auras.active(player, Auras.Kind.ICY_VEINS) && isSpell(skill) ? 0 : (long) Math.ceil(RpgStats.scaleSkillValue(player, RpgStats.CAST_TIME, skill.castTicks()));
        long cooldown = (long) Math.ceil(RpgStats.scaleSkillValue(player, RpgStats.COOLDOWN, skill.cooldownTicks()));
        if (cast == 0) { return execute(player, skill, cost, cooldown); }
        runtime.casting = skill;
        runtime.castClass = PlayerProfiles.getSelectedClass(player).orElseThrow();
        runtime.castEnds = ClassResources.now(player) + cast;
        runtime.castCost = cost;
        runtime.castCooldown = cooldown;
        if (skill == SkillDefinition.AIMED_SHOT) {
            player.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(AIMED_SHOT_MOVEMENT);
            SkillVisuals.bowDraw(player, cast);
        } else {
            player.swing(InteractionHand.MAIN_HAND, true);
        }
        return message("started", skill.displayName(), cast / 20.0);
    }

    private static boolean isSpell(SkillDefinition skill) {
        return skill.effect() == SkillDefinition.Effect.ARCANE_BOLT || skill.effect() == SkillDefinition.Effect.ELEMENTAL_BOLT
                || skill.effect() == SkillDefinition.Effect.LIGHTNING_BOLT;
    }

    private static Component validate(ServerPlayer player, SkillDefinition skill) {
        if (Auras.frozen(player)) { return message("frozen"); }
        if (skill.effect() == SkillDefinition.Effect.AURA && !Auras.assigned(player, Auras.Kind.of(skill))) { return message("assigned"); }
        if (!player.isAlive() || player.isSpectator()) { return message("unavailable"); }
        var type = PlayerProfiles.getSelectedClass(player).orElse(null);
        if (type == null || !skill.allowedClasses().contains(type)) { return message("class"); }
        if (!player.getData(RpgData.SKILL_LOADOUT).learned().contains(skill.id())) { return message("unlearned"); }
        if (cooldownRemaining(player, skill) > 0) { return message("cooldown", cooldownRemaining(player, skill) / 20.0); }
        if (skill.effect() == SkillDefinition.Effect.BOW_SHOT) {
            var weapon = player.getMainHandItem();
            if (!(weapon.getItem() instanceof BowItem)) { return message("bow"); }
            if (!(player.getProjectile(weapon).getItem() instanceof ArrowItem)) { return message("arrow"); }
        }
        if (skill.effect() == SkillDefinition.Effect.CLEAVE && targets(player, skill).isEmpty()) { return message("target"); }
        if (skill.effect() == SkillDefinition.Effect.REND && rendTarget(player) == null) { return message("target"); }
        return null;
    }

    public static long cooldownRemaining(ServerPlayer player, SkillDefinition skill) {
        return Math.max(0, player.getData(RpgData.SKILLS).cooldowns().getOrDefault(skill.id(), 0L) - ClassResources.now(player));
    }

    public static boolean isCasting(ServerPlayer player) { return player.getData(RpgData.SKILL_RUNTIME).casting != null; }

    private static Component execute(ServerPlayer player, SkillDefinition skill, double cost, long cooldown) {
        var failure = validate(player, skill);
        if (failure != null) { return failure; }
        var rendTarget = skill.effect() == SkillDefinition.Effect.REND ? rendTarget(player) : null;
        ItemStack ammunition = ItemStack.EMPTY;
        int ammunitionCost = 0;
        if (skill.effect() == SkillDefinition.Effect.BOW_SHOT) {
            var bow = player.getMainHandItem();
            ammunition = player.getProjectile(bow);
            boolean infinite = player.hasInfiniteMaterials() || ((ArrowItem) ammunition.getItem()).isInfinite(ammunition, bow, player);
            ammunitionCost = infinite ? 0 : Math.max(0, EnchantmentHelper.processAmmoUse(player.level(), bow, ammunition, 1));
            if (ammunitionCost > ammunition.getCount()) { return message("arrow"); }
        }
        TrapEntity trap = null;
        if (skill.effect() == SkillDefinition.Effect.TRAP) {
            var runtime = player.getData(RpgData.SKILL_RUNTIME);
            runtime.deploying = true;
            try { trap = Traps.prepare(player, skill); }
            finally { runtime.deploying = false; }
        }
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
            case AURA -> Auras.activate(player, skill);
            case LIGHTNING_BOLT -> ElementalBolts.lightning(player);
            case ELEMENTAL_BOLT -> {
                var bolt = new ElementalBolt(skill == SkillDefinition.FIRE_BOLT ? FIRE_BOLT.get() : ICE_BOLT.get(), player.level());
                bolt.setOwner(player);
                bolt.initialize(ElementalBolts.duration(player, skill));
                bolt.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
                bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5f, 0);
                player.level().addFreshEntity(bolt);
            }
            case TRAP -> Traps.accept(player, trap);
            case BOW_SHOT -> {
                var bow = player.getMainHandItem();
                var ammo = ammunition;
                var arrowItem = (ArrowItem) ammo.getItem();
                var arrow = arrowItem.createArrow(player.level(), ammo, player, bow);
                arrow.setBaseDamage(2 * skill.power() * AuraDamage.multiplier(player, false));
                arrow.setData(RpgData.AURA_PROJECTILE_POWER, AuraDamage.multiplier(player, false));
                // Native level is the number of extra targets; two means at most three total.
                ((ArrowPiercingAccessor) arrow).covenant$setPierceLevel((byte) (skill == SkillDefinition.PIERCING_SHOT ? 2 : 0));
                arrow.pickup = ammunitionCost == 0 ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 3, 0);
                arrow.setData(RpgData.SKILL_SHOT, true);
                Projectile.spawnProjectile(arrow, player.level(), ammo.copyWithCount(1));
                ammo.shrink(ammunitionCost);
                bow.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
                if (skill != SkillDefinition.AIMED_SHOT) { SkillVisuals.bowDraw(player, SkillVisuals.BOW_DRAW_TICKS); }
            }
            case CLEAVE -> {
                SkillVisuals.cleave(player, skill.range());
                float damage = (float) (player.getAttributeValue(RpgStats.DAMAGE) * skill.power() * AuraDamage.multiplier(player, false));
                for (var target : targets(player, skill)) {
                    if (hurtMeleeSkill(player, target, damage)) {
                        SkillVisuals.cleaveHit(player.level(), target);
                    }
                }
            }
            case REND -> {
                float attackDamage = (float) player.getAttributeValue(RpgStats.DAMAGE);
                float bleedDamage = AuraDamage.bleedPower(player);
                int duration = (int) Math.clamp(Math.ceil(RpgStats.scaleSkillValue(player,
                        RpgStats.SKILL_DURATION, Bleeding.REND_DURATION_TICKS)), 1, Integer.MAX_VALUE);
                var before = AuraDamage.before(rendTarget);
                var runtime = player.getData(RpgData.SKILL_RUNTIME);
                runtime.applyingRend = true;
                try { hurtMeleeSkill(player, rendTarget, (float) (attackDamage * skill.power() * AuraDamage.multiplier(player, false))); }
                finally { runtime.applyingRend = false; }
                if (before.landed(rendTarget)) {
                    if (bleedDamage > 0) { Bleeding.apply(rendTarget, player, bleedDamage, duration); }
                    SkillVisuals.cleaveHit(player.level(), rendTarget);
                }
            }
            case ARCANE_BOLT -> {
                var bolt = new ArcaneBolt(ARCANE_BOLT.get(), player.level());
                bolt.setOwner(player);
                bolt.setData(RpgData.AURA_PROJECTILE_POWER, AuraDamage.multiplier(player, false));
                bolt.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
                bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5f, 0);
                player.level().addFreshEntity(bolt);
            }
        }
        if (skill.effect() != SkillDefinition.Effect.BOW_SHOT) {
            player.swing(InteractionHand.MAIN_HAND, true);
        }
        return message("used", skill.displayName(), cost);
    }

    /** Uses the same held-item attack range and block-clipped crosshair pick as native melee. */
    private static LivingEntity rendTarget(ServerPlayer player) {
        var range = player.getAttackRangeWith(player.getMainHandItem());
        var hit = range.getClosesetHit(player, 1, entity -> entity.isPickable() && !entity.isSpectator());
        return hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target
                && canHit(player, target) && player.hasLineOfSight(target) ? target : null;
    }

    private static boolean hurtMeleeSkill(ServerPlayer player, LivingEntity target, float damage) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        var previous = runtime.skillDamageSource;
        var source = player.damageSources().playerAttack(player);
        runtime.skillDamageSource = source;
        try { return target.hurtServer(player.level(), source, damage); }
        finally { runtime.skillDamageSource = previous; }
    }

    private static java.util.List<LivingEntity> targets(ServerPlayer player, SkillDefinition skill) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(skill.range()),
                target -> canHit(player, target) && player.distanceToSqr(target) <= skill.range() * skill.range()
                        && inCleaveArc(player, target)
                        && player.hasLineOfSight(target));
    }

    private static boolean inCleaveArc(ServerPlayer player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double yaw = Math.toRadians(player.getYRot());
        return distance < 0.00001 || (-Math.sin(yaw) * dx + Math.cos(yaw) * dz)
                >= distance * Math.cos(Math.toRadians(CLEAVE_HALF_ANGLE_DEGREES));
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
        if (runtime.casting != null) {
            if (!player.isAlive() || player.isSpectator() || type != runtime.castClass
                    || (runtime.casting == SkillDefinition.AIMED_SHOT && validate(player, runtime.casting) != null)) {
                runtime.cancelCast(player);
                player.sendSystemMessage(message("cancelled"));
            } else if (now >= runtime.castEnds) {
                var skill = runtime.casting;
                runtime.cancelCast(player);
                player.sendSystemMessage(execute(player, skill, runtime.castCost, runtime.castCooldown));
            } else if (runtime.casting == SkillDefinition.ARCANE_BOLT
                    || runtime.casting.effect() == SkillDefinition.Effect.ELEMENTAL_BOLT
                    || runtime.casting.effect() == SkillDefinition.Effect.LIGHTNING_BOLT) {
                SkillVisuals.charge(player);
            }
        }
        if (type != null && player.isAlive() && !player.isSpectator() && now % 5 == 0) {
            ClassResources.tick(player, type);
        }
    }

    private static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { player.getData(RpgData.SKILL_RUNTIME).cancelCast(player); Traps.clear(player, null); }
    }

    private static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { player.getData(RpgData.SKILL_RUNTIME).cancelCast(player); Traps.clear(player, null); }
    }

    private static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { player.getData(RpgData.SKILL_RUNTIME).cancelCast(player); Traps.clear(player, null); }
    }

    static Component message(String key, Object... args) { return Component.translatable("commands.covenant.skill." + key, args); }
}
