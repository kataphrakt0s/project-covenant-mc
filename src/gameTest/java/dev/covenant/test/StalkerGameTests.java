package dev.covenant.test;

import com.mojang.authlib.GameProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.*;
import dev.covenant.rpg.stats.RpgStats;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Production activations and native arrow impacts with Survival-accurate embedded player connections. */
final class StalkerGameTests {
    static void activation(GameTestHelper helper) {
        try (var f = new Fixture(helper, 195)) {
            var p = f.player();
            for (var skill : List.of(SkillDefinition.DISENGAGE, SkillDefinition.ARCANE_ARROW, SkillDefinition.MULTISHOT)) {
                var node = SkillTree.find(skill.id()).orElseThrow();
                check(node.availableMaximum() == 1 && node.plannedMaximum() == 8, "Rank 1 purchase limit with planned Rank 8");
            }
            check(key(Skills.activate(p, SkillDefinition.MULTISHOT), "bow"), "Missing bow rejected");
            p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            check(key(Skills.activate(p, SkillDefinition.ARCANE_ARROW), "arrow"), "Missing ammo rejected");
            near(f.focus(p), 100, "Equipment failures do not spend");
            ClassResources.set(p, PlayerClass.STALKER, 19);
            check(key(Skills.activate(p, SkillDefinition.DISENGAGE), "resource"), "Insufficient Focus rejected");
            Rooted.apply(p, 60); ClassResources.set(p, PlayerClass.STALKER, 100);
            check(key(Skills.activate(p, SkillDefinition.DISENGAGE), "rooted"), "Root prevents voluntary leap");
            p.removeEffect(Rooted.EFFECT);
            p.getAttribute(RpgStats.RESOURCE_COST).setBaseValue(.5);
            p.getAttribute(RpgStats.COOLDOWN).setBaseValue(.5);
            used(p, SkillDefinition.DISENGAGE);
            near(f.focus(p), 90, "Cost multiplier");
            near(Skills.cooldownRemaining(p, SkillDefinition.DISENGAGE), 60, "Cooldown multiplier starts immediately");
            check(key(Skills.activate(p, SkillDefinition.DISENGAGE), "cooldown"), "Saved cooldown enforced");
            helper.succeed();
        }
    }

    static void disengage(GameTestHelper helper) {
        try (var f = new Fixture(helper, 205)) {
            for (float pitch : new float[] {-90, 0, 90}) {
                var p = f.player(); p.setXRot(pitch); p.setNoGravity(false);
                p.fallDistance = 100; p.setDeltaMovement(3, -10, 4);
                var start = p.position(); used(p, SkillDefinition.DISENGAGE);
                check(p.isNoGravity() && p.fallDistance == 0 && p.getDeltaMovement().equals(Vec3.ZERO), "Airborne activation replaces momentum and breaks fall");
                p.setYRot(90); p.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(3);
                p.move(MoverType.PLAYER, new Vec3(8, 0, 0));
                near(p.getX(), start.x, "No manual steering");
                steps(p, 3); near(p.getY() - start.y, .5, "Half-block peak at three ticks");
                steps(p, 3); near(p.getZ() - start.z, -4, "Four backward blocks regardless of pitch or later aim");
                near(p.getY(), start.y, "Arc returns to original height");
                near(p.getX(), start.x, "Captured horizontal direction");
                check(!Combos.moving(p) && !p.isNoGravity() && p.fallDistance == 0, "Restores gravity and clears fall distance");
                near(f.focus(p), 80, "Leap costs twenty Focus");
                check(p.getData(RpgData.COMBO_DISPLAY).rows().isEmpty(), "Standalone leap creates no combo HUD row");
                Combos.acknowledgeMovement(p, (long) get(p.getData(RpgData.SKILL_RUNTIME), "awaitingDashSequence"), p.position());
                p.move(MoverType.SELF, new Vec3(0, -1, 0)); p.doCheckFallDamage(0, -1, 0, false);
                check(p.fallDistance > 0, "Normal falling resumes afterward");
            }
            helper.succeed();
        }
    }

    static void movementLifecycle(GameTestHelper helper) {
        try (var f = new Fixture(helper, 215)) {
            var p = f.player(); p.setNoGravity(false); used(p, SkillDefinition.DISENGAGE);
            var saved = save(p);
            check(!saved.getBooleanOr("NoGravity", false), "Autosave does not persist temporary gravity suppression");
            f.arm(p, Items.ARROW); ready(p);
            f.fire(p, SkillDefinition.ARCANE_ARROW);
            check(Combos.moving(p), "Bow skill remains usable during leap");
            p.invulnerableTime = 0; float health = p.getHealth();
            p.hurtServer(helper.getLevel(), p.damageSources().generic(), 1);
            check(p.getHealth() < health, "Leap grants no invulnerability");
            Rooted.apply(p, 60);
            check(!Combos.moving(p) && !p.isNoGravity(), "Root interrupts and restores gravity");
            near(Skills.cooldownRemaining(p, SkillDefinition.DISENGAGE), 120, "Interruption does not refund cooldown");
            var knocked = f.player(); used(knocked, SkillDefinition.DISENGAGE); knocked.knockback(1, 0, 1);
            check(!Combos.moving(knocked) && knocked.getDeltaMovement().lengthSqr() > 0, "Knockback interrupts while retaining impulse");
            var unlearned = f.player(); used(unlearned, SkillDefinition.DISENGAGE);
            FixtureSkills.learn(unlearned, SkillDefinition.DISENGAGE.id(), false);
            check(!Combos.moving(unlearned), "Unlearning cancels movement");
            for (var reason : SkillLifecycle.Reason.values()) {
                var other = f.player(); used(other, SkillDefinition.DISENGAGE);
                SkillLifecycle.cancelPlayer(other, reason);
                check(!Combos.moving(other), "Lifecycle cleanup: " + reason);
                near(Skills.cooldownRemaining(other, SkillDefinition.DISENGAGE), 120, "Lifecycle retains committed cooldown");
            }
            helper.succeed();
        }
    }

    static void collisionAndPrediction(GameTestHelper helper) {
        try (var f = new Fixture(helper, 225)) {
            var p = f.player(); var start = p.position();
            for (int y = 0; y <= 3; y++) { f.block(BlockPos.containing(start.add(0, y, -1))); }
            used(p, SkillDefinition.DISENGAGE); steps(p, 6);
            check(start.z - p.getZ() < .3, "Backward wall clips travel");
            near(p.getX(), start.x, "Wall does not cause sideways speed");
            var ceiling = f.player(); start = ceiling.position(); f.block(BlockPos.containing(start.add(0, 2, 0)));
            used(ceiling, SkillDefinition.DISENGAGE); steps(ceiling, 6);
            near(ceiling.getZ() - start.z, -4, "Ceiling contact does not accelerate horizontal travel");
            check(ceiling.getY() - start.y < .3, "Low ceiling clips hop");
            var server = f.player(); var predicted = f.player(); predicted.setPos(server.position());
            start = server.position(); used(server, SkillDefinition.DISENGAGE);
            var movement = get(server.getData(RpgData.SKILL_RUNTIME), "movement");
            var motion = new DashMotion(1, helper.getLevel().dimension().identifier(), start,
                    (Vec3) get(movement, "step"), 6, true, Vec3.ZERO, (Vec3) get(movement, "acceleration"));
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            DashMotion.STREAM_CODEC.encode(buffer, motion);
            var decoded = DashMotion.STREAM_CODEC.decode(buffer); buffer.release();
            check(decoded.equals(motion), "Arcing trajectory packet roundtrip");
            var prediction = new DashPrediction(); prediction.accept(decoded, start);
            for (int i = 0; i < 6; i++) {
                var result = DashMovement.move(predicted, prediction.delta(predicted.position()), null, !prediction.arcing());
                prediction.advanced(result.nextStep()); steps(server, 1);
                check(predicted.position().distanceToSqr(server.position()) < 1e-8, "Owner prediction matches authoritative arc at tick " + i);
            }
            helper.succeed();
        }
    }

    static void arcane(GameTestHelper helper) {
        try (var f = new Fixture(helper, 235)) {
            var p = f.player(); f.arm(p, Items.ARROW); p.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(2);
            p.getAttribute(RpgStats.DAMAGE).setBaseValue(1000);
            var arrow = f.fire(p, SkillDefinition.ARCANE_ARROW).getFirst();
            p.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(.5);
            var target = f.target(p.position().add(0, 0, 3)); target.getAttribute(Attributes.ARMOR).setBaseValue(30);
            impact(arrow, target);
            near(target.getHealth(), 196, "Exactly four magic damage, bypassing armor and unrelated melee stats");
            check(target.hasEffect(Vulnerability.EFFECT) && target.getEffect(Vulnerability.EFFECT).getDuration() == 160, "Duration snapshots at launch");
            target.invulnerableTime = 0; target.hurtServer(helper.getLevel(), target.damageSources().magic(), 4);
            near(target.getHealth(), 191.4, "Vulnerability increases a subsequent hit by fifteen percent");
            Vulnerability.apply(target, p, 20); check(target.getEffect(Vulnerability.EFFECT).getDuration() == 160, "Shorter refresh cannot shorten duration");
            Vulnerability.apply(target, p, 180); check(target.getEffect(Vulnerability.EFFECT).getDuration() == 180, "Longer refresh replaces duration without stacking strength");
            near(f.focus(p), 70, "Arcane Arrow cost"); near(Skills.cooldownRemaining(p, SkillDefinition.ARCANE_ARROW), 160, "Arcane cooldown");
            var absorbed = f.target(p.position().add(2, 0, 3));
            absorbed.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20); absorbed.setAbsorptionAmount(10);
            impact(f.magicArrow(p), absorbed);
            near(absorbed.getAbsorptionAmount(), 6, "Absorption receives magic damage"); check(absorbed.hasEffect(Vulnerability.EFFECT), "Absorption damage qualifies");
            var immune = f.target(p.position().add(-2, 0, 3)); immune.setInvulnerable(true);
            impact(f.magicArrow(p), immune);
            check(!immune.hasEffect(Vulnerability.EFFECT), "Fully prevented hit grants no debuff");
            var second = f.player(); PlayerProfiles.switchClass(second, PlayerClass.WEAVER);
            second.setPos(target.position().add(1, 0, 0)); FixtureSkills.learn(second, SkillDefinition.SHOCK_AURA.id(), true);
            SkillLoadouts.bind(second, 0, SkillDefinition.SHOCK_AURA.id()); Auras.reconcile(second); Auras.refresh(second);
            check(Auras.passive(target, Auras.Kind.SHOCK), "Shock fixture covers target");
            target.invulnerableTime = 0; float before = target.getHealth();
            target.hurtServer(helper.getLevel(), target.damageSources().magic(), 4);
            near(before - target.getHealth(), 5, "Shock and vulnerability add to twenty-five percent");
            helper.succeed();
        }
    }

    static void ammunition(GameTestHelper helper) {
        try (var f = new Fixture(helper, 245)) {
            var p = f.player(); f.arm(p, Items.SPECTRAL_ARROW);
            var target = f.target(p.position().add(0, 0, 3));
            impact(f.fire(p, SkillDefinition.ARCANE_ARROW).getFirst(), target);
            check(target.hasEffect(MobEffects.GLOWING) && target.hasEffect(Vulnerability.EFFECT), "Spectral glow and vulnerability coexist");
            var tipped = new ItemStack(Items.TIPPED_ARROW, 20);
            tipped.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.POISON));
            p.setItemSlot(EquipmentSlot.OFFHAND, tipped); reset(p, SkillDefinition.ARCANE_ARROW);
            var poisoned = f.target(p.position().add(2, 0, 3));
            impact(f.fire(p, SkillDefinition.ARCANE_ARROW).getFirst(), poisoned);
            check(poisoned.hasEffect(MobEffects.POISON) && poisoned.hasEffect(Vulnerability.EFFECT), "Tipped poison and vulnerability coexist");
            var multi = f.player(); f.arm(multi, Items.SPECTRAL_ARROW);
            var arrows = f.fire(multi, SkillDefinition.MULTISHOT);
            var one = f.target(multi.position().add(0, 0, 3)); impact(arrows.get(0), one);
            check(one.hasEffect(MobEffects.GLOWING), "Fan preserves native ammunition effects");
            var other = f.target(multi.position().add(2, 0, 3)); impact(arrows.get(1), other);
            check(other.hasEffect(MobEffects.GLOWING), "Different target may receive its own native effect");
            var infinite = f.player(); f.arm(infinite, Items.ARROW);
            var infinity = infinite.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY);
            infinite.getMainHandItem().enchant(infinity, 1);
            var infiniteFan = f.fire(infinite, SkillDefinition.MULTISHOT);
            check(infinite.getOffhandItem().getCount() == 20, "Infinity preserves ordinary ammunition for entire fan");
            check(infiniteFan.stream().noneMatch(a -> a.pickup == AbstractArrow.Pickup.ALLOWED), "Infinity fan cannot create recoverable ammunition");
            ready(infinite); f.fire(infinite, SkillDefinition.ARCANE_ARROW);
            check(infinite.getOffhandItem().getCount() == 20, "Arcane Arrow respects Infinity");
            infinite.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SPECTRAL_ARROW, 20));
            reset(infinite, SkillDefinition.MULTISHOT); f.fire(infinite, SkillDefinition.MULTISHOT);
            check(infinite.getOffhandItem().getCount() == 19, "Infinity does not grant a special-ammunition exemption");
            helper.succeed();
        }
    }

    static void multishot(GameTestHelper helper) {
        try (var f = new Fixture(helper, 255)) {
            var p = f.player(); f.arm(p, Items.ARROW);
            var arrows = f.fire(p, SkillDefinition.MULTISHOT);
            check(arrows.size() == 5, "Five native arrows"); near(f.focus(p), 70, "Fan cost");
            check(p.getOffhandItem().getCount() == 19, "One ammunition item per fan");
            check(p.getMainHandItem().getDamageValue() == 1, "One bow durability use per fan");
            check(arrows.stream().filter(a -> a.pickup == AbstractArrow.Pickup.ALLOWED).count() == 1, "Only central arrow is recoverable");
            var angles = arrows.stream().map(a -> Math.toDegrees(Math.atan2(-a.getDeltaMovement().x, a.getDeltaMovement().z))).sorted().toList();
            check(Math.abs(angles.getFirst() + 15) < .01 && Math.abs(angles.getLast() - 15) < .01, "Thirty-degree fan within native rotation rounding");
            for (var arrow : arrows) { near(arrow.getDeltaMovement().length(), 3, "Full-speed launch"); }
            var target = f.target(p.position().add(0, 0, 3));
            impact(arrows.get(0), target); near(target.getHealth(), 195, "Native speed-based arrow damage with ceiling rounding");
            for (int i = 1; i < 5; i++) { target.invulnerableTime = 0; impact(arrows.get(i), target); }
            near(target.getHealth(), 195, "No shotgunning even without native hurt cooldown");
            reset(p, SkillDefinition.MULTISHOT); var next = f.fire(p, SkillDefinition.MULTISHOT);
            target.invulnerableTime = 0; impact(next.getFirst(), target); near(target.getHealth(), 190, "Independent fan gets another attempt");
            var immune = f.target(p.position().add(2, 0, 3)); immune.setInvulnerable(true);
            impact(next.get(1), immune); immune.setInvulnerable(false); immune.invulnerableTime = 0;
            impact(next.get(2), immune); near(immune.getHealth(), 200, "Prevented first attempt cannot be retried by sibling");
            helper.succeed();
        }
    }

    static void persistence(GameTestHelper helper) {
        try (var f = new Fixture(helper, 265)) {
            var p = f.player(); f.arm(p, Items.ARROW);
            var arrow = f.fire(p, SkillDefinition.ARCANE_ARROW).getFirst();
            var loaded = f.copyArrow(arrow); loaded.removeData(RpgData.SKILL_SHOT);
            var target = f.target(p.position().add(0, 0, 3)); impact(loaded, target);
            near(target.getHealth(), 196, "Reloaded magic payload independent of cosmetic trail");
            var targetCopy = f.target(p.position().add(2, 0, 3));
            load(targetCopy, save(target));
            check(targetCopy.hasEffect(Vulnerability.EFFECT), "Native save/load retains target debuff");
            SkillLifecycle.cancelPlayer(p, SkillLifecycle.Reason.LOGOUT);
            check(target.hasEffect(Vulnerability.EFFECT), "Applied debuff outlives owner departure");
            target.removeEffect(Vulnerability.EFFECT); check(!target.hasEffect(Vulnerability.EFFECT), "Native cleansing removes debuff");
            ready(p); var fan = f.fire(p, SkillDefinition.MULTISHOT);
            target.invulnerableTime = 0; impact(fan.get(0), target);
            load(targetCopy, save(target)); targetCopy.invulnerableTime = 0;
            float before = targetCopy.getHealth(); impact(f.copyArrow(fan.get(1)), targetCopy);
            near(targetCopy.getHealth(), before, "Reloaded sibling cannot retry a saved target claim");
            var expired = fan.get(2); var state = expired.getData(RpgData.STALKER_SHOT);
            expired.setData(RpgData.STALKER_SHOT, new StalkerArrows.Shot(0, state.fan(), ClassResources.now(p)));
            expired.tick(); check(expired.isRemoved(), "Fan lifetime expires even after chunk reload");
            SkillLifecycle.confirmedDeath(targetCopy);
            check(!targetCopy.hasData(RpgData.MULTISHOT_HITS) && !targetCopy.hasEffect(Vulnerability.EFFECT), "Death clears target state");
            helper.succeed();
        }
    }

    static void defenses(GameTestHelper helper) {
        try (var f = new Fixture(helper, 285)) {
            var p = f.player(); f.arm(p, Items.ARROW);
            var defender = f.player(); defender.setPos(p.position().add(0, 0, 3)); defender.setYRot(180);
            defender.connection.resetPosition();
            defender.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            defender.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
            for (int i = 0; i < 8; i++) { defender.doTick(); }
            defender.setYHeadRot(180); // Native shield angles use head rotation, independently of body yaw.
            check(defender.isBlocking(), "Native shield fixture is blocking");
            impact(f.fire(p, SkillDefinition.ARCANE_ARROW).getFirst(), defender);
            near(defender.getHealth(), 16, "Native armor-bypassing magic also bypasses shields through damage tags");
            check(defender.hasEffect(Vulnerability.EFFECT), "Unblocked magic damage applies vulnerability");
            defender.removeEffect(Vulnerability.EFFECT); defender.setHealth(20); defender.invulnerableTime = 0;
            ready(p); var fan = f.fire(p, SkillDefinition.MULTISHOT);
            impact(fan.get(0), defender); defender.stopUsingItem(); defender.invulnerableTime = 0;
            impact(fan.get(1), defender); near(defender.getHealth(), 20, "Blocked fan attempt cannot be retried after lowering shield");
            FixtureSkills.learn(defender, SkillDefinition.DODGE_AURA.id(), true);
            SkillLoadouts.bind(defender, 0, SkillDefinition.DODGE_AURA.id()); ready(defender);
            used(defender, SkillDefinition.DODGE_AURA);
            reset(p, SkillDefinition.MULTISHOT); var dodged = f.fire(p, SkillDefinition.MULTISHOT);
            impact(dodged.get(0), defender);
            check(!Auras.active(defender, Auras.Kind.DODGE), "First fan attempt consumes guaranteed Dodge");
            SkillLoadouts.bind(defender, 0, null); defender.invulnerableTime = 0;
            impact(dodged.get(1), defender); near(defender.getHealth(), 20, "Dodge cannot be retried by sibling arrows");
            reset(defender, SkillDefinition.DODGE_AURA); SkillLoadouts.bind(defender, 0, SkillDefinition.DODGE_AURA.id());
            used(defender, SkillDefinition.DODGE_AURA); impact(f.magicArrow(p), defender);
            check(!Auras.active(defender, Auras.Kind.DODGE) && !defender.hasEffect(Vulnerability.EFFECT), "Dodged magic arrow applies no vulnerability");
            SkillLoadouts.bind(defender, 0, null);
            PlayerProfiles.switchClass(defender, PlayerClass.REAVER);
            FixtureSkills.learn(defender, SkillDefinition.FORTITUDE_AURA.id(), true);
            SkillLoadouts.bind(defender, 0, SkillDefinition.FORTITUDE_AURA.id()); ready(defender);
            ClassResources.set(defender, PlayerClass.REAVER, 100); used(defender, SkillDefinition.FORTITUDE_AURA);
            defender.invulnerableTime = 0; impact(f.magicArrow(p), defender);
            near(Auras.shield(defender), 2, "Fortitude absorbs four-point magic hit");
            check(!defender.hasEffect(Vulnerability.EFFECT), "Fortitude-only absorption does not meet health/native-absorption gate");
            var wolf = EntityType.WOLF.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            wolf.tame(p); wolf.setPos(p.position().add(0, 0, 2)); wolf.setNoAi(true); wolf.setNoGravity(true);
            helper.getLevel().addFreshEntity(wolf); f.entities.add(wolf);
            var arrow = f.magicArrow(p); float before = wolf.getHealth(); impact(arrow, wolf);
            near(wolf.getHealth(), before, "Own pet excluded"); check(!wolf.hasEffect(Vulnerability.EFFECT), "No debuff on own pet");
            helper.succeed();
        }
    }

    static void flight(GameTestHelper helper) {
        var f = new Fixture(helper, 275);
        f.forceFlightLanes();
        helper.runAtTickTime(5, () -> {
        try (f) {
            var p = f.player(); f.arm(p, Items.ARROW); p.setXRot(8);
            var arrow = f.fire(p, SkillDefinition.ARCANE_ARROW).getFirst();
            var target = f.target(p.position().add(0, 0, 4));
            for (int i = 0; i < 3 && !arrow.isRemoved(); i++) { arrow.tick(); }
            check(target.hasEffect(Vulnerability.EFFECT), "Native flight actually collides and applies vulnerability");
            var wallPlayer = f.player(); f.arm(wallPlayer, Items.ARROW); wallPlayer.setXRot(8);
            for (int x = -1; x <= 1; x++) { for (int y = 0; y <= 2; y++) { f.block(BlockPos.containing(wallPlayer.position().add(x, y, 2))); } }
            var behind = f.target(wallPlayer.position().add(0, 0, 4));
            var blocked = f.fire(wallPlayer, SkillDefinition.ARCANE_ARROW).getFirst();
            for (int i = 0; i < 4 && !blocked.isRemoved(); i++) { blocked.tick(); }
            near(behind.getHealth(), 200, "Solid cover blocks magic arrow");
            check(!behind.hasEffect(Vulnerability.EFFECT), "No vulnerability through blocks");
            helper.succeed();
        }
        });
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final Vec3 origin;
        final List<ServerPlayer> players = new ArrayList<>();
        final List<Entity> entities = new ArrayList<>();
        final List<BlockPos> blocks = new ArrayList<>();
        final Set<net.minecraft.world.level.ChunkPos> forced = new HashSet<>();
        Fixture(GameTestHelper helper, int height) {
            this.helper = helper; var pos = helper.absolutePos(new BlockPos(2, height, 2));
            origin = new Vec3(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        }
        void forceFlightLanes() {
            for (int x = net.minecraft.util.Mth.floor(origin.x - 4) >> 4; x <= net.minecraft.util.Mth.floor(origin.x + 20) >> 4; x++) {
                for (int z = net.minecraft.util.Mth.floor(origin.z - 4) >> 4; z <= net.minecraft.util.Mth.floor(origin.z + 8) >> 4; z++) {
                    if (helper.getLevel().setChunkForced(x, z, true)) { forced.add(new net.minecraft.world.level.ChunkPos(x, z)); }
                }
            }
        }
        ServerPlayer player() {
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "stalker-test"), false);
            var p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new Connection(PacketFlow.SERVERBOUND); new io.netty.channel.embedded.EmbeddedChannel(connection);
            helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie); players.add(p);
            p.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket((int) get(p.connection, "awaitingTeleport")));
            p.setPos(origin.add((players.size() - 1) * 12, 0, 0)); p.connection.resetPosition();
            p.setGameMode(GameType.SURVIVAL); p.connection.markClientLoaded(); p.setNoGravity(true); p.setYRot(0); p.setXRot(0);
            PlayerProfiles.selectClass(p, PlayerClass.STALKER);
            for (var skill : List.of(SkillDefinition.DISENGAGE, SkillDefinition.ARCANE_ARROW, SkillDefinition.MULTISHOT)) { FixtureSkills.learn(p, skill.id(), true); }
            ClassResources.set(p, PlayerClass.STALKER, 100); return p;
        }
        LivingEntity target(Vec3 position) {
            var target = EntityType.COW.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            target.setPos(position); target.setNoAi(true); target.setNoGravity(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            helper.getLevel().addFreshEntity(target); entities.add(target); return target;
        }
        void arm(ServerPlayer p, Item ammo) { p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW)); p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ammo, 20)); }
        List<AbstractArrow> fire(ServerPlayer p, SkillDefinition skill) {
            var previous = new HashSet<>(helper.getLevel().getEntitiesOfClass(AbstractArrow.class, p.getBoundingBox().inflate(4)));
            used(p, skill);
            var result = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, p.getBoundingBox().inflate(4), a -> a.getOwner() == p && !previous.contains(a));
            check(!result.isEmpty(), "Activation spawned native arrow(s)"); entities.addAll(result); return result;
        }
        AbstractArrow magicArrow(ServerPlayer p) { reset(p, SkillDefinition.ARCANE_ARROW); return fire(p, SkillDefinition.ARCANE_ARROW).getFirst(); }
        AbstractArrow copyArrow(AbstractArrow original) {
            var copy = (AbstractArrow) original.getType().create(helper.getLevel(), EntitySpawnReason.COMMAND);
            load(copy, save(original)); entities.add(copy); return copy;
        }
        double focus(ServerPlayer p) { return ClassResources.current(p, PlayerClass.STALKER); }
        void block(BlockPos pos) { helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()); blocks.add(pos); }
        @Override public void close() {
            entities.forEach(Entity::discard);
            players.forEach(p -> { Combos.clear(p); Auras.clear(p); helper.getLevel().getServer().getPlayerList().remove(p); });
            blocks.forEach(pos -> helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
            forced.forEach(chunk -> helper.getLevel().setChunkForced(chunk.x(), chunk.z(), false));
        }
    }
    private static void impact(AbstractArrow arrow, LivingEntity target) {
        try { var method = AbstractArrow.class.getDeclaredMethod("onHitEntity", EntityHitResult.class); method.setAccessible(true); method.invoke(arrow, new EntityHitResult(target)); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static net.minecraft.nbt.CompoundTag save(Entity entity) {
        var out = TagValueOutput.createWithContext(new ProblemReporter.Collector(), entity.registryAccess()); entity.saveWithoutId(out); return out.buildResult();
    }
    private static void load(Entity entity, net.minecraft.nbt.CompoundTag tag) { tag.remove("UUID"); entity.load(TagValueInput.create(new ProblemReporter.Collector(), entity.registryAccess(), tag)); }
    private static void reset(ServerPlayer p, SkillDefinition skill) { ready(p); p.setData(RpgData.SKILLS, p.getData(RpgData.SKILLS).withCooldown(skill.id(), 0)); ClassResources.set(p, PlayerClass.STALKER, 100); }
    private static void ready(ServerPlayer p) { set(p.getData(RpgData.SKILL_RUNTIME), "nextActivation", 0L); }
    private static void used(ServerPlayer p, SkillDefinition skill) { var result = Skills.activate(p, skill); check(key(result, "used"), "Accepted " + skill.id() + ": " + result.getString()); }
    private static boolean key(Component value, String suffix) { return value.getContents() instanceof TranslatableContents text && text.getKey().equals("commands.covenant.skill." + suffix); }
    private static Object get(Object object, String name) {
        try { var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void set(Object object, String name, Object value) {
        try { var field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object, value); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void steps(ServerPlayer p, int count) {
        for (int i = 0; i < count; i++) { var movement = get(p.getData(RpgData.SKILL_RUNTIME), "movement"); if (movement != null) { set(movement, "lastTick", Long.MIN_VALUE); } NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre(p)); }
    }
    private static void near(double actual, double expected, String message) { check(Math.abs(actual - expected) < .001, message + ": expected " + expected + ", got " + actual); }
    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
}
