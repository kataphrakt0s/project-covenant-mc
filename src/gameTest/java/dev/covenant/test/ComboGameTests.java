package dev.covenant.test;

import com.mojang.authlib.GameProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.*;
import dev.covenant.rpg.stats.RpgStats;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Native server players and damage; clock/step fixtures never change the shared world's time. */
final class ComboGameTests {
    private ComboGameTests() {}

    static void progression(GameTestHelper helper) {
        try (var f = new Fixture(helper, 115)) {
            var p = f.player();
            used(p, SkillDefinition.CLEAVE);
            near(rage(p), 80, "Empty Cleave spends normally");
            check(Skills.cooldownRemaining(p, SkillDefinition.CLEAVE) == 60, "Empty Cleave cooldown");
            ready(p);
            p.getAttribute(RpgStats.COOLDOWN).setBaseValue(.5);
            used(p, SkillDefinition.LUNGING_COMBO);
            int teleportId = (int) get(p.connection, "awaitingTeleport");
            near(rage(p), 60, "Lunge spends 20");
            check(Skills.cooldownRemaining(p, SkillDefinition.LUNGING_COMBO) == 0, "No cooldown during combo");
            check(key(Skills.activate(p, SkillDefinition.LUNGING_COMBO), "busy"), "Moving stage cannot be advanced early");
            ready(p); used(p, SkillDefinition.RIP_AND_RUSH);
            near(rage(p), 30, "Empty circular Rend spends 30 and opens dash window");
            check(p.getData(RpgData.COMBO_DISPLAY).rows().size() == 2, "Independent combo rows");
            var rushRow = row(p, 1);
            ready(p);
            check(key(Skills.activate(p, SkillDefinition.RIP_AND_RUSH), "busy"), "Overlapping dash rejected");
            check(row(p, 1).equals(rushRow) && rage(p) == 30, "Rejection preserves deadline and Rage");
            steps(p, 3);
            check((int) get(p.connection, "awaitingTeleport") == teleportId, "Dash never streams native teleports");
            check(row(p, 0).stage() == 1 && row(p, 0).deadline() == ClassResources.now(p) + 40, "Full first window at movement end");
            p.getAttribute(RpgStats.COOLDOWN).setBaseValue(2);
            p.getAttribute(RpgStats.RESOURCE_COST).setBaseValue(.5);
            ready(p); used(p, SkillDefinition.LUNGING_COMBO);
            near(rage(p), 25, "Empty strike uses current cost modifier");
            check(row(p, 0).stage() == 2 && row(p, 0).deadline() == ClassResources.now(p) + 60, "Second fixed window");
            ready(p); used(p, SkillDefinition.LUNGING_COMBO);
            near(rage(p), 15, "Empty finisher spends and completes");
            check(Skills.cooldownRemaining(p, SkillDefinition.LUNGING_COMBO) == 120, "Stage-one cooldown snapshot retained");
            check(row(p, 1).equals(rushRow), "Other combo unchanged by completion");
            ClassResources.set(p, PlayerClass.REAVER, 0); ready(p);
            check(key(Skills.activate(p, SkillDefinition.RIP_AND_RUSH), "resource"), "Resource rejection");
            check(row(p, 1).equals(rushRow), "Resource rejection never refreshes");
            ClassResources.set(p, PlayerClass.REAVER, 100);
            Rooted.apply(p, 40); ready(p);
            check(key(Skills.activate(p, SkillDefinition.RIP_AND_RUSH), "rooted"), "Root rejects movement follow-up");
            check(row(p, 1).equals(rushRow), "Root preserves original window");
            p.removeEffect(Rooted.EFFECT);
            ready(p); used(p, SkillDefinition.RIP_AND_RUSH); steps(p, 4);
            ready(p); used(p, SkillDefinition.RIP_AND_RUSH);
            check(p.getData(RpgData.COMBO_DISPLAY).rows().isEmpty(), "Whiffed sequence completes and clears display");
            check(Skills.cooldownRemaining(p, SkillDefinition.RIP_AND_RUSH) == 200, "Independent captured rush cooldown");
            helper.succeed();
        }
    }

    static void movement(GameTestHelper helper) {
        var f = new Fixture(helper, 130);
        // Freeze uses a native entity query in a lane beyond the one-block template.
        f.forceLanes(120, 12);
        helper.runBeforeTestEnd(() -> {
            f.close();
            helper.fail("Combo movement fixture chunks did not become entity-ticking before timeout");
        });
        helper.startSequence().thenWaitUntil(f::assertLanesReady).thenExecute(() -> {
        try (f) {
            var p = f.player(); p.setNoGravity(false); p.fallDistance = 100; p.setDeltaMovement(8, -10, 4);
            Vec3 start = p.position();
            used(p, SkillDefinition.LUNGING_COMBO);
            check(p.isNoGravity() && p.fallDistance == 0 && p.getDeltaMovement().equals(Vec3.ZERO), "Replaces momentum and breaks fall");
            p.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(p.getX() + 50, p.getY() + 50, p.getZ() + 50, 90, 20, false, false));
            check(p.position().equals(start), "Movement packet cannot replace owned position");
            p.move(MoverType.PLAYER, new Vec3(5, 5, 5));
            check(p.position().equals(start), "Ordinary movement cannot steer dash");
            p.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(10); p.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(3);
            steps(p, 3);
            near(p.getZ() - start.z, 4, "Fixed four-block travel despite turning/stats");
            near(p.getX(), start.x, "Captured direction unaffected by turning");
            check(!p.isNoGravity() && !Combos.moving(p) && p.getDeltaMovement().equals(Vec3.ZERO), "Ends propulsion and restores gravity");
            near(p.fallDistance, 0, "Final movement clears fall tracking");
            Combos.acknowledgeMovement(p, (long) get(p.getData(RpgData.SKILL_RUNTIME), "awaitingDashSequence"), p.position());
            p.move(MoverType.SELF, new Vec3(0, -1, 0));
            p.doCheckFallDamage(0, -1, 0, false);
            check(p.fallDistance > 0, "Ordinary falling resumes afterward");
            for (int pitch : new int[] {-90, 90}) {
                var vertical = f.player(); vertical.setNoGravity(false); vertical.setXRot(pitch);
                double y = vertical.getY(); used(vertical, SkillDefinition.LUNGING_COMBO); steps(vertical, 3);
                near(vertical.getY() - y, pitch == -90 ? 4 : -4, "Full pitch travel");
                check(!vertical.isNoGravity(), "Vertical travel restores gravity");
            }
            var wall = f.player();
            f.block(BlockPos.containing(wall.position().add(0, 0, 1)));
            f.block(BlockPos.containing(wall.position().add(0, 1, 1)));
            Vec3 wallStart = wall.position(); used(wall, SkillDefinition.LUNGING_COMBO); steps(wall, 1);
            check(!Combos.moving(wall) && wall.getZ() - wallStart.z < .3, "Almost-zero terrain stop completes stage");
            check(row(wall, 0).deadline() == ClassResources.now(wall) + 40, "Collision opens full window");
            var rooted = f.player(); rooted.setNoGravity(false); used(rooted, SkillDefinition.LUNGING_COMBO);
            Rooted.apply(rooted, 60);
            check(!Combos.moving(rooted) && !rooted.isNoGravity() && row(rooted, 0).stage() == 1, "New root completes movement");
            var frozen = f.player(); used(frozen, SkillDefinition.LUNGING_COMBO);
            f.freeze(frozen, 90); steps(frozen, 1);
            check(!Combos.moving(frozen) && row(frozen, 0).stage() == 1, "Freeze completes movement");
            ready(frozen); check(key(Skills.activate(frozen, SkillDefinition.LUNGING_COMBO), "frozen"), "Frozen follow-up rejected");
            var knocked = f.player(); knocked.setNoGravity(false); used(knocked, SkillDefinition.LUNGING_COMBO);
            knocked.knockback(1, 0, 1);
            check(!Combos.moving(knocked) && !knocked.isNoGravity() && knocked.getDeltaMovement().lengthSqr() > 0, "Native knockback survives interruption");
            var resistant = f.player(); resistant.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            used(resistant, SkillDefinition.LUNGING_COMBO); resistant.knockback(1, 0, 1);
            check(Combos.moving(resistant), "Fully resisted knockback does not interrupt");
            helper.succeed();
        }
        });
    }

    static void effects(GameTestHelper helper) {
        var f = new Fixture(helper, 145);
        f.forceLanes(40, 12);
        helper.runBeforeTestEnd(() -> {
            f.close();
            helper.fail("Combo effect fixture chunks did not become entity-ticking before timeout");
        });
        helper.startSequence().thenWaitUntil(f::assertLanesReady).thenExecute(() -> {
        try (f) {
            var p = f.player();
            var front = f.target(p.position().add(0, 0, 2));
            var behind = f.target(p.position().add(0, 0, -2));
            used(p, SkillDefinition.RIP_AND_RUSH);
            near(front.getHealth(), 190, "Circular Rend hits front"); near(behind.getHealth(), 190, "Circular Rend hits rear");
            check(Bleeding.count(front) == 1 && Bleeding.count(behind) == 1, "One normal bleed per enemy");
            front.invulnerableTime = 0;
            ready(p); used(p, SkillDefinition.RIP_AND_RUSH); steps(p, 4);
            near(front.getHealth(), 185, "Dash attempts exactly one half-damage hit");
            check(front.hasEffect(ArmorShred.EFFECT), "Damaging dash applies shared shred");
            var normal = f.target(p.position().add(0, 0, 2));
            var absorbed = f.target(p.position().add(1, 0, 0)); absorbed.setAbsorptionAmount(20);
            var killed = f.target(p.position().add(-1, 0, 0)); killed.setHealth(5);
            var fourth = f.target(p.position().add(0, 0, -2));
            var immune = f.target(p.position().add(0, 0, 1)); immune.setInvulnerable(true);
            var teammate = f.player();
            for (var target : List.of(normal, absorbed, killed, fourth, immune)) {
                Bleeding.apply(target, p, 1, 100); Bleeding.apply(target, teammate, 2, 100);
            }
            ready(p); used(p, SkillDefinition.RIP_AND_RUSH);
            for (var target : List.of(normal, absorbed, killed, fourth)) { check(Bleeding.count(target) == 0, "Consumes all owners, including lethal hit"); }
            check(Bleeding.count(immune) == 2, "Invulnerable finisher preserves bleeds");
            near(p.getAttributeValue(Attributes.MOVEMENT_SPEED), .13, "Speed capped at +30% base after four enemies");
            check((long) get(p.getData(RpgData.SKILL_RUNTIME), "rushSpeedUntil") == ClassResources.now(p) + 80, "Four-second buff snapshot");
            var striker = f.player();
            used(striker, SkillDefinition.LUNGING_COMBO); steps(striker, 3);
            var hit = f.target(striker.position().add(0, 0, 2));
            check(helper.getLevel().getEntitiesOfClass(LivingEntity.class, hit.getBoundingBox()).contains(hit),
                    "Strike fixture target is available to native entity queries");
            ready(striker); used(striker, SkillDefinition.LUNGING_COMBO);
            near(hit.getHealth(), 187.5, "125 percent strike");
            hit.invulnerableTime = 0;
            ready(striker); used(striker, SkillDefinition.LUNGING_COMBO);
            near(hit.getHealth(), 177.5, "100 percent finisher before shred");
            hit.getAttribute(Attributes.ARMOR).setBaseValue(20);
            near(hit.getAttributeValue(Attributes.ARMOR), 16, "Shred reduces armor by 20 percent");
            near(hit.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 0, "No toughness change");
            ArmorShred.apply(hit, teammate, 40);
            check(hit.getEffect(ArmorShred.EFFECT).getDuration() == 100, "Shorter cross-owner application retains duration");
            ArmorShred.apply(hit, teammate, 160);
            near(hit.getAttributeValue(Attributes.ARMOR), 16, "Cross-skill/source refresh never stacks strength");
            check(hit.getEffect(ArmorShred.EFFECT).getDuration() == 160, "Longer application extends duration");
            Combos.clear(striker); check(hit.hasEffect(ArmorShred.EFFECT), "Caster cleanup retains target shred");
            hit.removeAllEffects(); near(hit.getAttributeValue(Attributes.ARMOR), 20, "Native cleansing restores armor");
            helper.succeed();
        }
        });
    }

    static void lifecycle(GameTestHelper helper) {
        try (var f = new Fixture(helper, 160)) {
            var p = f.player();
            SkillLoadouts.bind(p, 0, SkillDefinition.LUNGING_COMBO.id()); SkillLoadouts.bind(p, 1, SkillDefinition.LUNGING_COMBO.id());
            SkillLoadouts.bind(p, 2, SkillDefinition.RIP_AND_RUSH.id());
            used(p, SkillDefinition.LUNGING_COMBO); ready(p); used(p, SkillDefinition.RIP_AND_RUSH);
            var before = p.getData(RpgData.COMBO_DISPLAY);
            SkillLoadouts.bind(p, 0, SkillDefinition.LUNGING_COMBO.id()); SkillLoadouts.bind(p, -1, null);
            check(p.getData(RpgData.COMBO_DISPLAY).equals(before), "Unchanged/rejected edits preserve combos");
            SkillLoadouts.bind(p, 2, null);
            check(Combos.moving(p) && p.getData(RpgData.COMBO_DISPLAY).rows().size() == 1, "Unrelated combo survives binding edit");
            check(Skills.cooldownRemaining(p, SkillDefinition.RIP_AND_RUSH) == 400, "Binding cancellation commits only affected cooldown");
            SkillLoadouts.bind(p, 0, null);
            check(!Combos.moving(p) && p.getData(RpgData.COMBO_DISPLAY).rows().isEmpty(), "Duplicate remaining binding does not prevent cancellation");
            long cooldown = Skills.cooldownRemaining(p, SkillDefinition.LUNGING_COMBO);
            Combos.clear(p); Combos.clear(p);
            check(Skills.cooldownRemaining(p, SkillDefinition.LUNGING_COMBO) == cooldown, "Cleanup idempotent");
            for (int reason = 0; reason < 4; reason++) {
                var q = f.player(); q.setNoGravity(false);
                used(q, SkillDefinition.LUNGING_COMBO); ready(q); used(q, SkillDefinition.RIP_AND_RUSH);
                switch (reason) {
                    case 0 -> FixtureSkills.learn(q, SkillDefinition.LUNGING_COMBO.id(), false);
                    case 1 -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(q));
                    case 2 -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(q, Level.OVERWORLD, Level.NETHER));
                    case 3 -> PlayerProfiles.switchClass(q, PlayerClass.STALKER);
                }
                check(q.getData(RpgData.COMBO_DISPLAY).rows().isEmpty() && !Combos.moving(q) && !q.isNoGravity(), "Lifecycle removes both sequences and gravity state");
                check(Skills.cooldownRemaining(q, SkillDefinition.LUNGING_COMBO) == 240
                        && Skills.cooldownRemaining(q, SkillDefinition.RIP_AND_RUSH) == 400, "Lifecycle commits both cooldowns once");
            }
            var dead = f.player(); used(dead, SkillDefinition.LUNGING_COMBO);
            dead.hurtServer(helper.getLevel(), dead.damageSources().genericKill(), Float.MAX_VALUE);
            check(!Combos.moving(dead) && Skills.cooldownRemaining(dead, SkillDefinition.LUNGING_COMBO) == 240, "Death commits before clone");
            var replacement = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), dead.getGameProfile(), dead.clientInformation());
            replacement.connection = dead.connection;
            replacement.restoreFrom(dead, false);
            check(Skills.cooldownRemaining(replacement, SkillDefinition.LUNGING_COMBO) == 240
                    && replacement.getData(RpgData.COMBO_DISPLAY).rows().isEmpty(), "Native death-copy keeps cooldown, not progress");
            helper.succeed();
        }
    }

    static void persistence(GameTestHelper helper) {
        try (var f = new Fixture(helper, 175)) {
            var p = f.player(); p.setNoGravity(false); used(p, SkillDefinition.LUNGING_COMBO);
            var reporter = new ProblemReporter.Collector();
            var during = TagValueOutput.createWithContext(reporter, p.registryAccess()); p.saveWithoutId(during);
            check(!during.buildResult().getBooleanOr("NoGravity", false), "Autosave during movement never persists temporary gravity suppression");
            Combos.clear(p);
            var output = TagValueOutput.createWithContext(reporter, p.registryAccess()); p.saveWithoutId(output);
            var loaded = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), p.getGameProfile(), p.clientInformation());
            loaded.connection = p.connection;
            loaded.load(TagValueInput.create(reporter, loaded.registryAccess(), output.buildResult()));
            check(reporter.isEmpty(), "Player data saves cleanly");
            check(Skills.cooldownRemaining(loaded, SkillDefinition.LUNGING_COMBO) == 240, "Cooldown survives native serialization");
            check(loaded.getData(RpgData.COMBO_DISPLAY).rows().isEmpty() && !Combos.moving(loaded), "Progress/movement never serialized");
            var target = f.target(p.position().add(0, 0, 2)); target.getAttribute(Attributes.ARMOR).setBaseValue(20);
            ArmorShred.apply(target, p, 73);
            var mobOut = TagValueOutput.createWithContext(reporter, target.registryAccess()); target.saveWithoutId(mobOut);
            var mob = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            mob.load(TagValueInput.create(reporter, mob.registryAccess(), mobOut.buildResult()));
            check(mob.getEffect(ArmorShred.EFFECT).getDuration() == 73, "Shred retains relative timer when unloaded");
            near(mob.getAttributeValue(Attributes.ARMOR), 16, "Saved native shred restores armor modifier once");
            mob.hurtServer(helper.getLevel(), mob.damageSources().genericKill(), Float.MAX_VALUE);
            check(!mob.hasEffect(ArmorShred.EFFECT), "Target death clears shred");
            helper.succeed();
        }
    }

    static void deadlines(GameTestHelper helper) {
        var f = new Fixture(helper, 190);
        try {
            var p = f.player(); used(p, SkillDefinition.LUNGING_COMBO);
            helper.onEachTick(p::doTick);
            f.at(10, () -> {
                check(!Combos.moving(p) && row(p, 0).stage() == 1, "Native player ticks complete movement");
                var progress = ((Map<?, ?>) get(p.getData(RpgData.SKILL_RUNTIME), "combos")).values().iterator().next();
                ClassResources.set(p, PlayerClass.REAVER, 0); ready(p);
                set(progress, "deadline", ClassResources.now(p) + 1);
                check(key(Skills.activate(p, SkillDefinition.LUNGING_COMBO), "resource"), "Request before boundary is still follow-up");
                set(progress, "deadline", ClassResources.now(p));
                check(key(Skills.activate(p, SkillDefinition.LUNGING_COMBO), "cooldown"), "Exact deadline expires before accepting input");
                check(Skills.cooldownRemaining(p, SkillDefinition.LUNGING_COMBO) == 240, "Timeout commits captured cooldown once");
                check(p.getData(RpgData.COMBO_DISPLAY).rows().isEmpty(), "Timeout removes row");
                ClassResources.set(p, PlayerClass.REAVER, 100); ready(p); used(p, SkillDefinition.RIP_AND_RUSH);
                f.freeze(p, 90);
            });
            f.at(53, () -> {
                check(p.getData(RpgData.COMBO_DISPLAY).rows().isEmpty(), "Real follow-up deadline expires during freeze");
                check(Skills.cooldownRemaining(p, SkillDefinition.RIP_AND_RUSH) > 380, "Freeze did not pause timeout");
                f.close(); helper.succeed();
            });
        } catch (RuntimeException e) { f.close(); throw e; }
    }

    static void interactions(GameTestHelper helper) {
        var f = new Fixture(helper, 205);
        f.forceLanes(40, 28);
        helper.runBeforeTestEnd(() -> {
            f.close();
            helper.fail("Combo interaction fixture chunks did not become entity-ticking before timeout");
        });
        helper.startSequence().thenWaitUntil(f::assertLanesReady).thenExecute(() -> {
        try (f) {
            var p = f.player();
            p.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(2);
            finishRush(f, p, 3);
            near(p.getAttributeValue(Attributes.MOVEMENT_SPEED), .13, "Three consumed enemies grant +30% base");
            check((long) get(p.getData(RpgData.SKILL_RUNTIME), "rushSpeedUntil") == ClassResources.now(p) + 160, "Speed duration scales at finisher");
            p.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(1);
            finishRush(f, p, 1);
            near(p.getAttributeValue(Attributes.MOVEMENT_SPEED), .11, "New weaker reward replaces strength");
            long until = (long) get(p.getData(RpgData.SKILL_RUNTIME), "rushSpeedUntil");
            check(until == ClassResources.now(p) + 80, "New shorter reward replaces duration");
            finishRush(f, p, 0);
            near(p.getAttributeValue(Attributes.MOVEMENT_SPEED), .11, "Empty finisher preserves prior reward");
            check((long) get(p.getData(RpgData.SKILL_RUNTIME), "rushSpeedUntil") == until, "Zero consumption never refreshes duration");
            SkillLoadouts.bind(p, 0, SkillDefinition.RIP_AND_RUSH.id());
            long cooldown = Skills.cooldownRemaining(p, SkillDefinition.RIP_AND_RUSH);
            SkillLoadouts.bind(p, 0, null);
            near(p.getAttributeValue(Attributes.MOVEMENT_SPEED), .1, "Binding edit clears completed combo's speed reward");
            check(Skills.cooldownRemaining(p, SkillDefinition.RIP_AND_RUSH) == cooldown, "Buff cleanup does not restart cooldown");
            finishRush(f, p, 1);
            set(p.getData(RpgData.SKILL_RUNTIME), "rushSpeedUntil", ClassResources.now(p)); steps(p, 1);
            near(p.getAttributeValue(Attributes.MOVEMENT_SPEED), .1, "Speed expiration removes modifier");

            var armor = f.target(p.position().add(1, 0, 0)); armor.getAttribute(Attributes.ARMOR).setBaseValue(20);
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable> immune = event -> {
                if (event.getEntity() == armor && event.getEffectInstance().is(ArmorShred.EFFECT)) {
                    event.setResult(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
                }
            };
            NeoForge.EVENT_BUS.addListener(immune);
            try { ArmorShred.apply(armor, p, 100); check(!armor.hasEffect(ArmorShred.EFFECT), "Native immunity hook rejects shred"); }
            finally { NeoForge.EVENT_BUS.unregister(immune); }

            var reentrant = f.player(); var victim = f.target(reentrant.position().add(0, 0, 2));
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> callback = event -> {
                if (event.getEntity() == victim) {
                    check(key(Skills.activate(reentrant, SkillDefinition.RIP_AND_RUSH), "busy"), "Damage callback cannot advance/spend same stage again");
                }
            };
            NeoForge.EVENT_BUS.addListener(callback);
            try { used(reentrant, SkillDefinition.RIP_AND_RUSH); near(rage(reentrant), 70, "Re-entry spends opening only once"); }
            finally { NeoForge.EVENT_BUS.unregister(callback); }

            var blasted = f.player(); blasted.setNoGravity(false);
            var trapOwner = f.player(); PlayerProfiles.switchClass(trapOwner, PlayerClass.STALKER);
            var trap = new TrapEntity(Skills.TRAP.get(), helper.getLevel());
            invoke(trap, "initialize", new Class<?>[] {ServerPlayer.class, TrapKind.class, int.class}, trapOwner, TrapKind.BLAST, 0);
            trap.setPos(blasted.position().add(1, 0, 0));
            check(helper.getLevel().getEntitiesOfClass(LivingEntity.class, blasted.getBoundingBox()).contains(blasted),
                    "Blast fixture player is available to native entity queries");
            used(blasted, SkillDefinition.LUNGING_COMBO);
            invoke(trap, "trigger", new Class<?>[] {LivingEntity.class}, blasted);
            check(!Combos.moving(blasted) && !blasted.isNoGravity(), "Real Blast Trap effect ends skill movement");
            check(blasted.getDeltaMovement().lengthSqr() > 0 && row(blasted, 0).stage() == 1, "Blast impulse survives and follow-up opens");
            helper.succeed();
        }
        });
    }

    static void prediction(GameTestHelper helper) {
        try (var f = new Fixture(helper, 220)) {
            var dimension = helper.getLevel().dimension().identifier();
            var start = new DashMotion(1, dimension, Vec3.ZERO, new Vec3(0, 0, 4.0 / 3), 3, false, Vec3.ZERO);
            var prediction = new DashPrediction();
            Vec3 position = new Vec3(.3, 0, .2);
            check(prediction.accept(start, position), "Start transition accepted");
            Vec3 step = prediction.delta(position);
            for (int i = 0; i < 3; i++) {
                Vec3 delta = prediction.delta(position);
                check(delta.equals(step), "Starting offset is distributed, without a positional snap");
                position = position.add(delta); prediction.advanced(delta);
            }
            near(position.distanceTo(new Vec3(0, 0, 4)), 0, "Prediction reaches approved endpoint in three steps");
            check(prediction.ownsTravel() && prediction.delta(position).equals(Vec3.ZERO), "Hold at endpoint until authoritative stop");
            var stop = new DashMotion(2, dimension, new Vec3(0, 0, 4), Vec3.ZERO, 0, false, Vec3.ZERO);
            position = position.add(0, 0, -.5);
            check(prediction.accept(stop, position), "Stop transition accepted");
            for (int i = 0; i < 2; i++) {
                Vec3 delta = prediction.delta(position); position = position.add(delta); prediction.advanced(delta);
            }
            check(prediction.readyToAcknowledge() && prediction.acknowledge() == 2 && !prediction.ownsTravel(), "Smooth stop convergence releases local travel");
            near(position.distanceTo(stop.position()), 0, "Stop endpoint converged");
            check(!prediction.accept(start, position) && !prediction.accept(stop, position), "Stale and duplicate transitions ignored");
            prediction.accept(new DashMotion(3, dimension, position.add(0, 0, -1), Vec3.ZERO, 0, false, new Vec3(.2, .3, 0)), position);
            check(prediction.readyToAcknowledge(), "Knockback interruption returns client control immediately");
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                DashMotion.STREAM_CODEC.encode(buffer, start);
                check(DashMotion.STREAM_CODEC.decode(buffer).equals(start), "Trajectory codec preserves 3D motion and sequence");
                var ack = new SkillNetworking.DashFinished(2, position);
                SkillNetworking.DashFinished.STREAM_CODEC.encode(buffer, ack);
                check(SkillNetworking.DashFinished.STREAM_CODEC.decode(buffer).equals(ack), "Acknowledgement codec round trip");
            } finally { buffer.release(); }

            var p = f.player(); used(p, SkillDefinition.LUNGING_COMBO);
            int before = (int) get(p.connection, "awaitingTeleport");
            Combos.acknowledgeMovement(p, 1, new Vec3(1000, 1000, 1000));
            check(Combos.moving(p), "Cannot acknowledge an executing dash");
            steps(p, 3);
            check((int) get(p.connection, "awaitingTeleport") == before, "Normal travel and completion send no teleport corrections");
            Vec3 endpoint = p.position();
            long sequence = (long) get(p.getData(RpgData.SKILL_RUNTIME), "awaitingDashSequence");
            p.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(endpoint.add(0, 0, -3), 0, 0, false, false));
            check(p.position().equals(endpoint), "Late predicted packets cannot rewind completed dash");
            Combos.acknowledgeMovement(p, sequence - 1, endpoint);
            check(Combos.blocksMove(p), "Stale acknowledgement cannot release movement ownership");
            Combos.acknowledgeMovement(p, sequence, endpoint);
            check(!Combos.blocksMove(p) && (int) get(p.connection, "awaitingTeleport") == before, "Correct acknowledgement resumes ordinary movement without teleport");
            p.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(endpoint.add(0, 0, .1), 0, 0, false, false));
            near(p.getZ(), endpoint.z + .1, "Native walking resumes from final server anchor");

            var mismatch = f.player(); used(mismatch, SkillDefinition.LUNGING_COMBO); steps(mismatch, 3);
            Vec3 authority = mismatch.position();
            long mismatchSequence = (long) get(mismatch.getData(RpgData.SKILL_RUNTIME), "awaitingDashSequence");
            int oldId = (int) get(mismatch.connection, "awaitingTeleport");
            mismatch.setDeltaMovement(.2, .3, 0);
            Combos.acknowledgeMovement(mismatch, mismatchSequence, authority.add(100, 0, 0));
            check(mismatch.position().equals(authority) && !Combos.blocksMove(mismatch), "Bad client endpoint never controls server position");
            check((int) get(mismatch.connection, "awaitingTeleport") == oldId + 1, "Disagreement uses one exceptional native correction");
            check(mismatch.getDeltaMovement().equals(new Vec3(.2, .3, 0)), "Exceptional correction preserves incoming impulse");
            var timeout = f.player(); used(timeout, SkillDefinition.LUNGING_COMBO); steps(timeout, 3);
            set(timeout.getData(RpgData.SKILL_RUNTIME), "dashAcknowledgementUntil", ClassResources.now(timeout));
            steps(timeout, 1);
            check(!Combos.blocksMove(timeout), "Missing acknowledgement cannot permanently lock movement");
            helper.succeed();
        }
    }

    /** Connection.tickPlayer restores firstGood after doTick; exercising doTick alone misses that restore. */
    static void connectionMovement(GameTestHelper helper) {
        try (var f = new Fixture(helper, 235)) {
            for (var skill : List.of(SkillDefinition.LUNGING_COMBO, SkillDefinition.RIP_AND_RUSH)) {
                var p = f.player();
                if (skill == SkillDefinition.RIP_AND_RUSH) { used(p, skill); ready(p); }
                Vec3 start = p.position();
                int teleports = (int) get(p.connection, "awaitingTeleport");
                used(p, skill);
                int ticks = skill == SkillDefinition.LUNGING_COMBO ? 3 : 4;
                double distance = skill == SkillDefinition.LUNGING_COMBO ? 4 : 6;
                for (int i = 0; i < ticks; i++) {
                    var movement = get(p.getData(RpgData.SKILL_RUNTIME), "movement");
                    set(movement, "lastTick", Long.MIN_VALUE);
                    p.connection.tick();
                    near(p.getZ() - start.z, distance * (i + 1) / ticks,
                            "Authoritative dash step survives complete native connection tick: " + skill.id());
                    near((double) get(p.connection, "firstGoodZ"), p.getZ(), "Native restore anchor follows authoritative step");
                }
                check(!Combos.moving(p), "Native connection ticks finish movement");
                check((int) get(p.connection, "awaitingTeleport") == teleports, "No per-step teleport updates");
                Vec3 endpoint = p.position();
                long sequence = (long) get(p.getData(RpgData.SKILL_RUNTIME), "awaitingDashSequence");
                p.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(start, 0, 0, false, false));
                check(p.position().equals(endpoint), "Late starting-position packet cannot rewind dash");
                Combos.acknowledgeMovement(p, sequence, endpoint);
                p.connection.tick();
                near(p.getZ(), endpoint.z, "Completed endpoint survives following connection tick");
                p.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(endpoint.add(0, 0, .1), 0, 0, false, false));
                near(p.getZ(), endpoint.z + .1, "Native walking starts from retained dash endpoint");
            }
            helper.succeed();
        }
    }

    static void sliding(GameTestHelper helper) {
        var f = new Fixture(helper, 250);
        f.forceLanes();
        helper.runBeforeTestEnd(() -> {
            f.close();
            helper.fail("Sliding fixture chunks did not become entity-ticking before timeout");
        });
        helper.startSequence().thenWaitUntil(f::assertLanesReady).thenExecute(() -> {
        try (f) {
            for (var skill : List.of(SkillDefinition.LUNGING_COMBO, SkillDefinition.RIP_AND_RUSH)) {
                var p = f.player(); p.setNoGravity(false); f.floor(p); p.setXRot(45);
                if (skill == SkillDefinition.RIP_AND_RUSH) { used(p, skill); ready(p); }
                Vec3 start = p.position();
                used(p, skill);
                int ticks = skill == SkillDefinition.LUNGING_COMBO ? 3 : 4;
                double distance = skill == SkillDefinition.LUNGING_COMBO ? 4 : 6;
                for (int i = 0; i < ticks; i++) {
                    set(get(p.getData(RpgData.SKILL_RUNTIME), "movement"), "lastTick", Long.MIN_VALUE);
                    p.connection.tick();
                    near(p.getZ() - start.z, distance * (i + 1) / ticks, "Downward ground aim preserves dash speed and full distance");
                    near(p.getY(), start.y, "Slides along floor without entering it");
                    if (i < ticks - 1) { check(Combos.moving(p), "Floor contact does not prematurely complete movement"); }
                }
                check(!Combos.moving(p) && !p.isNoGravity(), "Faster ground dash ends on time and restores gravity");
            }
            var corner = f.player(); f.floor(corner); f.wall(corner);
            corner.setYRot(-45); corner.setXRot(45);
            Vec3 cornerStart = corner.position();
            used(corner, SkillDefinition.LUNGING_COMBO); steps(corner, 3);
            check(corner.getX() - cornerStart.x < .21 && corner.getX() > cornerStart.x, "Wall blocks its normal component");
            near(corner.getZ() - cornerStart.z, 4 / Math.sqrt(2), "Wall contact preserves only captured tangential speed");
            near(corner.getY(), cornerStart.y, "Corner slide stays outside floor");

            var ceiling = f.player();
            for (int x = -1; x <= 1; x++) { for (int z = -1; z <= 7; z++) {
                f.block(BlockPos.containing(ceiling.position().add(x, 2, z)));
            } }
            ceiling.setXRot(-30); Vec3 ceilingStart = ceiling.position();
            used(ceiling, SkillDefinition.LUNGING_COMBO); steps(ceiling, 3);
            check(ceiling.getY() - ceilingStart.y < .21 && ceiling.getZ() - ceilingStart.z > 3.8, "Glancing ceiling contact slides without clipping");

            var rush = f.player(); f.floor(rush); f.wall(rush);
            used(rush, SkillDefinition.RIP_AND_RUSH); ready(rush);
            rush.setYRot(-45); rush.setXRot(30);
            Vec3 rushStart = rush.position();
            var contact = f.target(rush.position().add(.2, 0, 3.5));
            var beside = f.target(rush.position().add(-1.5, 0, 2));
            check(helper.getLevel().getEntitiesOfClass(LivingEntity.class, contact.getBoundingBox()).contains(contact),
                    "Sliding fixture contact target is available to native entity queries");
            used(rush, SkillDefinition.RIP_AND_RUSH); steps(rush, 4);
            near(rush.getZ() - rushStart.z, 6 / Math.sqrt(2), "Sliding Rush retains original tangential travel");
            near(contact.getHealth(), 195, "Sliding Rush contacts enemy once along its actual segments; player=" + rush.position()
                    + ", enemy=" + contact.position() + ", visible=" + rush.hasLineOfSight(contact));
            check(contact.hasEffect(ArmorShred.EFFECT), "Sliding contact applies normal shred");
            near(beside.getHealth(), 200, "Enemies outside the sliding path are not hit");

            // Use the same collision result to advance the owner's actual prediction model.
            var predicted = f.player(); f.floor(predicted); predicted.setXRot(60);
            Vec3 start = predicted.position();
            var motion = new DashMotion(1, helper.getLevel().dimension().identifier(), start,
                    predicted.getLookAngle().normalize().scale(1.5), 4, true, Vec3.ZERO);
            var prediction = new DashPrediction(); prediction.accept(motion, start);
            for (int i = 0; i < 4; i++) {
                var result = DashMovement.move(predicted, prediction.delta(predicted.position()));
                prediction.advanced(result.nextStep());
                near(predicted.getZ() - start.z, (i + 1) * 1.5, "Owner prediction preserves floor-slide rate");
            }
            near(predicted.getY(), start.y, "Predicted slide obeys native floor collision");
            check(prediction.ownsTravel() && prediction.delta(predicted.position()).equals(Vec3.ZERO), "Predicted slide waits for authoritative stop at endpoint");

            var predictedWall = f.player(); f.floor(predictedWall); f.wall(predictedWall);
            predictedWall.setYRot(-89.9f); predictedWall.setXRot(45);
            Vec3 wallStart = predictedWall.position();
            prediction = new DashPrediction();
            prediction.accept(new DashMotion(1, helper.getLevel().dimension().identifier(), wallStart,
                    predictedWall.getLookAngle().normalize().scale(1.5), 4, true, Vec3.ZERO), wallStart);
            for (int i = 0; i < 4; i++) {
                var result = DashMovement.move(predictedWall, prediction.delta(predictedWall.position()));
                prediction.advanced(result.nextStep());
            }
            check(Math.abs(predictedWall.getZ() - wallStart.z) < .02, "Owner prediction cannot amplify blocked forward motion into sideways movement");
            // Append extra lanes after the existing contact fixture to retain its chunk placement.
            for (var skill : List.of(SkillDefinition.LUNGING_COMBO, SkillDefinition.RIP_AND_RUSH)) {
                for (float yaw : new float[] {-89.9f, -90.1f}) {
                    var blocked = f.player(); f.floor(blocked); f.wall(blocked);
                    blocked.setYRot(yaw); blocked.setXRot(45);
                    if (skill == SkillDefinition.RIP_AND_RUSH) { used(blocked, skill); ready(blocked); }
                    Vec3 blockedStart = blocked.position();
                    used(blocked, skill); steps(blocked, skill == SkillDefinition.LUNGING_COMBO ? 3 : 4);
                    check(blocked.getX() - blockedStart.x < .21, "Near-head-on dash obeys wall collision");
                    check(Math.abs(blocked.getZ() - blockedStart.z) < .02, "Blocked forward dash cannot amplify tiny left/right aim into a strafe");
                    near(blocked.getY(), blockedStart.y, "Blocked dash stays on the floor");
                }
            }
            helper.succeed();
        }
        });
    }

    private static void finishRush(Fixture f, ServerPlayer p, int count) {
        p.setData(RpgData.SKILLS, p.getData(RpgData.SKILLS).withCooldown(SkillDefinition.RIP_AND_RUSH.id(), 0));
        ClassResources.set(p, PlayerClass.REAVER, 100); ready(p); used(p, SkillDefinition.RIP_AND_RUSH);
        ready(p); used(p, SkillDefinition.RIP_AND_RUSH); steps(p, 4);
        for (int i = 0; i < count; i++) {
            var target = f.target(p.position().add(i - 1, 0, 1)); Bleeding.apply(target, p, 1, 100);
        }
        ready(p); used(p, SkillDefinition.RIP_AND_RUSH);
    }

    private static void invoke(Object object, String name, Class<?>[] types, Object... args) {
        try { var method = object.getClass().getDeclaredMethod(name, types); method.setAccessible(true); method.invoke(object, args); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private static double rage(ServerPlayer p) { return ClassResources.current(p, PlayerClass.REAVER); }
    private static ComboDisplay.Row row(ServerPlayer p, int kind) { return p.getData(RpgData.COMBO_DISPLAY).rows().stream().filter(r -> r.combo() == kind).findFirst().orElseThrow(); }
    private static void ready(ServerPlayer p) { set(p.getData(RpgData.SKILL_RUNTIME), "nextActivation", 0L); }
    private static void used(ServerPlayer p, SkillDefinition skill) { var result = Skills.activate(p, skill); check(key(result, "used"), "Expected accepted " + skill.id() + ": " + result.getString()); }
    private static boolean key(Component c, String suffix) { return c.getContents() instanceof TranslatableContents text && text.getKey().equals("commands.covenant.skill." + suffix); }
    private static Object get(Object object, String field) {
        try { var f = object.getClass().getDeclaredField(field); f.setAccessible(true); return f.get(object); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void set(Object object, String field, Object value) {
        try { var f = object.getClass().getDeclaredField(field); f.setAccessible(true); f.set(object, value); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void steps(ServerPlayer p, int count) {
        for (int i = 0; i < count; i++) {
            var movement = get(p.getData(RpgData.SKILL_RUNTIME), "movement");
            if (movement != null) { set(movement, "lastTick", Long.MIN_VALUE); }
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(p));
        }
    }
    private static void near(double actual, double expected, String message) { check(Math.abs(actual - expected) < .001, message + ": expected " + expected + ", got " + actual); }
    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }

    private static final class Fixture implements AutoCloseable {
        // TicketStorage compares type identity: each fixture owns independent, unsaved
        // entity-ticking tickets even when its lanes overlap another fixture's lanes.
        final TicketType laneTicket = new TicketType(TicketType.NO_TIMEOUT,
                TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
        final GameTestHelper helper;
        final List<ServerPlayer> players = new ArrayList<>();
        final List<LivingEntity> mobs = new ArrayList<>();
        final List<BlockPos> blocks = new ArrayList<>();
        final java.util.Set<net.minecraft.world.level.ChunkPos> forced = new java.util.HashSet<>();
        final Vec3 origin;
        int laneWidth = 160;
        int laneLength = 12;
        Fixture(GameTestHelper helper, int height) {
            this.helper = helper;
            var pos = helper.absolutePos(new BlockPos(2, height, 2));
            origin = new Vec3(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        }
        void forceLanes() {
            forceLanes(160, 12);
        }
        void forceLanes(int width, int length) {
            laneWidth = width;
            laneLength = length;
            // The tiny template does not cover the contact/prediction lanes appended to its east.
            for (int x = net.minecraft.util.Mth.floor(origin.x - 4) >> 4; x <= net.minecraft.util.Mth.floor(origin.x + laneWidth) >> 4; x++) {
                for (int z = net.minecraft.util.Mth.floor(origin.z - 4) >> 4; z <= net.minecraft.util.Mth.floor(origin.z + laneLength) >> 4; z++) {
                    var chunk = new net.minecraft.world.level.ChunkPos(x, z);
                    if (forced.add(chunk)) {
                        helper.getLevel().getChunkSource().addTicketWithRadius(laneTicket, chunk, 2);
                    }
                }
            }
        }
        void assertLanesReady() {
            // Block loading and a fixed delay do not establish entity-section visibility.
            for (int x = net.minecraft.util.Mth.floor(origin.x - 4) >> 4; x <= net.minecraft.util.Mth.floor(origin.x + laneWidth) >> 4; x++) {
                for (int z = net.minecraft.util.Mth.floor(origin.z - 4) >> 4; z <= net.minecraft.util.Mth.floor(origin.z + laneLength) >> 4; z++) {
                    helper.assertTrue(helper.getLevel().areEntitiesActuallyLoadedAndTicking(new net.minecraft.world.level.ChunkPos(x, z)),
                            "Combo fixture chunks must be loaded and entity-ticking");
                }
            }
        }
        ServerPlayer player() {
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "combo-test-player"), false);
            var p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new Connection(PacketFlow.SERVERBOUND); new io.netty.channel.embedded.EmbeddedChannel(connection);
            helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
            p.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket((int) get(p.connection, "awaitingTeleport")));
            p.setPos(origin.add(players.size() * 12, 0, 0)); players.add(p);
            p.connection.resetPosition();
            p.setNoGravity(true); p.setGameMode(GameType.SURVIVAL); p.connection.markClientLoaded();
            p.setYRot(0); p.setXRot(0); p.getAttribute(RpgStats.DAMAGE).setBaseValue(10);
            PlayerProfiles.selectClass(p, PlayerClass.REAVER);
            for (var skill : List.of(SkillDefinition.CLEAVE, SkillDefinition.REND, SkillDefinition.LUNGING_COMBO, SkillDefinition.RIP_AND_RUSH)) { FixtureSkills.learn(p, skill.id(), true); }
            ClassResources.set(p, PlayerClass.REAVER, 100);
            return p;
        }
        LivingEntity target(Vec3 position) {
            var target = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            target.setPos(position); target.setNoAi(true); target.setNoGravity(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0); helper.getLevel().addFreshEntity(target); mobs.add(target); return target;
        }
        void freeze(ServerPlayer target, int ticks) {
            var source = player(); source.setPos(target.position().add(1, 0, 0));
            PlayerProfiles.switchClass(source, PlayerClass.WEAVER);
            FixtureSkills.learn(source, SkillDefinition.ICY_VEINS.id(), true);
            FixtureSkills.learn(source, SkillDefinition.FREEZING_AURA.id(), true);
            SkillLoadouts.bind(source, 0, SkillDefinition.FREEZING_AURA.id());
            source.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(ticks / 30.0);
            used(source, SkillDefinition.FREEZING_AURA);
            check(Auras.frozen(target), "Native freeze aura fixture applies");
        }
        void block(BlockPos position) { helper.getLevel().setBlockAndUpdate(position, Blocks.STONE.defaultBlockState()); blocks.add(position); }
        void floor(ServerPlayer p) {
            for (int x = -2; x <= 2; x++) { for (int z = -2; z <= 8; z++) {
                block(BlockPos.containing(p.position().add(x, -1, z)));
            } }
        }
        void wall(ServerPlayer p) {
            for (int y = 0; y <= 2; y++) { for (int z = -2; z <= 8; z++) {
                block(BlockPos.containing(p.position().add(1, y, z)));
            } }
        }
        void at(int tick, Runnable action) { helper.runAtTickTime(tick, () -> { try { action.run(); } catch (RuntimeException e) { close(); throw e; } }); }
        @Override public void close() {
            mobs.forEach(Entity::discard); mobs.clear();
            for (var player : players) {
                Combos.clear(player);
                if (helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player) { helper.getLevel().getServer().getPlayerList().remove(player); }
            }
            players.clear(); blocks.forEach(p -> helper.getLevel().setBlockAndUpdate(p, Blocks.AIR.defaultBlockState())); blocks.clear();
            forced.forEach(chunk -> helper.getLevel().getChunkSource().removeTicketWithRadius(laneTicket, chunk, 2));
            forced.clear();
        }
    }
}
