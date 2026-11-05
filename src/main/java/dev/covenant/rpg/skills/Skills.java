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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Server activation boundary shared by commands and skill slots. No client-supplied costs or targets. */
public final class Skills {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Covenant.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<ArcaneBolt>> ARCANE_BOLT = ENTITIES.register("arcane_bolt", () ->
            EntityType.Builder.<ArcaneBolt>of(ArcaneBolt::new, MobCategory.MISC).sized(0.25f, 0.25f)
                    .clientTrackingRange(4).updateInterval(1).noSave()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "arcane_bolt"))));

    private Skills() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.addListener(Skills::tick);
        NeoForge.EVENT_BUS.addListener(ClassResources::damaged);
        NeoForge.EVENT_BUS.addListener(SkillCommands::register);
        NeoForge.EVENT_BUS.addListener(Skills::changedDimension);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SkillVisuals::arrowImpact);
    }

    /** Returns localized feedback and changes nothing on rejection. Casts spend only at completion. */
    public static Component activate(ServerPlayer player, SkillDefinition skill) {
        ClassResources.requireServer(player);
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        var failure = validate(player, skill);
        if (failure != null) { return failure; }
        if (runtime.casting != null) { return message("casting"); }
        if (ClassResources.now(player) < runtime.nextActivation) { return message("busy"); }
        double cost = RpgStats.scaleSkillValue(player, RpgStats.RESOURCE_COST, skill.cost());
        if (ClassResources.current(player, PlayerProfiles.getSelectedClass(player).orElseThrow()) < cost) { return message("resource"); }
        long cast = (long) Math.ceil(RpgStats.scaleSkillValue(player, RpgStats.CAST_TIME, skill.castTicks()));
        long cooldown = (long) Math.ceil(RpgStats.scaleSkillValue(player, RpgStats.COOLDOWN, skill.cooldownTicks()));
        if (cast == 0) { return execute(player, skill, cost, cooldown); }
        runtime.casting = skill;
        runtime.castClass = PlayerProfiles.getSelectedClass(player).orElseThrow();
        runtime.castEnds = ClassResources.now(player) + cast;
        runtime.castCost = cost;
        runtime.castCooldown = cooldown;
        player.swing(InteractionHand.MAIN_HAND, true);
        return message("started", skill.displayName(), cast / 20.0);
    }

    private static Component validate(ServerPlayer player, SkillDefinition skill) {
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
        return null;
    }

    public static long cooldownRemaining(ServerPlayer player, SkillDefinition skill) {
        return Math.max(0, player.getData(RpgData.SKILLS).cooldowns().getOrDefault(skill.id(), 0L) - ClassResources.now(player));
    }

    private static Component execute(ServerPlayer player, SkillDefinition skill, double cost, long cooldown) {
        var failure = validate(player, skill);
        if (failure != null) { return failure; }
        ItemStack ammunition = ItemStack.EMPTY;
        int ammunitionCost = 0;
        if (skill.effect() == SkillDefinition.Effect.BOW_SHOT) {
            var bow = player.getMainHandItem();
            ammunition = player.getProjectile(bow);
            boolean infinite = player.hasInfiniteMaterials() || ((ArrowItem) ammunition.getItem()).isInfinite(ammunition, bow, player);
            ammunitionCost = infinite ? 0 : Math.max(0, EnchantmentHelper.processAmmoUse(player.level(), bow, ammunition, 1));
            if (ammunitionCost > ammunition.getCount()) { return message("arrow"); }
        }
        // Commit before any effect: damage hooks/other mods cannot reenter and spend the same balance.
        if (!ClassResources.consume(player, cost)) { return message("resource"); }
        long now = ClassResources.now(player);
        player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(skill.id(), now + Math.max(1, cooldown)));
        player.getData(RpgData.SKILL_RUNTIME).nextActivation = now + 1;
        switch (skill.effect()) {
            case BOW_SHOT -> {
                var bow = player.getMainHandItem();
                var ammo = ammunition;
                var arrowItem = (ArrowItem) ammo.getItem();
                var arrow = arrowItem.createArrow(player.level(), ammo, player, bow);
                arrow.setBaseDamage(2 * skill.power());
                // "Piercing" is increased single-target damage, not vanilla multi-target piercing.
                arrow.pickup = ammunitionCost == 0 ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 3, 0);
                arrow.setData(RpgData.SKILL_SHOT, true);
                Projectile.spawnProjectile(arrow, player.level(), ammo.copyWithCount(1));
                ammo.shrink(ammunitionCost);
                bow.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
                player.setData(RpgData.BOW_DRAW_UNTIL, player.level().getGameTime() + SkillVisuals.BOW_DRAW_TICKS);
            }
            case CLEAVE -> {
                SkillVisuals.cleave(player, skill.range());
                float damage = (float) (player.getAttributeValue(RpgStats.DAMAGE) * skill.power());
                for (var target : targets(player, skill)) {
                    if (target.hurtServer(player.level(), player.damageSources().playerAttack(player), damage)) {
                        SkillVisuals.cleaveHit(player.level(), target);
                    }
                }
            }
            case ARCANE_BOLT -> {
                var bolt = new ArcaneBolt(ARCANE_BOLT.get(), player.level());
                bolt.setOwner(player);
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

    private static java.util.List<LivingEntity> targets(ServerPlayer player, SkillDefinition skill) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(skill.range()),
                target -> canHit(player, target) && player.distanceToSqr(target) <= skill.range() * skill.range()
                        && player.hasLineOfSight(target));
    }

    public static boolean canHit(LivingEntity owner, LivingEntity target) {
        return owner != target && target.isAlive() && !target.isSpectator() && !owner.isAlliedTo(target)
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
            if (!player.isAlive() || player.isSpectator() || type != runtime.castClass) {
                runtime.cancelCast();
                player.sendSystemMessage(message("cancelled"));
            } else if (now >= runtime.castEnds) {
                var skill = runtime.casting;
                runtime.cancelCast();
                player.sendSystemMessage(execute(player, skill, runtime.castCost, runtime.castCooldown));
            } else if (runtime.casting == SkillDefinition.ARCANE_BOLT) {
                SkillVisuals.charge(player);
            }
        }
        if (type != null && player.isAlive() && !player.isSpectator() && now % 5 == 0) {
            ClassResources.tick(player, type);
        }
    }

    private static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        event.getEntity().getData(RpgData.SKILL_RUNTIME).cancelCast();
    }

    static Component message(String key, Object... args) { return Component.translatable("commands.covenant.skill." + key, args); }
}
