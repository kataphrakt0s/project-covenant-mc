package dev.covenant.test;

import dev.covenant.rpg.skills.*;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.stats.RpgStats;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Real server players, native damage and native projectiles; no mocked damage containers. */
public final class AuraGameTests {
    public static void rage(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var source = f.player(PlayerClass.REAVER, 0); var target = f.target(2);
            f.equip(source, SkillDefinition.RAGING_AURA, 0); ClassResources.set(source, PlayerClass.REAVER, 100);
            for (int i = 0; i < 4; i++) { invoke(ClassResources.class, "tick", null, new Class<?>[]{ServerPlayer.class, PlayerClass.class}, source, PlayerClass.REAVER); }
            near(ClassResources.current(source, PlayerClass.REAVER), 97, "Assigned passive decays three Rage per second");
            f.cast(source, SkillDefinition.RAGING_AURA);
            for (int i = 0; i < 4; i++) { invoke(ClassResources.class, "tick", null, new Class<?>[]{ServerPlayer.class, PlayerClass.class}, source, PlayerClass.REAVER); }
            near(ClassResources.current(source, PlayerClass.REAVER), 60, "Raging active stops decay");
            f.learn(source, SkillDefinition.REND); source.removeData(RpgData.SKILL_RUNTIME);
            Skills.activate(source, SkillDefinition.REND);
            near(ClassResources.current(source, PlayerClass.REAVER), 40, "Active still spends normal skill cost");
            check(ClassResources.inCombat(source), "Confirmed skill damage starts existing combat grace");
            SkillLoadouts.bind(source, 0, null);
            for (int i = 0; i < 4; i++) { invoke(ClassResources.class, "tick", null, new Class<?>[]{ServerPlayer.class, PlayerClass.class}, source, PlayerClass.REAVER); }
            near(ClassResources.current(source, PlayerClass.REAVER), 40, "Grace remains seven seconds independent of aura");
            helper.succeed();
        }
    }

    public static void projectiles(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var source = f.player(PlayerClass.WEAVER, 0);
            f.equip(source, SkillDefinition.ENCHANTING_AURA, 0); f.cast(source, SkillDefinition.ENCHANTING_AURA);
            source.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
            source.getInventory().setItem(5, new ItemStack(Items.ARROW, 10));
            source.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
            var bow = (BowItem) Items.BOW;
            bow.releaseUsing(source.getMainHandItem(), source.level(), source, 72000 - 10);
            var arrows = source.level().getEntitiesOfClass(AbstractArrow.class, source.getBoundingBox().inflate(2));
            check(arrows.size() == 1, "Native bow fires a projectile");
            near(arrows.getFirst().getData(RpgData.ENCHANTED_SHOT), 2 * BowItem.getPowerForTime(10), "Native partial draw captured at firing");
            f.entities.addAll(arrows); arrows.forEach(Entity::discard); source.stopUsingItem();
            var crossbow = new ItemStack(Items.CROSSBOW);
            crossbow.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,
                    net.minecraft.world.item.component.ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW), new ItemStack(Items.ARROW), new ItemStack(Items.ARROW))));
            source.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, crossbow);
            ((CrossbowItem) Items.CROSSBOW).performShooting(source.level(), source, net.minecraft.world.InteractionHand.MAIN_HAND, crossbow, 3.15f, 0, null);
            arrows = source.level().getEntitiesOfClass(AbstractArrow.class, source.getBoundingBox().inflate(2));
            check(arrows.size() == 3, "Native loaded multishot creates three independent projectiles");
            for (var arrow : arrows) { near(arrow.getData(RpgData.ENCHANTED_SHOT), 2, "Each crossbow shot captures full bonus"); }
            f.entities.addAll(arrows);
            SkillLoadouts.bind(source, 0, null);
            var target = f.target(3);
            for (var arrow : arrows) { target.invulnerableTime = 0; target.hurtServer(source.level(), source.damageSources().arrow(arrow, source), 1); }
            near(target.getHealth(), 191, "Each committed multishot arrow applies its own magic");
            helper.succeed();
        }
    }

    public static void expiries(GameTestHelper helper) {
        var f = new Fixture(helper);
        var source = f.player(PlayerClass.REAVER, 0); var friend = f.player(PlayerClass.WEAVER, 2); f.team(source, friend);
        f.equip(source, SkillDefinition.FORTITUDE_AURA, 0); source.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(.1);
        f.cast(source, SkillDefinition.FORTITUDE_AURA);
        friend.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(10); friend.setAbsorptionAmount(5);
        var enemy = f.player(PlayerClass.WEAVER, 4); f.equip(enemy, SkillDefinition.FREEZING_AURA, 0);
        enemy.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(.1); f.cast(enemy, SkillDefinition.FREEZING_AURA);
        check(Auras.frozen(friend), "First freeze applies");
        helper.onEachTick(f::tick);
        helper.runAtTickTime(6, () -> {
            check(!Auras.frozen(friend), "Short freeze expires with no immunity");
            f.cast(enemy, SkillDefinition.FREEZING_AURA); check(Auras.frozen(friend), "Immediate subsequent freeze allowed");
        });
        helper.runAtTickTime(20, () -> {
            try {
                near(Auras.shield(friend), 0, "Unused shield expires at scaled duration");
                near(friend.getData(RpgData.FORTITUDE_CAPACITY), 0, "Synced display capacity clears on expiry");
                near(friend.getAbsorptionAmount(), 5, "Expiry preserves unrelated absorption");
                check(!Auras.frozen(friend) && !friend.getData(RpgData.AURA_FROZEN), "Authoritative freeze sync clears");
                helper.succeed();
            } finally { f.close(); }
        });
    }

    public static void ownership(GameTestHelper helper) {
        var f = new Fixture(helper);
        try {
            var source = f.player(PlayerClass.STALKER, 0);
            var friend = f.player(PlayerClass.WEAVER, 2);
            var outsider = f.player(PlayerClass.REAVER, 3);
            f.team(source, friend);
            var pet = f.wolf(friend, 4);
            var ownPet = f.wolf(source, 5);
            var above = f.wolf(friend, 0); above.setPos(source.position().add(0, 6.01, 0));
            f.equip(source, SkillDefinition.SWIFTNESS_AURA, 0);
            SkillLoadouts.bind(source, 1, SkillDefinition.SWIFTNESS_AURA.id());
            ClassResources.set(source, PlayerClass.STALKER, 0);
            f.coverage(source);
            near(friend.getAttributeValue(Attributes.MOVEMENT_SPEED), .11, "Teammate coverage");
            near(source.getAttributeValue(Attributes.MOVEMENT_SPEED), .11, "Duplicate slots do not stack");
            check(Auras.passive(pet, Auras.Kind.SWIFTNESS) && Auras.passive(ownPet, Auras.Kind.SWIFTNESS), "Both owners' pets benefit");
            check(!Auras.passive(outsider, Auras.Kind.SWIFTNESS) && !Auras.passive(above, Auras.Kind.SWIFTNESS), "Outsiders and sphere exterior excluded");
            check(!Skills.canHit(source, pet) && Skills.canHit(source, outsider), "Offensive pet/team policy");
            f.cast(source, SkillDefinition.SWIFTNESS_AURA);
            near(friend.getAttributeValue(Attributes.MOVEMENT_SPEED), .13, "Passive and active add to 30 percent");
            double step = friend.getAttributeValue(Attributes.STEP_HEIGHT);
            check(step >= 1.1, "Native step height increases by half a block");
            var late = f.wolf(friend, 1);
            check(!Auras.active(late, Auras.Kind.SWIFTNESS), "Late arrivals get no active buff");
            friend.setPos(source.position().add(0, 0, 12));
            check(Auras.active(friend, Auras.Kind.SWIFTNESS), "Leaving field retains active");
            SkillLoadouts.bind(source, 0, null);
            check(Auras.active(friend, Auras.Kind.SWIFTNESS), "Removing one duplicate retains ownership");
            long cooldown = Skills.cooldownRemaining(source, SkillDefinition.SWIFTNESS_AURA);
            SkillLoadouts.bind(source, 1, null);
            check(!Auras.active(friend, Auras.Kind.SWIFTNESS), "Last slot removes remote active immediately");
            near(friend.getAttributeValue(Attributes.MOVEMENT_SPEED), .1, "Source cleanup restores native speed");
            check(Skills.cooldownRemaining(source, SkillDefinition.SWIFTNESS_AURA) == cooldown, "Cleanup retains cooldown");
            var before = ClassResources.current(source, PlayerClass.STALKER);
            check(Skills.activate(source, SkillDefinition.SWIFTNESS_AURA).getString().toLowerCase(java.util.Locale.ROOT).contains("assign"), "Command requires assignment");
            near(ClassResources.current(source, PlayerClass.STALKER), before, "Rejected activation spends nothing");
            helper.succeed();
        } finally { f.close(); }
    }

    public static void coverage(GameTestHelper helper) {
        var f = new Fixture(helper);
        var source = f.player(PlayerClass.STALKER, 0);
        var other = f.player(PlayerClass.STALKER, 1);
        var friend = f.player(PlayerClass.WEAVER, 6);
        f.team(source, other, friend);
        f.equip(source, SkillDefinition.SWIFTNESS_AURA, 0);
        f.equip(other, SkillDefinition.SWIFTNESS_AURA, 0);
        var wall = BlockPos.containing(source.position().add(0, 1, 3)); f.block(wall);
        helper.onEachTick(f::tick);
        helper.runAtTickTime(12, () -> {
            near(friend.getAttributeValue(Attributes.MOVEMENT_SPEED), .11, "Sphere edge and cover ignored");
            f.cast(source, SkillDefinition.SWIFTNESS_AURA);
            other.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(2);
            f.cast(other, SkillDefinition.SWIFTNESS_AURA);
            friend.setPos(source.position().add(0, 0, 20));
            // Changing teams stops refresh, without revoking the two active copies.
            source.level().getScoreboard().removePlayerFromTeam(friend.getScoreboardName());
        });
        helper.runAtTickTime(15, () -> check(Auras.passive(friend, Auras.Kind.SWIFTNESS), "Exit grace remains initially"));
        helper.runAtTickTime(35, () -> {
            near(friend.getAttributeValue(Attributes.MOVEMENT_SPEED), .12, "Coverage expires; active continues");
            PlayerProfiles.switchClass(source, PlayerClass.REAVER);
            check(Auras.active(friend, Auras.Kind.SWIFTNESS), "Other source survives class cleanup");
        });
        helper.runAtTickTime(100, () -> check(Auras.active(friend, Auras.Kind.SWIFTNESS), "Longer copy survives shorter expiry"));
        helper.runAtTickTime(177, () -> {
            try { near(friend.getAttributeValue(Attributes.MOVEMENT_SPEED), .1, "All copies expire independently"); helper.succeed(); }
            finally { f.close(); }
        });
    }

    public static void dodge(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var one = f.player(PlayerClass.STALKER, 0); var two = f.player(PlayerClass.STALKER, 1);
            var friend = f.player(PlayerClass.REAVER, 2); f.team(one, two, friend);
            var pet = f.wolf(friend, 3); var enemy = f.target(4);
            f.equip(one, SkillDefinition.DODGE_AURA, 0); f.equip(two, SkillDefinition.DODGE_AURA, 0);
            f.coverage(one); f.coverage(two);
            near(RpgStats.dodgeChance(pet), .1, "Pets receive aura Dodge without broad attribute injection");
            friend.getAttribute(RpgStats.DODGE).setBaseValue(1);
            near(RpgStats.dodgeChance(friend), .85, "Exact ordinary cap without random assertions");
            f.cast(one, SkillDefinition.DODGE_AURA); f.cast(two, SkillDefinition.DODGE_AURA);
            friend.hurtServer(one.level(), one.damageSources().explosion(enemy, enemy), 1);
            check(Auras.active(friend, Auras.Kind.DODGE), "Explosion retains guarantee");
            friend.invulnerableTime = 0;
            check(!friend.hurtServer(one.level(), one.damageSources().mobAttack(enemy), 3), "Guarantee dodges eligible hit");
            check(!Auras.active(friend, Auras.Kind.DODGE), "Hit consumes every overlapping guarantee");
            check(!pet.hurtServer(one.level(), one.damageSources().mobAttack(enemy), 3), "Pet gets guaranteed Dodge");
            check(!Auras.active(pet, Auras.Kind.DODGE), "Pet guarantee consumed");
            helper.succeed();
        }
    }

    public static void fortitude(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var source = f.player(PlayerClass.REAVER, 0); var other = f.player(PlayerClass.REAVER, 1);
            var friend = f.player(PlayerClass.REAVER, 2); var enemy = f.player(PlayerClass.REAVER, 5);
            f.team(source, other, friend);
            f.equip(source, SkillDefinition.FORTITUDE_AURA, 0); f.equip(other, SkillDefinition.FORTITUDE_AURA, 0);
            f.coverage(source); f.coverage(other);
            near(friend.getAttributeValue(Attributes.ARMOR), 4, "Fortitude armor strongest only");
            near(friend.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 2, "Native toughness");
            f.cast(source, SkillDefinition.FORTITUDE_AURA);
            friend.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20); friend.setAbsorptionAmount(10);
            ClassResources.set(friend, PlayerClass.REAVER, 0); ClassResources.set(enemy, PlayerClass.REAVER, 0);
            var hit = AuraDamage.before(friend);
            friend.hurtServer(source.level(), source.damageSources().playerAttack(enemy), 4);
            check(hit.landed(friend) && Auras.shield(friend) < 6 && Auras.shield(friend) > 2, "Mitigated hit consumes separate shield");
            near(friend.getHealth(), 20, "Shield-only damage preserves health"); near(friend.getAbsorptionAmount(), 10, "Shield precedes unrelated absorption");
            double spent = 6 - Auras.shield(friend);
            near(ClassResources.current(friend, PlayerClass.REAVER), spent * 2, "Shield-only incoming Rage counted once");
            near(ClassResources.current(enemy, PlayerClass.REAVER), spent, "Shield-only outgoing Rage counted once");
            f.cast(other, SkillDefinition.FORTITUDE_AURA); near(Auras.shield(friend), 6, "Fresh owner replaces shield capacity");
            SkillLoadouts.bind(source, 0, null); near(Auras.shield(friend), 6, "Old owner cannot remove replacement");
            friend.invulnerableTime = 0; friend.hurtServer(source.level(), source.damageSources().playerAttack(enemy), 12);
            near(Auras.shield(friend), 0, "Overflow depletes shield"); check(friend.getAbsorptionAmount() < 10, "Overflow reaches ordinary absorption");
            SkillLoadouts.bind(other, 0, null);
            check(friend.getAbsorptionAmount() > 0, "Source cleanup preserves golden absorption");
            f.equip(source, SkillDefinition.FORTITUDE_AURA, 0); f.cast(source, SkillDefinition.FORTITUDE_AURA);
            // Rend must apply bleed on shield-only damage and cannot refund outgoing skill Rage.
            source.setPos(source.position().add(-10, 0, 0)); other.setPos(other.position().add(-10, 0, 0));
            f.learn(enemy, SkillDefinition.REND); enemy.setPos(friend.position().add(0, 0, -2)); enemy.setYRot(0);
            enemy.getAttribute(RpgStats.DAMAGE).setBaseValue(1); friend.invulnerableTime = 0;
            ClassResources.set(enemy, PlayerClass.REAVER, 100); f.reset(enemy, SkillDefinition.REND);
            Skills.activate(enemy, SkillDefinition.REND);
            check(Bleeding.count(friend) == 1, "Shield-only Rend applies exactly one bleed");
            near(ClassResources.current(enemy, PlayerClass.REAVER), 80, "Skill shield damage grants no outgoing Rage");
            helper.succeed();
        }
    }

    public static void ripping(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var source = f.player(PlayerClass.REAVER, 0); var target = f.target(2);
            source.getAttribute(RpgStats.DAMAGE).setBaseValue(10);
            f.equip(source, SkillDefinition.RIPPING_AURA, 0); f.equip(source, SkillDefinition.RAGING_AURA, 1);
            f.coverage(source); f.cast(source, SkillDefinition.RIPPING_AURA); f.cast(source, SkillDefinition.RAGING_AURA);
            var damage = source.damageSources().playerAttack(source);
            target.hurtServer(source.level(), damage, 10);
            near(target.getHealth(), 188, "Raging boosts direct hit once");
            near(target.getData(RpgData.BLEED).stacks().getFirst().damage(), 1.4, "Ripping plus Raging snapshots additively");
            target.invulnerableTime = 0; target.hurtServer(source.level(), damage, 10);
            check(Bleeding.count(target) == 1, "Attacker/target pair shares 20-tick limit");
            var second = f.target(3); second.hurtServer(source.level(), damage, 1);
            check(Bleeding.count(second) == 1, "Different targets have independent limits");
            f.learn(source, SkillDefinition.REND); f.reset(source, SkillDefinition.REND); target.invulnerableTime = 0;
            Skills.activate(source, SkillDefinition.REND);
            check(Bleeding.count(target) == 2, "Rend adds its own one stack despite Ripping");
            SkillLoadouts.bind(source, 0, null); SkillLoadouts.bind(source, 1, null);
            near(target.getData(RpgData.BLEED).stacks().getFirst().damage(), 1.4, "Cleanup preserves committed bleed");
            float health = target.getHealth();
            for (int i = 0; i < 20; i++) { NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(target)); }
            near(target.getHealth(), health - 2.8, "Bleed pulse never reapplies outgoing bonus");
            helper.succeed();
        }
    }

    public static void slows(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var weaver = f.player(PlayerClass.WEAVER, 0); var target = f.target(2);
            f.equip(weaver, SkillDefinition.FREEZING_AURA, 0); f.coverage(weaver);
            double base = target.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base * .9, "Freezing passive ten percent slow");
            Chilled.apply(target, weaver, 100);
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base * .7, "Chilled suppresses weaker aura");
            var stalker = f.player(PlayerClass.STALKER, 1);
            var patch = new TrapEntity(Skills.TRAP.get(), stalker.level()); invoke(TrapEntity.class, "initialize", patch, new Class<?>[]{ServerPlayer.class, TrapKind.class, int.class}, stalker, TrapKind.FROST, 160);
            invoke(FrostSlows.class, "add", null, new Class<?>[]{LivingEntity.class, TrapEntity.class}, target, patch);
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base * .5, "Frost suppresses other slows");
            invoke(FrostSlows.class, "remove", null, new Class<?>[]{LivingEntity.class, TrapEntity.class}, target, patch);
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base * .7, "Weaker retained Chilled resumes");
            target.removeEffect(Chilled.EFFECT);
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base * .9, "Aura resumes after Chilled removal");
            Rooted.apply(target, 100); target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100));
            Chilled.apply(target, weaver, 100); Auras.cleanse(target);
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base, "Cleanse clears Covenant and vanilla slows");
            check(Rooted.contains(target), "Cleanse retains root");
            f.cast(weaver, SkillDefinition.FREEZING_AURA); Auras.cleanse(target);
            check(Auras.frozen(target), "Cleanse retains full freeze");
            float health = weaver.getHealth();
            target.doHurtTarget(weaver.level(), weaver);
            near(weaver.getHealth(), health, "Frozen native mob cannot perform melee");
            f.coverage(weaver);
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base * .9, "Field can reapply after cleanse");
            helper.succeed();
        }
    }

    public static void casting(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var weaver = f.player(PlayerClass.WEAVER, 0); var target = f.player(PlayerClass.WEAVER, 2);
            f.equip(weaver, SkillDefinition.ICY_VEINS, 0); f.equip(weaver, SkillDefinition.FREEZING_AURA, 1);
            f.learn(weaver, SkillDefinition.FIRE_BOLT); f.learn(weaver, SkillDefinition.ICE_BOLT);
            near(weaver.getAttributeValue(RpgStats.COOLDOWN), .9, "Icy Veins native cooldown passive");
            f.cast(weaver, SkillDefinition.ICY_VEINS);
            check(Skills.cooldownRemaining(weaver, SkillDefinition.ICY_VEINS) == 540, "Icy Veins reduces its own new cooldown");
            f.reset(weaver, SkillDefinition.FIRE_BOLT); Skills.activate(weaver, SkillDefinition.FIRE_BOLT);
            check(!Skills.isCasting(weaver), "First spell has no wind-up");
            f.reset(weaver, SkillDefinition.ICE_BOLT); Skills.activate(weaver, SkillDefinition.ICE_BOLT);
            check(!Skills.isCasting(weaver), "Second distinct spell also instant");
            long cooldown = Skills.cooldownRemaining(weaver, SkillDefinition.ICE_BOLT);
            SkillLoadouts.bind(weaver, 0, null);
            check(Skills.cooldownRemaining(weaver, SkillDefinition.ICE_BOLT) == cooldown, "Removal never rewrites existing cooldown");
            f.learn(target, SkillDefinition.ARCANE_BOLT); f.reset(target, SkillDefinition.ARCANE_BOLT);
            Skills.activate(target, SkillDefinition.ARCANE_BOLT); check(Skills.isCasting(target), "Pending cast started");
            f.cast(weaver, SkillDefinition.FREEZING_AURA);
            check(Auras.frozen(target) && !Skills.isCasting(target), "Freeze cancels pending Covenant cast");
            near(ClassResources.current(target, PlayerClass.WEAVER), 100, "Interrupted cast spends nothing");
            check(Skills.cooldownRemaining(target, SkillDefinition.ARCANE_BOLT) == 0, "Interrupted cast adds no cooldown");
            Skills.activate(target, SkillDefinition.ARCANE_BOLT); check(!Skills.isCasting(target), "Frozen cannot recast");
            target.setDeltaMovement(Vec3.ZERO); target.moveRelative(1, new Vec3(1, 0, 0));
            near(target.getDeltaMovement().length(), 0, "Freeze blocks voluntary input");
            target.setDeltaMovement(.2, .3, 0); target.jumpFromGround();
            near(target.getDeltaMovement().y, .3, "Freeze blocks jump and retains external velocity");
            var victim = f.target(4); target.attack(victim); near(victim.getHealth(), 200, "Frozen player attack denied");
            target.hurtServer(target.level(), target.damageSources().generic(), 1);
            check(Auras.frozen(target) && target.getHealth() < 20, "Damage does not break freeze");
            weaver.setPos(weaver.position().add(0, 0, 30));
            f.cast(weaver, SkillDefinition.FREEZING_AURA);
            near(ClassResources.current(weaver, PlayerClass.WEAVER), 60, "Empty Freezing still spends its committed cost");
            helper.succeed();
        }
    }

    public static void fireAndShock(GameTestHelper helper) {
        var f = new Fixture(helper);
        var source = f.player(PlayerClass.WEAVER, 0); var second = f.player(PlayerClass.WEAVER, 1);
        var target = f.target(2); target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        f.equip(source, SkillDefinition.IMMOLATION_AURA, 0); f.equip(second, SkillDefinition.IMMOLATION_AURA, 0);
        f.equip(source, SkillDefinition.SHOCK_AURA, 1); f.equip(source, SkillDefinition.FLAME_CLOAK, 2);
        f.coverage(source); f.coverage(second);
        f.cast(source, SkillDefinition.IMMOLATION_AURA);
        target.invulnerableTime = 17;
        var seen = new ArrayList<java.util.UUID>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post> listener = event -> {
            if (event.getEntity() == target && event.getSource().is(AuraDamage.IMMOLATION)) { seen.add(event.getSource().getEntity().getUUID()); }
        };
        NeoForge.EVENT_BUS.addListener(listener); f.listeners.add(listener);
        helper.onEachTick(f::tick);
        helper.runAtTickTime(24, () -> {
            check(seen.size() == 1 && seen.getFirst().equals(source.getUUID()), "Overlapping fire uses one pulse and strongest attribution");
            near(target.getHealth(), 200 - 1.3 * 1.1, "Fire bonus and Shock apply once");
            SkillLoadouts.bind(source, 0, null);
        });
        helper.runAtTickTime(45, () -> {
            try {
                check(seen.size() == 2 && seen.getLast().equals(second.getUUID()), "Source handoff retains shared pulse deadline");
                near(target.getHealth(), 200 - 1.3 * 1.1 - 1.1, "Handoff has no stacked pulse");
                f.cast(source, SkillDefinition.FLAME_CLOAK);
                source.hurtServer(source.level(), source.damageSources().mobAttack(target), 1);
                check(target.getRemainingFireTicks() >= 80, "Damaging melee ignites attacker");
                source.clearFire(); source.invulnerableTime = 0;
                float health = source.getHealth(); source.hurtServer(source.level(), source.damageSources().onFire(), 5);
                near(source.getHealth(), health - 4, "Flame Cloak reduces fire-tagged incoming damage twenty percent");
                helper.succeed();
            } finally { f.close(); }
        });
    }

    public static void chain(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var source = f.player(PlayerClass.WEAVER, 0);
            f.equip(source, SkillDefinition.SHOCK_AURA, 0); f.cast(source, SkillDefinition.SHOCK_AURA);
            f.learn(source, SkillDefinition.LIGHTNING_BOLT); source.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
            var one = f.target(4); var two = f.target(7); var three = f.target(10); var four = f.target(13);
            f.reset(source, SkillDefinition.LIGHTNING_BOLT); Skills.activate(source, SkillDefinition.LIGHTNING_BOLT);
            near(one.getHealth(), 194, "Initial base power six"); near(two.getHealth(), 197, "First jump power three");
            near(three.getHealth(), 197, "Second jump retains power three"); near(four.getHealth(), 200, "At most three total targets");
            one.invulnerableTime = two.invulnerableTime = three.invulnerableTime = 0;
            f.block(BlockPos.containing(source.getX(), source.getEyeY(), source.getZ() + 5));
            f.reset(source, SkillDefinition.LIGHTNING_BOLT); Skills.activate(source, SkillDefinition.LIGHTNING_BOLT);
            near(one.getHealth(), 188, "First hit still lands"); near(two.getHealth(), 197, "Cover blocks arc");
            helper.succeed();
        }
    }

    public static void enchanting(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var source = f.player(PlayerClass.WEAVER, 0); var fighter = f.player(PlayerClass.REAVER, 1);
            var target = f.target(3); f.team(source, fighter);
            f.equip(source, SkillDefinition.ENCHANTING_AURA, 0); f.coverage(source); f.cast(source, SkillDefinition.ENCHANTING_AURA);
            near(fighter.getAttributeValue(RpgStats.MANA_REGENERATION), 1.2, "Native regeneration modifier");
            ClassResources.set(fighter, PlayerClass.WEAVER, 0); fighter.doTick();
            near(ClassResources.current(fighter, PlayerClass.WEAVER), 0, "Inactive Mana never regenerates");
            fighter.getAttribute(RpgStats.DAMAGE).setBaseValue(4);
            for (int i = 0; i < 25; i++) { fighter.doTick(); }
            fighter.attack(target); near(target.getHealth(), 194, "Fully charged native melee plus two magic");
            target.invulnerableTime = 0;
            float strength = fighter.getAttackStrengthScale(.5f); float health = target.getHealth();
            fighter.attack(target);
            double ordinary = 4 * (.2 + strength * strength * .8);
            near(target.getHealth(), health - ordinary - 2 * strength, "Weak swing scales magic by pre-hit strength");
            var arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(source.level(), fighter, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            AuraDamage.snapshot(arrow, fighter, .5f);
            near(arrow.getData(RpgData.ENCHANTED_SHOT), 1, "Half bow draw captures one magic");
            SkillLoadouts.bind(source, 0, null);
            target.invulnerableTime = 0; health = target.getHealth();
            target.hurtServer(source.level(), source.damageSources().arrow(arrow, fighter), 2);
            near(target.getHealth(), health - 3, "Committed shot keeps magic after source removal");
            target.invulnerableTime = 0; health = target.getHealth();
            target.hurtServer(source.level(), source.damageSources().arrow(arrow, fighter), 2);
            near(target.getHealth(), health - 2, "One bonus per projectile per target");
            var other = f.target(5); other.invulnerableTime = 0;
            other.hurtServer(source.level(), source.damageSources().arrow(arrow, fighter), 2);
            near(other.getHealth(), 197, "Same piercing shot can enhance another target");
            check(target.invulnerableTime == 20, "Extra magic preserves initial hit immunity");
            arrow.discard(); helper.succeed();
        }
    }

    public static void lifecycle(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var source = f.player(PlayerClass.REAVER, 0); var friend = f.player(PlayerClass.WEAVER, 2); f.team(source, friend);
            f.equip(source, SkillDefinition.FORTITUDE_AURA, 0); f.coverage(source); f.cast(source, SkillDefinition.FORTITUDE_AURA);
            var reporter = new net.minecraft.util.ProblemReporter.Collector();
            var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, source.level().registryAccess());
            friend.saveWithoutId(output); var data = output.buildResult(); data.remove("UUID");
            var loaded = f.player(PlayerClass.WEAVER, 10);
            loaded.load(net.minecraft.world.level.storage.TagValueInput.create(reporter, source.level().registryAccess(), data));
            check(Auras.shield(loaded) == 0 && !loaded.hasData(RpgData.AURAS), "Native save/load has no temporary aura state");
            near(loaded.getAttributeValue(Attributes.ARMOR), 0, "Temporary armor absent on load");
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(friend,
                    net.minecraft.world.level.Level.OVERWORLD, net.minecraft.world.level.Level.NETHER));
            near(Auras.shield(friend), 0, "Recipient dimension clears buffs");
            f.cast(source, SkillDefinition.FORTITUDE_AURA);
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(source));
            near(Auras.shield(friend), 0, "Source logout clears remote shield");
            check(Skills.cooldownRemaining(source, SkillDefinition.FORTITUDE_AURA) > 0, "Lifecycle keeps saved cooldown");
            f.equip(source, SkillDefinition.FORTITUDE_AURA, 0); f.cast(source, SkillDefinition.FORTITUDE_AURA);
            var replacement = f.player(PlayerClass.REAVER, 12); replacement.restoreFrom(source, false);
            near(Auras.shield(replacement), 0, "Death replacement never copies temporary shield");
            source.hurtServer(source.level(), source.damageSources().genericKill(), Float.MAX_VALUE);
            near(Auras.shield(friend), 0, "Actual caster death removes remote shield");
            check(!source.hasData(RpgData.AURAS), "Death discards source references");
            check(reporter.isEmpty(), "Native serialization succeeds"); helper.succeed();
        }
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final List<ServerPlayer> players = new ArrayList<>();
        final List<Entity> entities = new ArrayList<>();
        final List<Object> listeners = new ArrayList<>();
        final List<BlockPos> blocks = new ArrayList<>();
        final List<net.minecraft.world.scores.PlayerTeam> teams = new ArrayList<>();
        final Vec3 origin;
        Fixture(GameTestHelper helper) { this.helper = helper; origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 110, 1))); }
        ServerPlayer player(PlayerClass type, double z) {
            var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), "aura" + UUID.randomUUID().toString().substring(0, 8)), false);
            var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            player.setGameMode(GameType.SURVIVAL); player.connection.markClientLoaded();
            player.setPos(origin.add(0, 0, z)); player.setNoGravity(true); player.setYRot(0); player.setXRot(0);
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            PlayerProfiles.selectClass(player, type); players.add(player); return player;
        }
        LivingEntity target(double z) {
            var target = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            target.setPos(origin.add(0, 0, z)); target.setNoGravity(true); target.setNoAi(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); target.setHealth(200);
            helper.getLevel().addFreshEntity(target); entities.add(target); return target;
        }
        LivingEntity wolf(ServerPlayer owner, double z) {
            var wolf = EntityType.WOLF.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            wolf.tame(owner); wolf.setPos(origin.add(0, 0, z)); wolf.setNoAi(true); wolf.setNoGravity(true);
            helper.getLevel().addFreshEntity(wolf); entities.add(wolf); return wolf;
        }
        void team(ServerPlayer... members) {
            var board = helper.getLevel().getScoreboard(); var team = board.addPlayerTeam("a" + UUID.randomUUID().toString().substring(0, 8)); teams.add(team);
            for (var member : members) { board.addPlayerToTeam(member.getScoreboardName(), team); }
        }
        void learn(ServerPlayer player, SkillDefinition skill) {
            var node = SkillTree.find(skill.id()).orElseThrow();
            node.prerequisite().ifPresent(id -> learn(player, SkillDefinition.find(id).orElseThrow()));
            FixtureSkills.learn(player, skill.id(), true);
        }
        void equip(ServerPlayer player, SkillDefinition skill, int slot) { learn(player, skill); SkillLoadouts.bind(player, slot, skill.id()); }
        void reset(ServerPlayer player, SkillDefinition skill) {
            player.getData(RpgData.SKILL_RUNTIME).cancelCast(player); player.removeData(RpgData.SKILL_RUNTIME);
            player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(skill.id(), 0));
            ClassResources.set(player, PlayerProfiles.getSelectedClass(player).orElseThrow(), 100);
        }
        void cast(ServerPlayer player, SkillDefinition skill) {
            reset(player, skill); var result = Skills.activate(player, skill);
            check(result.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents feedback
                    && feedback.getKey().equals("commands.covenant.skill.used"), "Aura activation: " + result.getString());
        }
        void coverage(ServerPlayer player) {
            Auras.reconcile(player); Auras.refresh(player);
        }

        void tick() { for (var player : players) { player.doTick(); } }
        void block(BlockPos pos) { helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()); blocks.add(pos); }
        @Override public void close() {
            listeners.forEach(NeoForge.EVENT_BUS::unregister);
            for (var player : players) { Auras.clear(player); helper.getLevel().getServer().getPlayerList().remove(player); }
            entities.forEach(Entity::discard);
            blocks.forEach(pos -> helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
            teams.forEach(helper.getLevel().getScoreboard()::removePlayerTeam);
        }
    }
    private static void invoke(Class<?> type, String name, Object instance, Class<?>[] parameters, Object... arguments) {
        try { var method = type.getDeclaredMethod(name, parameters); method.setAccessible(true); method.invoke(instance, arguments); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
    private static void near(double actual, double expected, String message) { check(Math.abs(actual - expected) < .001, message + ": " + actual + " != " + expected); }
}
