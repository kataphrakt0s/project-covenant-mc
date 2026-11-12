package dev.covenant.test;

import dev.covenant.test.fixture.TestPlayers;
import dev.covenant.test.fixture.FixtureSkills;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.Bleeding;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLoadouts;
import dev.covenant.rpg.skills.Skills;
import dev.covenant.rpg.stats.RpgStats;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Real native damage, server targeting and serialization; controlled pulses also exercise paused targets. */
final class BleedGameTests {
    private BleedGameTests() {}

    static void rend(GameTestHelper helper) {
        var players = new ArrayList<ServerPlayer>();
        var targets = new ArrayList<LivingEntity>();
        try {
            var source = player(helper, players, 0);
            near(source.entityInteractionRange(), 3, "Native default Survival reach");
            whiff(source, "Empty crosshair is a paid whiff");
            var target = target(helper, targets, source, 3.5);
            whiff(source, "Beyond normal reach whiffs");
            target.setPos(source.getX(), source.getY(), source.getZ() + 2);
            var wall = BlockPos.containing(source.getX(), source.getEyeY(), source.getZ() + 1);
            helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            whiff(source, "Walls stop hits but do not reject activation");
            helper.getLevel().setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
            var shortWeapon = new ItemStack(Items.STICK);
            shortWeapon.set(DataComponents.ATTACK_RANGE, new AttackRange(0, 1, 0, 1, 0, 1));
            source.setItemSlot(EquipmentSlot.MAINHAND, shortWeapon);
            whiff(source, "Short native item reach causes a whiff");
            source.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            check(key(Skills.activate(source, SkillDefinition.REND), "used"), "Rend executes");
            near(target.getHealth(), 190, "Initial hit is 100 percent attack damage");
            near(ClassResources.current(source, PlayerClass.REAVER), 80, "Rend costs 20 Rage");
            check(Skills.cooldownRemaining(source, SkillDefinition.REND) == 40, "Two-second cooldown");
            var wound = target.getData(RpgData.BLEED).stacks().getFirst();
            near(wound.damage(), 1, "Stack snapshots ten percent attack damage");
            check(wound.remainingTicks() == 100, "Five-second base duration");
            source.getAttribute(RpgStats.DAMAGE).setBaseValue(99);
            near(wound.damage(), 1, "Later damage changes do not alter the wound");
            check(key(Skills.activate(source, SkillDefinition.REND), "cooldown"), "Duplicate activation rejected");

            var extended = player(helper, players, 12);
            var longWeapon = new ItemStack(Items.STICK);
            longWeapon.set(DataComponents.ATTACK_RANGE, new AttackRange(0, 5, 0, 5, 0, 1));
            extended.setItemSlot(EquipmentSlot.MAINHAND, longWeapon);
            var distant = target(helper, targets, extended, 4);
            var extendedResult = Skills.activate(extended, SkillDefinition.REND);
            check(key(extendedResult, "used") && Bleeding.count(distant) == 1,
                    "Rend follows extended native item reach: " + extendedResult.getString() + "; pick="
                            + extended.getAttackRangeWith(longWeapon).getClosesetHit(extended, 1, entity -> entity.isPickable()));

            var absorbed = player(helper, players, 24);
            absorbed.getAttribute(RpgStats.RESOURCE_COST).setBaseValue(0.5);
            absorbed.getAttribute(RpgStats.COOLDOWN).setBaseValue(0.5);
            absorbed.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(2);
            var shieldedHealth = target(helper, targets, absorbed, 2);
            shieldedHealth.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(30);
            shieldedHealth.setAbsorptionAmount(30);
            check(key(Skills.activate(absorbed, SkillDefinition.REND), "used"), "Absorbed Rend executes");
            near(shieldedHealth.getHealth(), 200, "Absorption protects health");
            near(shieldedHealth.getAbsorptionAmount(), 20, "Initial strike consumes absorption");
            check(Bleeding.count(shieldedHealth) == 1 && shieldedHealth.getData(RpgData.BLEED).stacks().getFirst().remainingTicks() == 200,
                    "Absorption damage applies bleed and native duration scaling");
            near(ClassResources.current(absorbed, PlayerClass.REAVER), 90, "Native cost scaling");
            check(Skills.cooldownRemaining(absorbed, SkillDefinition.REND) == 20, "Native cooldown scaling");

            var dodged = player(helper, players, 36);
            var defender = player(helper, players, 38);
            defender.setPos(dodged.getX(), dodged.getY(), dodged.getZ() + 2);
            defender.getAttribute(RpgStats.DODGE).setBaseValue(1); defender.getRandom().setSeed(0);
            check(key(Skills.activate(dodged, SkillDefinition.REND), "used"), "Dodged strike is still attempted");
            check(Bleeding.count(defender) == 0, "Dodge prevents bleed");
            near(ClassResources.current(dodged, PlayerClass.REAVER), 80, "Defended strike still spends Rage");

            var immune = player(helper, players, 48);
            var invulnerable = target(helper, targets, immune, 2);
            invulnerable.setInvulnerable(true);
            var immuneResult = Skills.activate(immune, SkillDefinition.REND);
            check(key(immuneResult, "used") && Bleeding.count(invulnerable) == 0,
                    "Invulnerable target takes no wound: " + immuneResult.getString() + " stacks=" + Bleeding.count(invulnerable));

            var cancelled = player(helper, players, 60);
            var cancelledTarget = target(helper, targets, cancelled, 2);
            Bleeding.apply(cancelledTarget, source, 1, 50);
            Consumer<LivingIncomingDamageEvent> cancel = event -> {
                if (event.getEntity() == cancelledTarget) { event.setCanceled(true); }
            };
            NeoForge.EVENT_BUS.addListener(cancel);
            try {
                var feedback = Skills.activate(cancelled, SkillDefinition.REND);
                check(key(feedback, "used"), "Canceled damage is an attempted strike: " + feedback.getString()
                        + " at " + cancelled.position() + " aiming " + cancelled.getHeadLookAngle()
                        + " toward " + cancelledTarget.position());
                check(Bleeding.count(cancelledTarget) == 1
                        && cancelledTarget.getData(RpgData.BLEED).stacks().getFirst().remainingTicks() == 50,
                        "Canceled damage neither adds nor refreshes existing stacks");
            } finally { NeoForge.EVENT_BUS.unregister(cancel); }

            var blocked = player(helper, players, 72);
            var guard = player(helper, players, 74);
            guard.setPos(blocked.getX(), blocked.getY(), blocked.getZ() + 2);
            guard.setYRot(180);
            guard.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            guard.startUsingItem(InteractionHand.OFF_HAND);
            helper.onEachTick(guard::doTick);
            helper.runAtTickTime(8, () -> {
                try {
                    float health = guard.getHealth();
                    check(key(Skills.activate(blocked, SkillDefinition.REND), "used"), "Shielded strike is attempted");
                    near(guard.getHealth(), health, "Native shield fully blocks the initial strike");
                    check(Bleeding.count(guard) == 0, "Fully blocked strike cannot apply bleed");
                    helper.succeed();
                } finally { cleanup(helper, players, targets); }
            });
        } catch (RuntimeException error) { cleanup(helper, players, targets); throw error; }
    }

    static void pulses(GameTestHelper helper) {
        var players = new ArrayList<ServerPlayer>();
        var targets = new ArrayList<LivingEntity>();
        try {
            var one = player(helper, players, 0, 160);
            var two = player(helper, players, 8, 160);
            var target = target(helper, targets, one, 2);
            target.hurtServer(helper.getLevel(), one.damageSources().playerAttack(one), 20);
            Bleeding.apply(target, one, 1, 100);
            pulse(target, 7);
            check(target.getData(RpgData.BLEED).untilPulse() == 13, "Pulse countdown advances with active ticks");
            Bleeding.apply(target, one, 2, 100);
            Bleeding.apply(target, one, 3, 100);
            Bleeding.apply(target, two, 4, 100);
            var stacks = target.getData(RpgData.BLEED).stacks();
            check(stacks.size() == 3 && stacks.getFirst().damage() == 2 && stacks.getLast().attacker().equals(two.getUUID()),
                    "Shared cap replaces the oldest with the new attacker's stack");
            check(stacks.stream().allMatch(s -> s.remainingTicks() == 100), "Reapplication refreshes all durations");
            check(target.getData(RpgData.BLEED).untilPulse() == 13, "Reapplication never resets the pulse");
            var credits = new ArrayList<UUID>();
            Consumer<LivingDamageEvent.Post> credit = event -> {
                if (event.getEntity() == target && event.getSource().is(Bleeding.DAMAGE_TYPE)) {
                    credits.add(event.getSource().getEntity().getUUID());
                }
            };
            NeoForge.EVENT_BUS.addListener(credit);
            try {
                pulse(target, 12);
                near(target.getHealth(), 180, "No early pulse");
                pulse(target, 1);
            } finally { NeoForge.EVENT_BUS.unregister(credit); }
            near(target.getHealth(), 171, "All three stacks damage independently despite normal hurt immunity");
            check(credits.equals(List.of(one.getUUID(), one.getUUID(), two.getUUID())), "Per-stack attacker credit");
            check(target.invulnerableTime == 20, "Pulse preserves the prior immunity deadline");
            target.hurtServer(helper.getLevel(), one.damageSources().playerAttack(one), 25);
            near(target.getHealth(), 166, "Pulse preserves prior hit strength for native differential damage");
            target.invulnerableTime = 0;
            target.hurtServer(helper.getLevel(), one.damageSources().playerAttack(one), 3);
            near(target.getHealth(), 163, "Bleed does not suppress later melee");
            check(Bleeding.consume(target) == 3 && Bleeding.count(target) == 0, "Consumption removes every owner's stacks");
            Bleeding.apply(target, one, 1, 100);
            check(target.getData(RpgData.BLEED).untilPulse() == 20, "New bleeding starts a fresh schedule");
            pulse(target, 100);
            near(target.getHealth(), 158, "Five-second stack gets exactly five pulses including expiry");
            check(!target.hasData(RpgData.BLEED), "Expired state is removed");

            var armored = target(helper, targets, one, 4);
            armored.getAttribute(Attributes.ARMOR).setBaseValue(30);
            armored.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(20);
            Bleeding.apply(armored, one, 10, 100);
            pulse(armored, 20);
            near(armored.getHealth(), 190, "Armor and toughness do not mitigate bleed");
            armored.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 0));
            pulse(armored, 20);
            near(armored.getHealth(), 182, "Resistance still mitigates bleed");
            armored.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20);
            armored.setAbsorptionAmount(20);
            pulse(armored, 20);
            near(armored.getHealth(), 182, "Absorption still protects health from bleed");
            near(armored.getAbsorptionAmount(), 12, "Bleed consumes absorption after mitigation");
            armored.setInvulnerable(true);
            pulse(armored, 20);
            near(armored.getAbsorptionAmount(), 12, "Actual invulnerability prevents bleed damage");

            var dodging = player(helper, players, 16, 160);
            dodging.getAttribute(RpgStats.DODGE).setBaseValue(1); dodging.getRandom().setSeed(0);
            Bleeding.apply(dodging, one, 2, 100);
            float health = dodging.getHealth();
            pulse(dodging, 20);
            near(dodging.getHealth(), health - 2, "Bleed ticks cannot be dodged");
            helper.succeed();
        } finally { cleanup(helper, players, targets); }
    }

    static void persistence(GameTestHelper helper) {
        var players = new ArrayList<ServerPlayer>();
        var targets = new ArrayList<LivingEntity>();
        var attacker = player(helper, players, 0, 180);
        var original = target(helper, targets, attacker, 2);
        Bleeding.apply(original, attacker, 1, 100);
        pulse(original, 7);
        var reporter = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(reporter, helper.getLevel().registryAccess());
        original.saveWithoutId(output);
        var saved = output.buildResult();
        saved.remove("UUID");
        var loaded = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        targets.add(loaded);
        loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), saved));
        check(reporter.isEmpty(), "Native mob serialization has no errors: " + reporter.getReport());
        var restored = loaded.getData(RpgData.BLEED);
        check(restored.count() == 1 && restored.untilPulse() == 13 && restored.stacks().getFirst().remainingTicks() == 93,
                "Native save/load retains both relative timers");
        check(restored.stacks().getFirst().attacker().equals(attacker.getUUID()), "Saved ownership survives reload");
        near(restored.stacks().getFirst().damage(), 1, "Saved damage survives reload");
        var returning = player(helper, players, 8, 180);
        var playerSave = TagValueOutput.createWithContext(reporter, helper.getLevel().registryAccess());
        Bleeding.apply(attacker, returning, 2, 100);
        attacker.saveWithoutId(playerSave);
        var playerData = playerSave.buildResult();
        playerData.remove("UUID");
        returning.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), playerData));
        check(Bleeding.count(returning) == 1, "Player save/load retains wounds across reconnect");
        var afterDeath = player(helper, players, 16, 180);
        afterDeath.copyAttachmentsFrom(attacker, true);
        check(Bleeding.count(afterDeath) == 0, "Death replacement does not copy wounds");
        var endReturn = player(helper, players, 24, 180);
        endReturn.copyAttachmentsFrom(attacker, false);
        check(Bleeding.count(endReturn) == 1, "Non-death replacement retains wounds");
        original.discard();
        helper.getLevel().getServer().getPlayerList().remove(attacker);
        helper.runAtTickTime(30, () -> {
            try {
                check(restored.untilPulse() == 13 && restored.stacks().getFirst().remainingTicks() == 93,
                        "Inactive target timers pause despite server time passing");
                pulse(loaded, 12);
                near(loaded.getHealth(), 200, "Resume does not catch up or tick early");
                pulse(loaded, 1);
                near(loaded.getHealth(), 199, "Bleed resumes and damages with attacker offline");
                helper.getLevel().addFreshEntity(loaded);
            } catch (RuntimeException error) { cleanup(helper, players, targets); throw error; }
        });
        helper.runAtTickTime(125, () -> {
            try {
                near(loaded.getHealth(), 195, "Real loaded-entity ticks deliver the four remaining scheduled pulses");
                check(Bleeding.count(loaded) == 0, "Real entity ticking expires the wound without catch-up");
                Bleeding.apply(loaded, returning, 1, 100);
                loaded.hurtServer(helper.getLevel(), helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                check(Bleeding.count(loaded) == 0, "Death clears stored wounds");
                helper.succeed();
            } finally { cleanup(helper, players, targets); }
        });
    }

    private static ServerPlayer player(GameTestHelper helper, List<ServerPlayer> players, int x) {
        return player(helper, players, x, 210);
    }

    private static ServerPlayer player(GameTestHelper helper, List<ServerPlayer> players, int x, int height) {
        // The deprecated helper forces gameMode() to Creative, invalidating defense and reach checks.
        var player = TestPlayers.create(helper);
        players.add(player);
        // Wide fixtures can overlap adjacent GameTest structures; separate each test vertically.
        var origin = helper.absolutePos(new BlockPos(2 + x, height, 2));
        player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
        player.setNoGravity(true);
        player.setGameMode(GameType.SURVIVAL);
        player.connection.markClientLoaded();
        player.setYRot(0); player.setYHeadRot(0); player.setXRot(0);
        PlayerProfiles.selectClass(player, PlayerClass.REAVER);
        FixtureSkills.learn(player, SkillDefinition.REND.id(), true);
        ClassResources.set(player, PlayerClass.REAVER, 100);
        player.getAttribute(RpgStats.DAMAGE).setBaseValue(10);
        return player;
    }

    private static Husk target(GameTestHelper helper, List<LivingEntity> targets, ServerPlayer source, double z) {
        var target = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        target.setPos(source.getX(), source.getY(), source.getZ() + z);
        target.setNoAi(true); target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setHealth(200);
        helper.getLevel().addFreshEntity(target);
        targets.add(target);
        return target;
    }

    private static void pulse(LivingEntity target, int ticks) {
        for (int i = 0; i < ticks; i++) { NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(target)); }
    }

    private static void cleanup(GameTestHelper helper, List<ServerPlayer> players, List<LivingEntity> targets) {
        targets.forEach(LivingEntity::discard);
        players.forEach(player -> {
            if (helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player) {
                helper.getLevel().getServer().getPlayerList().remove(player);
            }
        });
    }

    private static boolean key(Component c, String suffix) {
        return c.getContents() instanceof TranslatableContents text && text.getKey().equals("commands.covenant.skill." + suffix);
    }
    private static void whiff(ServerPlayer source, String message) {
        check(key(Skills.activate(source, SkillDefinition.REND), "used"), message);
        near(ClassResources.current(source, PlayerClass.REAVER), 80, "Whiff spends 20 Rage");
        check(Skills.cooldownRemaining(source, SkillDefinition.REND) == 40, "Whiff starts cooldown");
        source.setData(RpgData.SKILLS, source.getData(RpgData.SKILLS).withCooldown(SkillDefinition.REND.id(), 0));
        source.setData(RpgData.SKILL_RUNTIME, new dev.covenant.rpg.skills.SkillRuntime());
        ClassResources.set(source, PlayerClass.REAVER, 100);
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.0001, message + ": expected " + expected + ", got " + actual);
    }
    private static void check(boolean value, String message) { if (!value) { throw new IllegalStateException(message); } }
}
