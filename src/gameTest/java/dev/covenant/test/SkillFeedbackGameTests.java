package dev.covenant.test;

import com.mojang.authlib.GameProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.ArcaneBolt;
import dev.covenant.rpg.skills.CombatParticles;
import dev.covenant.rpg.skills.ElementalBolt;
import dev.covenant.rpg.skills.ElementalBolts;
import dev.covenant.rpg.skills.LightningVisual;
import dev.covenant.rpg.skills.Auras;
import dev.covenant.rpg.skills.Bleeding;
import dev.covenant.rpg.skills.ProjectileCombat;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLifecycle;
import dev.covenant.rpg.skills.Skills;
import dev.covenant.rpg.skills.WeaverPower;
import dev.covenant.rpg.stats.RpgStats;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/** Native sound/particle emission boundaries; these do not establish audible or rendered client quality. */
final class SkillFeedbackGameTests {
    private SkillFeedbackGameTests() {}

    static void auraActivations(GameTestHelper helper) {
        loadedFeedback(helper, 135, f -> {
            for (var kind : Auras.Kind.values()) {
                if (kind == Auras.Kind.BATTLE_CRY || kind == Auras.Kind.DISCHARGE_FREEZE) { continue; }
                var skill = SkillDefinition.find(kind.id).orElseThrow();
                var playerClass = skill.allowedClasses().iterator().next();
                var p = f.player(playerClass, skill); p.setPos(f.origin);
                learnAura(p, skill);
                dev.covenant.rpg.skills.SkillLoadouts.bind(p, 0, skill.id());
                check(Auras.assigned(p, kind), "Assigned aura: " + kind);
                var enemy = f.target(p.position().add(0, 0, 2));
                var immune = f.target(p.position().add(0, 0, 3)); immune.setInvulnerable(true);
                var packets = f.particlePackets.get(p.getUUID()); packets.clear();
                Auras.reconcile(p); Auras.refresh(p); Auras.refresh(p);
                check(packets.isEmpty(), "Passive upkeep is quiet: " + kind);
                ClassResources.set(p, playerClass, 0);
                check(key(Skills.activate(p, skill), "resource"), "Unpaid aura rejected: " + kind);
                check(packets.isEmpty(), "No rejected aura spectacle");
                ClassResources.set(p, playerClass, 100); comboUse(p, skill);
                var release = f.particles(p, CombatParticles.AURA_RELEASE.get());
                var receipt = f.particles(p, CombatParticles.AURA_RECEIVED.get());
                check(release.size() == 1 && receipt.size() == 1, "One release and one eligible receipt: " + kind);
                near(release.getFirst().getXDist(), kind.ordinal(), "Exact aura visual identity");
                check(!release.getFirst().alwaysShow() && !release.getFirst().isOverrideLimiter(), "Native limiter respected");
                LivingEntity recipient = kind == Auras.Kind.FREEZING ? enemy : p;
                near(receipt.getFirst().getZ(), recipient.getZ(), "Receipt at actual recipient");
                check(kind == Auras.Kind.FORTITUDE ? Auras.shield(recipient) == 6 : Auras.active(recipient, kind), "Receipt agrees with active gameplay");
                expireAura(recipient, kind, p.getUUID()); tickAura(recipient); tickAura(recipient);
                check(f.particles(p, CombatParticles.AURA_EXPIRED.get()).size() == 1, "One final expiry: " + kind);
                SkillLifecycle.cancelPlayer(p, SkillLifecycle.Reason.LOGOUT);
                check(f.particles(p, CombatParticles.AURA_EXPIRED.get()).size() == 1, "Cleanup does not replay expiry");
                enemy.discard(); immune.discard(); p.setPos(f.origin.add(60, 0, 0));
            }
            helper.succeed();
        });
    }

    static void auraOverlap(GameTestHelper helper) {
        loadedFeedback(helper, 125, f -> {
            var one = f.player(PlayerClass.STALKER, SkillDefinition.SWIFTNESS_AURA);
            var two = f.player(PlayerClass.STALKER, SkillDefinition.SWIFTNESS_AURA); two.setPos(one.position().add(2, 0, 0));
            var pet = (net.minecraft.world.entity.animal.wolf.Wolf) f.target(one.position().add(0, 0, 2), EntityType.WOLF); pet.tame(two);
            var board = helper.getLevel().getScoreboard();
            var team = board.addPlayerTeam("af" + UUID.randomUUID().toString().substring(0, 8));
            try {
                board.addPlayerToTeam(one.getScoreboardName(), team); board.addPlayerToTeam(two.getScoreboardName(), team);
                for (var p : List.of(one, two)) {
                    dev.covenant.rpg.skills.SkillLoadouts.bind(p, 0, SkillDefinition.SWIFTNESS_AURA.id());
                    comboUse(p, SkillDefinition.SWIFTNESS_AURA);
                }
                check(f.particles(one, CombatParticles.AURA_RECEIVED.get()).size() == 6, "Each cast marks its real caster, teammate and teammate pet");
                expireAura(pet, Auras.Kind.SWIFTNESS, one.getUUID()); tickAura(pet);
                check(Auras.active(pet, Auras.Kind.SWIFTNESS), "Other source retains active effect");
                check(f.particles(one, CombatParticles.AURA_EXPIRED.get()).isEmpty(), "One expired supplier cannot claim final expiry");
                expireAura(pet, Auras.Kind.SWIFTNESS, two.getUUID()); tickAura(pet); tickAura(pet);
                check(f.particles(one, CombatParticles.AURA_EXPIRED.get()).size() == 1, "Last supplier expires once");
                var late = f.target(one.position().add(0, 0, 3), EntityType.WOLF);
                ((net.minecraft.world.entity.animal.wolf.Wolf) late).tame(one);
                Auras.refresh(one);
                check(!Auras.active(late, Auras.Kind.SWIFTNESS), "Late entrants have no active receipt");
                check(f.particles(one, CombatParticles.AURA_RECEIVED.get()).size() == 6, "Refresh never replays receipts");
                SkillLifecycle.cancelPlayer(one, SkillLifecycle.Reason.LOGOUT);
                SkillLifecycle.cancelPlayer(two, SkillLifecycle.Reason.DIMENSION_CHANGE);
                check(f.particles(one, CombatParticles.AURA_EXPIRED.get()).size() == 1, "Source cleanup stays quiet");
            } finally { board.removePlayerTeam(team); }
            helper.succeed();
        });
    }

    static void auraDefenses(GameTestHelper helper) {
        loadedFeedback(helper, 115, f -> {
            var p = f.player(PlayerClass.STALKER, SkillDefinition.DODGE_AURA);
            dev.covenant.rpg.skills.SkillLoadouts.bind(p, 0, SkillDefinition.DODGE_AURA.id());
            comboUse(p, SkillDefinition.DODGE_AURA);
            var enemy = f.target(p.position().add(0, 0, 2));
            float health = p.getHealth();
            p.hurtServer(p.level(), p.damageSources().mobAttack(enemy), 2);
            near(p.getHealth(), health, "Guaranteed native dodge prevents damage");
            check(!Auras.active(p, Auras.Kind.DODGE), "Guarantee consumed");
            check(f.particles(p, CombatParticles.AURA_CONSUMED.get()).size() == 1, "One actual Dodge consumption cue");
            tickAura(p);
            check(f.particles(p, CombatParticles.AURA_EXPIRED.get()).isEmpty(), "Consumed Dodge has no expiry cue");
            SkillLifecycle.cancelPlayer(p, SkillLifecycle.Reason.LOGOUT); p.setPos(f.origin.add(60, 0, 0));
            var shield = f.player(PlayerClass.REAVER, SkillDefinition.FORTITUDE_AURA); shield.setPos(f.origin);
            dev.covenant.rpg.skills.SkillLoadouts.bind(shield, 0, SkillDefinition.FORTITUDE_AURA.id());
            comboUse(shield, SkillDefinition.FORTITUDE_AURA);
            shield.setInvulnerable(true); shield.hurtServer(shield.level(), shield.damageSources().mobAttack(enemy), 2);
            check(f.particles(shield, CombatParticles.AURA_ABSORB.get()).isEmpty(), "Rejected hit never claims shield absorption");
            shield.setInvulnerable(false);
            shield.hurtServer(shield.level(), shield.damageSources().mobAttack(enemy), 2);
            check(Auras.shield(shield) > 0 && Auras.shield(shield) < 6, "Native damage spends partial shield");
            check(f.particles(shield, CombatParticles.AURA_ABSORB.get()).size() == 1, "Partial absorption has one compact cue");
            shield.invulnerableTime = 0;
            shield.hurtServer(shield.level(), shield.damageSources().mobAttack(enemy), 10);
            near(Auras.shield(shield), 0, "Native overflow depletes shield");
            check(f.particles(shield, CombatParticles.AURA_CONSUMED.get()).size() == 1, "Depletion emits one breakup");
            tickAura(shield); tickAura(shield);
            check(f.particles(shield, CombatParticles.AURA_EXPIRED.get()).isEmpty(), "Depleted shield never claims natural expiry");
            Skills.setIgnoreCooldowns(shield, true); ClassResources.set(shield, PlayerClass.REAVER, 100);
            comboUse(shield, SkillDefinition.FORTITUDE_AURA);
            comboUse(shield, SkillDefinition.FORTITUDE_AURA);
            near(Auras.shield(shield), 6, "Recasts replace capacity");
            check(f.particles(shield, CombatParticles.AURA_CONSUMED.get()).size() == 1, "Replacement is not a break");
            dev.covenant.rpg.skills.SkillLoadouts.bind(shield, 0, null); tickAura(shield);
            check(f.particles(shield, CombatParticles.AURA_EXPIRED.get()).isEmpty(), "Unassignment is quiet");
            helper.succeed();
        });
    }

    private static void learnAura(ServerPlayer p, SkillDefinition skill) {
        dev.covenant.rpg.skills.SkillTree.find(skill.id()).orElseThrow().prerequisite()
                .ifPresent(id -> learnAura(p, SkillDefinition.find(id).orElseThrow()));
        FixtureSkills.learn(p, skill.id(), true);
    }

    @SuppressWarnings("unchecked")
    private static void expireAura(LivingEntity target, Auras.Kind kind, UUID owner) {
        try {
            var state = target.getData(RpgData.AURAS);
            var next = state.getClass().getDeclaredField("nextExpiry"); next.setAccessible(true); next.setLong(state, 0);
            if (kind == Auras.Kind.FORTITUDE) {
                var until = state.getClass().getDeclaredField("shieldUntil"); until.setAccessible(true); until.setLong(state, 0);
            } else {
                var active = state.getClass().getDeclaredField("active"); active.setAccessible(true);
                var grants = (java.util.Map<Auras.Kind, java.util.Map<UUID, Long>>) active.get(state);
                grants.get(kind).put(owner, 0L);
            }
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    private static void tickAura(LivingEntity target) { NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(target)); }

    static void dischargeFeedback(GameTestHelper helper) {
        loadedFeedback(helper, 155, f -> {
            var p = f.player(PlayerClass.WEAVER, SkillDefinition.DISCHARGE);
            chargeWeaver(p, 2, 0, 5);
            var packets = f.particlePackets.get(p.getUUID()); packets.clear(); f.beams.get(p.getUUID()).clear();
            ClassResources.set(p, PlayerClass.WEAVER, 0);
            clearThrottle(p);
            check(key(Skills.activate(p, SkillDefinition.DISCHARGE), "resource"), "Unpaid Discharge rejected");
            check(packets.isEmpty(), "Rejected Discharge has no release");
            near(WeaverPower.charges(p, WeaverPower.Element.FIRE), 2, "Rejection retains real charges");
            ClassResources.set(p, PlayerClass.WEAVER, 100);
            p.getAttribute(RpgStats.CAST_TIME).setBaseValue(1);
            check(key(Skills.activate(p, SkillDefinition.DISCHARGE), "started"), "Discharge wind-up accepted");
            SkillLifecycle.cancelPlayer(p, SkillLifecycle.Reason.DIMENSION_CHANGE);
            check(f.particles(p, CombatParticles.DISCHARGE_FIRE.get()).isEmpty(), "Cancelled wind-up has no release");
            chargeWeaver(p, 2, 0, 5);
            packets.clear(); f.beams.get(p.getUUID()).clear();
            var first = f.target(p.position().add(0, 0, 4), EntityType.HUSK);
            var second = f.target(p.position().add(0, 0, 7), EntityType.HUSK);
            var third = f.target(p.position().add(0, 0, 10), EntityType.HUSK);
            var immune = f.target(p.position().add(0, 0, -4.5)); immune.setInvulnerable(true);
            for (var target : List.of(first, second, third)) {
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0); target.setHealth(200);
            }
            comboUse(p, SkillDefinition.DISCHARGE);
            var fire = f.particles(p, CombatParticles.DISCHARGE_FIRE.get());
            var lightning = f.particles(p, CombatParticles.DISCHARGE_LIGHTNING.get());
            check(fire.size() == 1 && lightning.size() == 1, "One emitter per consumed element");
            check(f.particles(p, CombatParticles.DISCHARGE_ICE.get()).isEmpty(), "Absent ice bank has no cosmetic burst");
            near(fire.getFirst().getXDist(), 5, "Release radius matches live tuning");
            near(fire.getFirst().getYDist(), .4, "Fire uses actual two-fifths consumed strength");
            near(fire.getFirst().getZDist(), 0, "Partial fire does not claim full-charge rider");
            near(lightning.getFirst().getYDist(), 1, "Full lightning strength");
            near(lightning.getFirst().getZDist(), 1, "Full lightning rider flag");
            check(!fire.getFirst().alwaysShow() && !fire.getFirst().isOverrideLimiter(), "Release respects native particle limiting");
            var beams = f.beams.get(p.getUUID());
            check(beams.size() == 2, "Only two real external chains");
            check(beams.getFirst().start().equals(first.getEyePosition()) && beams.getFirst().end().equals(second.getEyePosition()), "First chain uses actual targets");
            check(beams.getLast().start().equals(second.getEyePosition()) && beams.getLast().end().equals(third.getEyePosition()), "Second chain uses actual targets");
            check(packets.stream().filter(packet -> packet.getParticle().getType() == CombatParticles.CONTACT.get()).count() == 3, "One accepted contact per target, no protected contact or per-element duplicates");
            check(first.getHealth() < 200 && second.getHealth() < 200 && third.getHealth() < 200, "Visual contacts agree with native damage");
            near(immune.getHealth(), immune.getMaxHealth(), "Protected target stays unharmed");
            for (var element : WeaverPower.Element.values()) { near(WeaverPower.charges(p, element), 0, "All charges consumed once"); }
            clearThrottle(p);
            check(!key(Skills.activate(p, SkillDefinition.DISCHARGE), "used"), "Empty bank cannot release again");
            check(f.particles(p, CombatParticles.DISCHARGE_FIRE.get()).size() == 1, "No duplicate release");
            check(f.particles(p, net.minecraft.core.particles.ParticleTypes.ENCHANT).isEmpty(), "Old enchant cloud removed");
            helper.succeed();
        });
    }

    static void threefoldFeedback(GameTestHelper helper) {
        loadedFeedback(helper, 145, f -> {
            var p = f.player(PlayerClass.WEAVER, SkillDefinition.THREEFOLD_SYSTEM);
            dev.covenant.rpg.skills.SkillLoadouts.bind(p, 0, SkillDefinition.THREEFOLD_SYSTEM.id());
            check(WeaverPower.assigned(p), "Threefold fixture assigned");
            Skills.setIgnoreCooldowns(p, true);
            var target = f.target(p.position().add(0, 0, 2));
            var holder = p.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, SkillDefinition.FIRE_BOLT.id()));
            var source = new net.minecraft.world.damagesource.DamageSource(holder, p, p);
            target.hurtServer(p.level(), source, 4);
            near(WeaverPower.reserve(p, WeaverPower.Element.ICE), 1, "Actual damage banks opposite reserves");
            var packets = f.particlePackets.get(p.getUUID()); packets.clear();
            refreshWeaver(p, false); refreshWeaver(p, false);
            check(packets.isEmpty(), "Reserve refresh stays quiet");
            ClassResources.set(p, PlayerClass.WEAVER, 0); clearThrottle(p);
            check(key(Skills.activate(p, SkillDefinition.THREEFOLD_SYSTEM), "resource"), "Unpaid Threefold rejected");
            check(packets.isEmpty(), "Rejected activation emits no consumption");
            ClassResources.set(p, PlayerClass.WEAVER, 100);
            comboUse(p, SkillDefinition.THREEFOLD_SYSTEM);
            var consume = f.particles(p, CombatParticles.THREEFOLD_CONSUME.get());
            check(consume.size() == 1, "One activation receipt");
            near(consume.getFirst().getXDist(), 0, "No fire reserve consumed");
            near(consume.getFirst().getYDist(), 1.0 / 6, "Actual ice reserve fraction");
            near(consume.getFirst().getZDist(), 1.0 / 6, "Actual lightning reserve fraction");
            near(WeaverPower.bonus(p, WeaverPower.Element.ICE), .05, "Visual agrees with active bonus");
            near(WeaverPower.reserve(p, WeaverPower.Element.ICE), 0, "Bank consumed");
            clearThrottle(p);
            check(!key(Skills.activate(p, SkillDefinition.THREEFOLD_SYSTEM), "used"), "Active buff cannot be consumed again");
            check(f.particles(p, CombatParticles.THREEFOLD_CONSUME.get()).size() == 1, "Rejected reactivation stays quiet");
            refreshWeaver(p, false);
            check(f.particles(p, CombatParticles.THREEFOLD_EXPIRE.get()).isEmpty(), "No premature expiry");
            refreshWeaver(p, true); refreshWeaver(p, false);
            var expired = f.particles(p, CombatParticles.THREEFOLD_EXPIRE.get());
            check(expired.size() == 1, "Natural expiry emits exactly once across refreshes");
            near(expired.getFirst().getXDist(), 6, "Expiry retains only consumed ice/lightning identity");
            near(WeaverPower.bonus(p, WeaverPower.Element.ICE), 0, "Expiry follows actual bonus deadline");
            target.invulnerableTime = 0; target.hurtServer(p.level(), source, 2);
            comboUse(p, SkillDefinition.THREEFOLD_SYSTEM);
            dev.covenant.rpg.skills.SkillLoadouts.bind(p, 0, null);
            refreshWeaver(p, false);
            check(f.particles(p, CombatParticles.THREEFOLD_EXPIRE.get()).size() == 1, "Unassignment is silent cancellation");
            dev.covenant.rpg.skills.SkillLoadouts.bind(p, 0, SkillDefinition.THREEFOLD_SYSTEM.id());
            target.invulnerableTime = 0; target.hurtServer(p.level(), source, 2);
            comboUse(p, SkillDefinition.THREEFOLD_SYSTEM);
            SkillLifecycle.cancelPlayer(p, SkillLifecycle.Reason.LOGOUT); refreshWeaver(p, false);
            check(f.particles(p, CombatParticles.THREEFOLD_EXPIRE.get()).size() == 1, "Lifecycle cleanup cannot replay expiry");
            check(f.particles(p, CombatParticles.THREEFOLD_CONSUME.get()).size() == 3, "Only accepted activations emit consumption");
            helper.succeed();
        });
    }

    private static void chargeWeaver(ServerPlayer p, int fire, int ice, int lightning) {
        p.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
        ClassResources.setInfinite(p, true); Skills.setIgnoreCooldowns(p, true);
        float pitch = p.getXRot(); p.setXRot(-90);
        int[] counts = { fire, ice, lightning };
        var bolts = new SkillDefinition[] { SkillDefinition.FIRE_BOLT, SkillDefinition.ICE_BOLT, SkillDefinition.LIGHTNING_BOLT };
        for (int e = 0; e < 3; e++) {
            FixtureSkills.learn(p, bolts[e].id(), true);
            for (int i = 0; i < counts[e]; i++) { comboUse(p, bolts[e]); }
        }
        p.setXRot(pitch); ClassResources.setInfinite(p, false);
    }

    private static void refreshWeaver(ServerPlayer p, boolean expire) {
        try {
            if (expire) {
                var runtime = p.getData(RpgData.SKILL_RUNTIME);
                var field = runtime.getClass().getDeclaredField("weaver"); field.setAccessible(true);
                var state = field.get(runtime);
                var until = state.getClass().getDeclaredField("until"); until.setAccessible(true);
                until.setLong(state, ClassResources.now(p) - 1);
            }
            var sync = WeaverPower.class.getDeclaredMethod("sync", ServerPlayer.class); sync.setAccessible(true); sync.invoke(null, p);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    static void cryFeedback(GameTestHelper helper) {
        loadedFeedback(helper, 165, f -> {
            var source = f.player(PlayerClass.REAVER, SkillDefinition.BATTLE_CRY);
            var ally = f.player(PlayerClass.WEAVER, SkillDefinition.ARCANE_BOLT); ally.setPos(source.position().add(0, 0, 2));
            var outsider = f.player(PlayerClass.STALKER, SkillDefinition.DISENGAGE); outsider.setPos(source.position().add(2, 0, 0));
            var late = f.player(PlayerClass.REAVER, SkillDefinition.BATTLE_CRY); late.setPos(source.position().add(0, 0, 9));
            var scoreboard = helper.getLevel().getScoreboard();
            var team = scoreboard.addPlayerTeam("cry-" + UUID.randomUUID().toString().substring(0, 8));
            try {
                for (var member : List.of(source, ally, late)) { scoreboard.addPlayerToTeam(member.getScoreboardName(), team); }
                ClassResources.set(source, PlayerClass.REAVER, 0);
                check(key(Skills.activate(source, SkillDefinition.BATTLE_CRY), "resource"), "Unpaid shout rejected");
                check(f.particles(source, CombatParticles.BATTLE_CRY_PULSE.get()).isEmpty(), "No pulse on rejected shout");
                ClassResources.set(source, PlayerClass.REAVER, 100);
                check(key(Skills.activate(source, SkillDefinition.BATTLE_CRY), "used"), "Shout accepted");
                var pulses = f.particles(source, CombatParticles.BATTLE_CRY_PULSE.get());
                check(pulses.size() == 1, "One shout-time pulse");
                near(pulses.getFirst().getXDist(), dev.covenant.rpg.skills.ReaverSkillData.get(helper.getLevel().getServer()).cry().radius(), "Pulse carries live shout radius");
                var receipts = f.particles(source, CombatParticles.BATTLE_CRY_RECEIVED.get());
                check(receipts.size() == 2, "Only source and actual nearby ally receive marks; got " + receipts.size());
                check(receipts.stream().anyMatch(p -> Math.abs(p.getZ() - ally.getZ()) < .001), "Ally's actual position marked");
                check(receipts.stream().noneMatch(p -> Math.abs(p.getX() - outsider.getX()) < .001), "Outsider has no receipt");
                near(Auras.battleCryBonus(ally), .15, "Marked ally has actual grant");
                near(Auras.battleCryBonus(outsider), 0, "Outsider has no grant");
                late.setPos(source.position().add(0, 0, 3));
                dev.covenant.rpg.skills.BuffDisplay.sync(late);
                near(Auras.battleCryBonus(late), 0, "Late entrant gets no retroactive grant");
                check(f.particles(source, CombatParticles.BATTLE_CRY_RECEIVED.get()).size() == 2, "Display refresh cannot replay receipts");
                SkillLifecycle.cancelPlayer(source, SkillLifecycle.Reason.LOGOUT);
                near(Auras.battleCryBonus(ally), 0, "Lifecycle clears actual grant");
                check(f.particles(source, CombatParticles.BATTLE_CRY_PULSE.get()).size() == 1, "Cleanup emits no new shout");
            } finally { scoreboard.removePlayerTeam(team); }
            helper.succeed();
        });
    }

    static void stormFeedback(GameTestHelper helper) {
        loadedFeedback(helper, 175, f -> {
            var p = f.player(PlayerClass.REAVER, SkillDefinition.RAGE_STORM);
            check(key(Skills.activate(p, SkillDefinition.RAGE_STORM), "started"), "Storm has real wind-up");
            check(helper.getLevel().getEntitiesOfClass(dev.covenant.rpg.skills.RageStorm.class, p.getBoundingBox().inflate(8)).isEmpty(), "No shell entity before release");
            SkillLifecycle.cancelPlayer(p, SkillLifecycle.Reason.DIMENSION_CHANGE);
            near(ClassResources.current(p, PlayerClass.REAVER), 100, "Cancelled wind-up remains free");
            check(f.particles(p, CombatParticles.STORM_DISSIPATE.get()).isEmpty(), "Cancelled wind-up has no release/end cue");
            p.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
            Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> reject = event -> {
                if (event.getEntity() instanceof dev.covenant.rpg.skills.RageStorm storm && storm.getOwner() == p) {
                    near(storm.visualRadius(), 0, "Uncommitted storm has no visible shell"); event.setCanceled(true);
                }
            };
            NeoForge.EVENT_BUS.addListener(reject);
            try { check(key(Skills.activate(p, SkillDefinition.RAGE_STORM), "unavailable"), "Rejected storm admission"); }
            finally { NeoForge.EVENT_BUS.unregister(reject); }
            near(ClassResources.current(p, PlayerClass.REAVER), 100, "Admission rejection spends nothing");
            var target = f.target(p.position().add(0, 0, 1));
            var immune = f.target(p.position().add(1, 0, 1)); immune.setInvulnerable(true);
            check(key(Skills.activate(p, SkillDefinition.RAGE_STORM), "used"), "Instant test release accepted");
            var storm = helper.getLevel().getEntitiesOfClass(dev.covenant.rpg.skills.RageStorm.class, p.getBoundingBox().inflate(8)).getFirst();
            double radius = dev.covenant.rpg.skills.ReaverSkillData.get(helper.getLevel().getServer()).storm().radius();
            near(storm.visualRadius(), radius, "Synced shell radius matches release tuning");
            check(storm.shouldRenderAtSqrDistance(32 * 32), "Visual radius prevents tiny-body distance culling");
            check(storm.getEntityData().getNonDefaultValues().stream().anyMatch(value -> value.value() instanceof Float number && Math.abs(number - radius) < .001), "Radius participates in native entity tracking");
            check(target.getHealth() < target.getMaxHealth() && immune.getHealth() == immune.getMaxHealth(), "Release pulse respects immunity");
            check(f.particles(p, CombatParticles.COMBO_CONTACT.get()).size() == 1, "Only actual accepted storm damage has contact cue");
            var wall = BlockPos.containing(storm.position().add(0, 0, 1));
            f.blocks.add(wall); helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            for (int i = 0; i < 5 && !storm.isRemoved(); i++) { storm.tick(); }
            check(storm.isRemoved(), "Terrain terminates tracked shell");
            var endings = f.particles(p, CombatParticles.STORM_DISSIPATE.get());
            check(endings.size() == 1, "One terrain-stop breakup");
            near(endings.getFirst().getZ(), storm.getZ(), "Breakup uses last valid center, not an imagined endpoint");
            check(f.particles(p, net.minecraft.core.particles.ParticleTypes.CRIMSON_SPORE).isEmpty(), "Old cloud stream is removed");
            f.blocks.remove(wall); helper.getLevel().setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
            reset(p, SkillDefinition.RAGE_STORM);
            check(key(Skills.activate(p, SkillDefinition.RAGE_STORM), "used"), "Second release accepted");
            var cancelled = helper.getLevel().getEntitiesOfClass(dev.covenant.rpg.skills.RageStorm.class, p.getBoundingBox().inflate(8)).getFirst();
            SkillLifecycle.cancelPlayer(p, SkillLifecycle.Reason.LOGOUT);
            check(cancelled.isRemoved(), "Lifecycle removes visual entity");
            check(f.particles(p, CombatParticles.STORM_DISSIPATE.get()).size() == 1, "Lifecycle does not replay natural completion");
            helper.succeed();
        });
    }

    static void feverFeedback(GameTestHelper helper) {
        loadedFeedback(helper, 185, f -> {
            var p = f.player(PlayerClass.REAVER, SkillDefinition.BLOOD_FEVER);
            dev.covenant.rpg.skills.SkillLoadouts.bind(p, 0, SkillDefinition.BLOOD_FEVER.id());
            check(dev.covenant.rpg.skills.BloodFever.assigned(p), "Passive binds without Rend");
            var target = f.target(p.position().add(0, 0, 2)); Bleeding.apply(target, p, .1f, 100);
            dev.covenant.rpg.skills.BloodFever.refresh(p);
            var rows = p.getData(RpgData.BUFF_DISPLAY).rows();
            check(rows.stream().anyMatch(row -> row.skill().equals(SkillDefinition.BLOOD_FEVER.id()) && row.stacks() == 1 && row.passive()), "HUD reflects one bleeding enemy");
            Bleeding.apply(target, p, .1f, 100); Bleeding.apply(target, p, .1f, 100);
            for (int i = 0; i < 10; i++) { dev.covenant.rpg.skills.BloodFever.refresh(p); }
            check(p.getData(RpgData.BUFF_DISPLAY).rows().equals(rows), "Extra bleed stacks and passive refresh do not alter enemy count");
            Bleeding.consume(target); dev.covenant.rpg.skills.BloodFever.refresh(p);
            check(p.getData(RpgData.BUFF_DISPLAY).rows().stream().noneMatch(row -> row.skill().equals(SkillDefinition.BLOOD_FEVER.id())), "Zero count removes passive tile");
            check(f.particlePackets.get(p.getUUID()).isEmpty() && f.soundsAt(p) == 0, "Passive refresh/count changes emit no world spectacle or sound");
            helper.succeed();
        });
    }

    private static void loadedFeedback(GameTestHelper helper, int height, Consumer<Fixture> test) {
        var fixture = new Fixture(helper, height);
        fixture.loadFeedbackArea();
        helper.runBeforeTestEnd(fixture::close);
        helper.startSequence().thenWaitUntil(fixture::assertFeedbackReady).thenExecute(() -> {
            try (fixture) { test.accept(fixture); }
            catch (RuntimeException error) { helper.fail(error.toString()); }
        });
    }

    static void lungingFeedback(GameTestHelper helper) {
        try (var f = new Fixture(helper, 175)) {
            var p = f.player(PlayerClass.REAVER, SkillDefinition.LUNGING_COMBO);
            var bystander = f.target(p.position().add(0, 0, 2));
            float health = bystander.getHealth();
            var start = p.position();
            comboUse(p, SkillDefinition.LUNGING_COMBO);
            for (int i = 0; i < 3; i++) { movementStep(p); }
            near(bystander.getHealth(), health, "Lunge remains movement-only");
            var trail = f.particles(p, CombatParticles.LUNGE_TRAIL.get());
            check(trail.size() == 3, "One lunge trail packet per real step");
            var cursor = start.add(0, .3, 0);
            for (var packet : trail) {
                near(packet.getZ(), cursor.z, "Trail starts at actual prior position");
                near(packet.getY(), cursor.y, "Trail stays below aiming line");
                check(!packet.alwaysShow() && !packet.isOverrideLimiter(), "Trail uses native limiter");
                cursor = cursor.add(packet.getXDist(), packet.getYDist(), packet.getZDist());
            }
            near(cursor.distanceTo(p.position().add(0, .3, 0)), 0, "Trail matches actual endpoint");
            check(f.particles(p, CombatParticles.COMBO_CONTACT.get()).isEmpty(), "Movement-only stage never claims a hit");
            var front = f.target(p.position().add(0, 0, 2));
            p.setXRot(12);
            comboUse(p, SkillDefinition.LUNGING_COMBO);
            check(f.particles(p, CombatParticles.LUNGE_STRIKE.get()).size() == 1, "Second stage has a compact jab");
            check(f.particles(p, CombatParticles.COMBO_CONTACT.get()).size() == 1, "Native picked target confirms strike");
            check(front.getHealth() < health, "Strike really damages its picked target");
            front.invulnerableTime = 0;
            comboUse(p, SkillDefinition.LUNGING_COMBO);
            check(f.particles(p, CombatParticles.CLEAVE.get()).size() == 1, "Finisher retains the original frontal crescent");
            check(f.particles(p, CombatParticles.RUSH_FINISHER.get()).isEmpty(), "Frontal finisher never claims circular coverage");
            near(bystander.getHealth(), health, "Rear target remains outside frontal finisher");
            check(f.particles(p, CombatParticles.COMBO_CONTACT.get()).size() == 2, "One contact per accepted strike");
            movementStep(p);
            check(f.particles(p, CombatParticles.LUNGE_TRAIL.get()).size() == 3, "No post-movement trail");

            reset(p, SkillDefinition.LUNGING_COMBO);
            f.entities.forEach(Entity::discard);
            comboUse(p, SkillDefinition.LUNGING_COMBO);
            movementStep(p);
            dev.covenant.rpg.skills.Rooted.apply(p, 60);
            f.particlePackets.get(p.getUUID()).clear();
            movementStep(p);
            check(f.particles(p, CombatParticles.LUNGE_TRAIL.get()).isEmpty(), "Interrupted lunge stops emitting");
            p.removeEffect(dev.covenant.rpg.skills.Rooted.EFFECT);
            var immune = f.target(p.position().add(0, 0, 2)); immune.setInvulnerable(true);
            comboUse(p, SkillDefinition.LUNGING_COMBO);
            check(f.particles(p, CombatParticles.LUNGE_STRIKE.get()).size() == 1, "Defended strike still shows its action");
            check(f.particles(p, CombatParticles.COMBO_CONTACT.get()).isEmpty(), "Defended strike has no damage mark");
            immune.discard();
            comboUse(p, SkillDefinition.LUNGING_COMBO);
            check(f.particles(p, CombatParticles.CLEAVE.get()).size() == 1, "Empty finisher still shows its paid action");
            check(f.particles(p, CombatParticles.COMBO_CONTACT.get()).isEmpty(), "Whiff has no hit mark");
            helper.succeed();
        }
    }

    static void rushFeedback(GameTestHelper helper) {
        try (var f = new Fixture(helper, 185)) {
            var p = f.player(PlayerClass.REAVER, SkillDefinition.RIP_AND_RUSH);
            var front = f.target(p.position().add(0, 0, 2));
            var rear = f.target(p.position().add(0, 0, -2));
            var immune = f.target(p.position().add(2, 0, 0)); immune.setInvulnerable(true);
            comboUse(p, SkillDefinition.RIP_AND_RUSH);
            check(f.particles(p, CombatParticles.RIP_SWEEP.get()).size() == 1, "One opening circular release");
            check(Bleeding.count(front) == 1 && Bleeding.count(rear) == 1 && Bleeding.count(immune) == 0, "Opening hits front/rear but preserves immunity");
            check(f.particles(p, CombatParticles.REND_CONTACT.get()).size() == 2, "Opening uses accepted Rend contacts");
            check(f.particles(p, CombatParticles.CLEAVE.get()).isEmpty(), "Circular attack never uses frontal crescent");
            front.invulnerableTime = 0;
            comboUse(p, SkillDefinition.RIP_AND_RUSH);
            for (int i = 0; i < 4; i++) { movementStep(p); }
            // The owner moved six blocks, so inspect all received packets rather than the fixture's five-block filter.
            var packets = f.particlePackets.get(p.getUUID());
            check(packets.stream().filter(packet -> packet.getParticle().getType() == CombatParticles.RUSH_TRAIL.get()).count() == 4, "Rush emits actual dash segments");
            check(packets.stream().filter(packet -> packet.getParticle().getType() == CombatParticles.COMBO_CONTACT.get()).count() == 1, "Dash contacts once, matching native one-attempt targeting");
            front.setPos(p.position().add(0, 0, 2)); front.invulnerableTime = 0; front.setHealth(.25f);
            rear.discard();
            var dry = f.target(p.position().add(0, 0, -2));
            immune.setPos(p.position().add(2, 0, 0)); Bleeding.apply(immune, p, 1, 100);
            packets.clear();
            comboUse(p, SkillDefinition.RIP_AND_RUSH);
            check(f.particles(p, CombatParticles.RUSH_FINISHER.get()).size() == 1, "Distinct circular finisher release");
            check(f.particles(p, CombatParticles.COMBO_CONTACT.get()).size() == 2, "Bleeding and dry targets both receive accepted-hit cues");
            check(f.particles(p, CombatParticles.COMBO_CONSUME.get()).size() == 1, "Only qualifying bleed hit receives consumption cue, including lethal hit");
            check(!front.isAlive() && dry.getHealth() < dry.getMaxHealth() && Bleeding.count(immune) == 1, "Lethal reward, dry damage and prevented bleed remain distinct");
            check(p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifiers().stream()
                    .anyMatch(modifier -> modifier.id().getPath().equals("rush_speed")), "Consumption cue agrees with actual reward");
            movementStep(p);
            check(f.particles(p, CombatParticles.RUSH_TRAIL.get()).isEmpty(), "Finisher never resumes trail");
            ClassResources.set(p, PlayerClass.REAVER, 0);
            check(key(Skills.activate(p, SkillDefinition.RIP_AND_RUSH), "cooldown"), "Completed combo rejects repeat");
            check(f.particles(p, CombatParticles.RUSH_FINISHER.get()).size() == 1, "Rejected repeat emits nothing");
            helper.succeed();
        }
    }

    private static void comboUse(ServerPlayer player, SkillDefinition skill) {
        clearThrottle(player);
        check(key(Skills.activate(player, skill), "used"), "Skill activation accepted: " + skill.id());
    }

    private static void clearThrottle(ServerPlayer player) {
        try {
            var field = player.getData(RpgData.SKILL_RUNTIME).getClass().getDeclaredField("nextActivation");
            field.setAccessible(true); field.setLong(player.getData(RpgData.SKILL_RUNTIME), 0);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    static void disengageFeedback(GameTestHelper helper) {
        try (var f = new Fixture(helper, 180)) {
            var player = f.player(PlayerClass.STALKER, SkillDefinition.DISENGAGE);
            ClassResources.set(player, PlayerClass.STALKER, 0);
            check(key(Skills.activate(player, SkillDefinition.DISENGAGE), "resource"), "Unpaid leap rejected");
            check(f.particles(player, CombatParticles.DISENGAGE_DEPARTURE.get()).isEmpty(), "Rejection has no departure");
            ClassResources.set(player, PlayerClass.STALKER, 100);
            var start = player.position();
            check(key(Skills.activate(player, SkillDefinition.DISENGAGE), "used"), "Leap accepted");
            check(f.particles(player, CombatParticles.DISENGAGE_DEPARTURE.get()).size() == 1, "One committed departure");
            check(f.particles(player, CombatParticles.DISENGAGE_ARRIVAL.get()).isEmpty(), "No predicted arrival");
            player.setYRot(90);
            for (int i = 0; i < 6; i++) { movementStep(player); }
            var trail = f.particles(player, CombatParticles.DISENGAGE_TRAIL.get());
            check(trail.size() == 6, "One packet per actual unobstructed step");
            var cursor = start;
            for (var packet : trail) {
                near(packet.getX(), cursor.x, "Trail starts on the actual segment X");
                near(packet.getY(), cursor.y, "Trail starts on the actual arc Y");
                near(packet.getZ(), cursor.z, "Trail starts on the actual segment Z");
                check(packet.getCount() == 0 && !packet.isOverrideLimiter() && !packet.alwaysShow(), "Native limited exact-segment packet");
                cursor = cursor.add(packet.getXDist(), packet.getYDist(), packet.getZDist());
            }
            near(cursor.distanceTo(player.position()), 0, "Trail ends at the actual endpoint");
            var arrival = f.particles(player, CombatParticles.DISENGAGE_ARRIVAL.get());
            check(arrival.size() == 1, "One arrival after completion");
            near(arrival.getFirst().getZ(), player.getZ(), "Arrival uses actual endpoint");
            near(arrival.getFirst().getXDist(), 0, "Arrival retains departure heading after turning");
            movementStep(player);
            check(f.particles(player, CombatParticles.DISENGAGE_TRAIL.get()).size() == 6, "No continuing trail after travel");

            var interrupted = f.player(PlayerClass.STALKER, SkillDefinition.DISENGAGE);
            check(key(Skills.activate(interrupted, SkillDefinition.DISENGAGE), "used"), "Second leap accepted");
            movementStep(interrupted);
            dev.covenant.rpg.skills.Rooted.apply(interrupted, 60);
            movementStep(interrupted);
            check(f.particles(interrupted, CombatParticles.DISENGAGE_TRAIL.get()).size() == 1, "Root stops further trail emission");
            check(f.particles(interrupted, CombatParticles.DISENGAGE_ARRIVAL.get()).isEmpty(), "Root does not claim completion");
            for (var reason : SkillLifecycle.Reason.values()) {
                var cancelled = f.player(PlayerClass.STALKER, SkillDefinition.DISENGAGE);
                check(key(Skills.activate(cancelled, SkillDefinition.DISENGAGE), "used"), "Lifecycle fixture leap accepted");
                SkillLifecycle.cancelPlayer(cancelled, reason);
                movementStep(cancelled);
                check(f.particles(cancelled, CombatParticles.DISENGAGE_TRAIL.get()).isEmpty()
                        && f.particles(cancelled, CombatParticles.DISENGAGE_ARRIVAL.get()).isEmpty(), "Quiet cancellation: " + reason);
            }

            var clipped = f.player(PlayerClass.STALKER, SkillDefinition.DISENGAGE);
            var wall = BlockPos.containing(clipped.position().add(0, 0, -1));
            for (int i = 0; i < 3; i++) { var pos = wall.above(i); f.blocks.add(pos); helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()); }
            check(key(Skills.activate(clipped, SkillDefinition.DISENGAGE), "used"), "Wall-clipped leap accepted");
            for (int i = 0; i < 6; i++) { movementStep(clipped); }
            for (var packet : f.particles(clipped, CombatParticles.DISENGAGE_TRAIL.get())) {
                check(packet.getZ() + packet.getZDist() >= wall.getZ() + 1.29, "Trail never extends through wall");
            }
            check(f.particles(clipped, CombatParticles.DISENGAGE_ARRIVAL.get()).size() == 1, "Clipped travel ends once at actual position");
            helper.succeed();
        }
    }

    static void teleportFeedback(GameTestHelper helper) {
        try (var f = new Fixture(helper, 195)) {
            var player = f.player(PlayerClass.WEAVER, SkillDefinition.TELEPORT);
            var start = player.position();
            // A low ceiling clips upward aim to a horizontal remainder; only the actual endpoints are shown.
            var ceiling = BlockPos.containing(start.add(0, 2, 0));
            f.blocks.add(ceiling); helper.getLevel().setBlockAndUpdate(ceiling, Blocks.STONE.defaultBlockState());
            player.setXRot(-45);
            check(key(Skills.activate(player, SkillDefinition.TELEPORT), "used"), "Ceiling-clipped blink accepted");
            var packets = f.particlePackets.get(player.getUUID());
            var departures = packets.stream().filter(p -> p.getParticle().getType() == CombatParticles.TELEPORT_DEPARTURE.get()).toList();
            var arrivals = packets.stream().filter(p -> p.getParticle().getType() == CombatParticles.TELEPORT_ARRIVAL.get()).toList();
            check(departures.size() == 1 && arrivals.size() == 1, "One departure and one arrival");
            near(departures.getFirst().getY(), start.y, "Departure at pre-blink position");
            near(departures.getFirst().getZ(), start.z, "Departure retains original Z");
            near(arrivals.getFirst().getY(), player.getY(), "Arrival follows clipped height");
            near(arrivals.getFirst().getZ(), player.getZ(), "Arrival follows confirmed destination");
            check(player.getY() < start.y + .3, "Fixture actually clips pitch");
            check(packets.stream().noneMatch(p -> p.getParticle().getType() == net.minecraft.core.particles.ParticleTypes.PORTAL), "No old portal cloud duplicates endpoints");
            check(!arrivals.getFirst().alwaysShow() && !arrivals.getFirst().isOverrideLimiter(), "Blink respects particle limiting");
            check(key(Skills.activate(player, SkillDefinition.TELEPORT), "cooldown"), "Repeated blink rejected");
            check(packets.stream().filter(p -> p.getParticle().getType() == CombatParticles.TELEPORT_ARRIVAL.get()).count() == 1, "Rejection cannot replay arrival");

            var blocked = f.player(PlayerClass.WEAVER, SkillDefinition.TELEPORT);
            var wall = BlockPos.containing(blocked.position().add(0, 0, 1));
            for (int i = 0; i < 2; i++) { var pos = wall.above(i); f.blocks.add(pos); helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()); }
            blocked.setPos(blocked.getX(), blocked.getY(), wall.getZ() - blocked.getBbWidth() / 2.0);
            check(key(Skills.activate(blocked, SkillDefinition.TELEPORT), "destination"), "Touching wall rejects blink");
            check(f.particles(blocked, CombatParticles.TELEPORT_DEPARTURE.get()).isEmpty()
                    && f.particles(blocked, CombatParticles.TELEPORT_ARRIVAL.get()).isEmpty(), "Invalid destination has no endpoint cues");
            near(ClassResources.current(blocked, PlayerClass.WEAVER), 100, "Invalid blink spends nothing");
            helper.succeed();
        }
    }

    private static void movementStep(ServerPlayer player) {
        try {
            var field = player.getData(RpgData.SKILL_RUNTIME).getClass().getDeclaredField("movement"); field.setAccessible(true);
            var movement = field.get(player.getData(RpgData.SKILL_RUNTIME));
            if (movement != null) {
                var lastTick = movement.getClass().getDeclaredField("lastTick"); lastTick.setAccessible(true); lastTick.setLong(movement, Long.MIN_VALUE);
            }
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
    }

    static void volley(GameTestHelper helper) {
        try (var f = new Fixture(helper, 180)) {
            var player = f.player(PlayerClass.STALKER, SkillDefinition.MULTISHOT);
            check(key(Skills.activate(player, SkillDefinition.MULTISHOT), "bow"), "Missing bow rejects activation");
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            check(key(Skills.activate(player, SkillDefinition.MULTISHOT), "arrow"), "Missing ammunition rejects activation");
            player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW, 20));
            ClassResources.set(player, PlayerClass.STALKER, 0);
            check(key(Skills.activate(player, SkillDefinition.MULTISHOT), "resource"), "Insufficient Focus rejects activation");
            check(f.soundsAt(player) == 0, "Rejected requests emit no skill sounds");
            ClassResources.set(player, PlayerClass.STALKER, 100);
            check(key(Skills.activate(player, SkillDefinition.MULTISHOT), "used"), "Accepted volley");
            var arrows = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox().inflate(4),
                    arrow -> arrow.getOwner() == player);
            f.entities.addAll(arrows);
            check(arrows.size() == 5, "Presentation preserves the five native arrows");
            check(arrows.stream().allMatch(arrow -> arrow.getData(RpgData.SKILL_SHOT_STYLE) == 3),
                    "All volley arrows carry the lower-density cosmetic style");
            var reporter = new ProblemReporter.Collector();
            var output = TagValueOutput.createWithContext(reporter, player.registryAccess());
            var original = arrows.getFirst(); original.saveWithoutId(output);
            var saved = output.buildResult(); saved.remove("UUID");
            var loaded = (AbstractArrow) original.getType().create(helper.getLevel(), EntitySpawnReason.COMMAND);
            f.entities.add(loaded);
            loaded.load(TagValueInput.create(reporter, player.registryAccess(), saved));
            check(reporter.isEmpty() && !loaded.hasData(RpgData.SKILL_SHOT) && !loaded.hasData(RpgData.SKILL_SHOT_STYLE),
                    "Native serialization omits both cosmetic arrow attachments");
            check(ProjectileCombat.isSkill(loaded), "Cosmetic style does not replace saved skill provenance");
            check(f.soundsAt(player) == 1, "One release cue per volley, not one per arrow");
            check(player.getOffhandItem().getCount() == 19, "Volley still consumes one arrow");
            check(player.getMainHandItem().getDamageValue() == 1, "Volley still costs one bow durability");
            check(key(Skills.activate(player, SkillDefinition.MULTISHOT), "cooldown"), "Immediate repeat remains rejected");
            check(f.soundsAt(player) == 1, "Rejected repeat cannot replay release audio");
            helper.succeed();
        }
    }

    static void melee(GameTestHelper helper) {
        try (var f = new Fixture(helper, 225)) {
            var whiff = f.player(PlayerClass.REAVER, SkillDefinition.CLEAVE);
            whiff.setYRot(90);
            check(key(Skills.activate(whiff, SkillDefinition.CLEAVE), "used"), "Whiff is still an accepted strike");
            check(f.soundsAt(whiff) == 1, "Whiff has a swing cue but no damage confirmation");
            var crescent = f.particles(whiff, CombatParticles.CLEAVE.get());
            check(crescent.size() == 1 && crescent.getFirst().getCount() == 0, "Whiff emits one directional native particle");
            near(crescent.getFirst().getXDist(), Math.PI / 2, "Crescent carries accepted yaw in radians");
            near(crescent.getFirst().getYDist(), SkillDefinition.CLEAVE.range(), "Crescent carries actual attack range");
            check(!crescent.getFirst().isOverrideLimiter() && !crescent.getFirst().alwaysShow(), "Decoration respects native particle settings");
            check(f.particles(whiff, CombatParticles.CONTACT.get()).isEmpty(), "Whiff has no accepted-hit mark");

            var striker = f.player(PlayerClass.REAVER, SkillDefinition.CLEAVE);
            var first = f.target(striker.position().add(-.5, 0, 2));
            var second = f.target(striker.position().add(.5, 0, 2));
            float firstHealth = first.getHealth(), secondHealth = second.getHealth();
            check(key(Skills.activate(striker, SkillDefinition.CLEAVE), "used"), "Area strike accepted");
            check(first.getHealth() < firstHealth && second.getHealth() < secondHealth, "Both in-arc targets receive damage");
            check(f.soundsAt(striker) == 2, "Multi-target strike adds one aggregate damage confirmation");
            check(f.particles(striker, CombatParticles.CONTACT.get()).size() == 2, "Each damaged target gets one small contact mark");

            var blocked = f.player(PlayerClass.REAVER, SkillDefinition.CLEAVE);
            var immune = f.target(blocked.position().add(0, 0, 2)); immune.setInvulnerable(true);
            float immuneHealth = immune.getHealth();
            check(key(Skills.activate(blocked, SkillDefinition.CLEAVE), "used"), "Protected target does not reject the swing");
            near(immune.getHealth(), immuneHealth, "Protected target receives no damage");
            check(f.soundsAt(blocked) == 1, "Fully prevented damage produces no success cue");
            check(f.particles(blocked, CombatParticles.CONTACT.get()).isEmpty(), "Prevented damage has no accepted-hit mark");
            helper.succeed();
        }
    }

    static void casting(GameTestHelper helper) {
        var f = new Fixture(helper, 195);
        try {
            var completed = f.player(PlayerClass.WEAVER, SkillDefinition.ARCANE_BOLT);
            var cancelled = f.player(PlayerClass.WEAVER, SkillDefinition.ARCANE_BOLT);
            check(key(Skills.activate(completed, SkillDefinition.ARCANE_BOLT), "started"), "First cast accepted");
            check(key(Skills.activate(cancelled, SkillDefinition.ARCANE_BOLT), "started"), "Second cast accepted");
            check(f.soundsAt(completed) == 1 && f.soundsAt(cancelled) == 1, "Each accepted cast emits one start cue");
            check(key(Skills.activate(completed, SkillDefinition.ARCANE_BOLT), "casting"), "Overlapping cast rejected");
            check(f.soundsAt(completed) == 1, "Overlapping request does not replay start cue");
            near(ClassResources.current(completed, PlayerClass.WEAVER), 100, "Start cue does not spend Mana early");
            SkillLifecycle.cancelPlayer(cancelled, SkillLifecycle.Reason.DIMENSION_CHANGE);
            check(!Skills.isCasting(cancelled), "Lifecycle cancellation clears pending cast");
            var aimed = f.player(PlayerClass.STALKER, SkillDefinition.AIMED_SHOT);
            var aimedCancelled = f.player(PlayerClass.STALKER, SkillDefinition.AIMED_SHOT);
            var interrupted = f.player(PlayerClass.STALKER, SkillDefinition.AIMED_SHOT);
            for (var archer : List.of(aimed, aimedCancelled, interrupted)) {
                archer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                archer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW, 2));
                check(key(Skills.activate(archer, SkillDefinition.AIMED_SHOT), "started"), "Aimed Shot begins its real wind-up");
            }
            SkillLifecycle.cancelPlayer(aimedCancelled, SkillLifecycle.Reason.DIMENSION_CHANGE);
            interrupted.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            helper.onEachTick(f::tickPlayers);
            helper.runAtTickTime(25, () -> {
                try (f) {
                    check(!Skills.isCasting(completed), "Normal cast completes");
                    check(f.soundsAt(completed) == 2, "Completion emits one release after its single start cue");
                    check(f.soundsAt(cancelled) == 1, "Cancelled cast never emits its release cue");
                    near(ClassResources.current(completed, PlayerClass.WEAVER), 80, "Completed cast spends normal Mana");
                    near(ClassResources.current(cancelled, PlayerClass.WEAVER), 100, "Cancelled cast preserves Mana");
                    check(Skills.cooldownRemaining(completed, SkillDefinition.ARCANE_BOLT) > 0, "Completion retains cooldown");
                    check(Skills.cooldownRemaining(cancelled, SkillDefinition.ARCANE_BOLT) == 0, "Cancellation commits no cooldown");
                    check(f.particles(cancelled, CombatParticles.ARCANE_MOTE.get()).isEmpty(), "Cancelled cast never emits later charge motes");
                    check(!f.particles(aimed, CombatParticles.AIMED_GATHER.get()).isEmpty(), "Aimed Shot has gathering cues during its actual wind-up");
                    check(f.particles(aimedCancelled, CombatParticles.AIMED_GATHER.get()).isEmpty(), "Cancelled Aimed Shot has no later gathering cues");
                    check(f.soundsAt(aimed) == 2 && f.soundsAt(aimedCancelled) == 1, "Only completed Aimed Shot emits release audio");
                    check(aimed.getOffhandItem().getCount() == 1 && aimedCancelled.getOffhandItem().getCount() == 2,
                            "Presentation preserves completed/cancelled ammunition spending");
                    check(f.messages.get(completed.getUUID()).stream().anyMatch(packet -> packet.overlay() && key(packet.content(), "used")),
                            "Real cast completion uses native action-bar feedback");
                    var notices = f.messages.get(interrupted.getUUID());
                    check(notices.stream().anyMatch(packet -> packet.overlay() && key(packet.content(), "cancelled")),
                            "Equipment cancellation has authoritative action-bar feedback");
                    check(notices.stream().noneMatch(packet -> key(packet.content(), "used")), "Cancelled cast has no success message");
                    near(ClassResources.current(interrupted, PlayerClass.STALKER), 100, "Equipment interruption spends no Focus");
                    check(interrupted.getOffhandItem().getCount() == 2, "Equipment interruption spends no ammunition");
                    helper.succeed();
                }
            });
        } catch (RuntimeException error) { f.close(); throw error; }
    }

    static void impacts(GameTestHelper helper) {
        var f = new Fixture(helper, 210);
        try {
            for (var skill : List.of(SkillDefinition.ARCANE_BOLT, SkillDefinition.FIRE_BOLT, SkillDefinition.ICE_BOLT)) {
                var player = f.player(PlayerClass.WEAVER, skill);
                player.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
                var wall = BlockPos.containing(player.getEyePosition().add(0, 0, 2));
                f.blocks.add(wall);
                helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
                check(key(Skills.activate(player, skill), "used"), "Instant bolt accepted: " + skill.id());
                check(f.soundsAt(player) == 1, "Instant bolt has one release without a nonexistent wind-up");
                var bolts = helper.getLevel().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(3),
                        bolt -> bolt.getOwner() == player);
                check(bolts.size() == 1, "Accepted bolt spawns one projectile");
                var bolt = bolts.getFirst(); f.entities.add(bolt);
                for (int i = 0; i < 6 && !bolt.isRemoved(); i++) { bolt.tick(); }
                check(bolt.isRemoved(), "Native block collision removes bolt: " + skill.id());
                check(f.soundsAt(player) == 2, "Terminal collision emits exactly one impact cue: " + skill.id());
                var collision = skill == SkillDefinition.ARCANE_BOLT ? CombatParticles.ARCANE_COLLISION.get()
                        : skill == SkillDefinition.FIRE_BOLT ? CombatParticles.FIRE_COLLISION.get() : CombatParticles.ICE_COLLISION.get();
                check(f.particles(player, collision).size() == 1, "Wall terminates the bolt with one element-specific collision sequence");
                check(f.particles(player, CombatParticles.CONTACT.get()).isEmpty(), "Wall collision never claims accepted damage");
                near(ClassResources.current(player, PlayerClass.WEAVER), 80, "Feedback retains normal spell cost");
            }
            helper.runAtTickTime(5, () -> {
                try (f) {
                    for (var player : f.players) {
                        check(f.soundsAt(player) == 2, "Removed projectile cannot repeat its impact on later server ticks");
                    }
                    helper.succeed();
                }
            });
        } catch (RuntimeException error) { f.close(); throw error; }
    }

    static void projectileContacts(GameTestHelper helper) {
        try (var f = new Fixture(helper, 240)) {
            var player = f.player(PlayerClass.WEAVER, SkillDefinition.ARCANE_BOLT);
            var bolt = new ArcaneBolt(Skills.ARCANE_BOLT.get(), helper.getLevel());
            bolt.setOwner(player);
            var target = f.target(player.position().add(0, 0, 2));
            var source = player.damageSources().indirectMagic(bolt, player);
            target.setInvulnerable(true);
            target.hurtServer(helper.getLevel(), source, 2);
            check(f.particles(player, CombatParticles.CONTACT.get()).isEmpty(), "Rejected projectile damage emits no contact mark");
            target.setInvulnerable(false);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(5);
            target.setAbsorptionAmount(5);
            float health = target.getHealth();
            target.hurtServer(helper.getLevel(), source, 2);
            near(target.getHealth(), health, "Absorption-only hit preserves health");
            check(f.particles(player, CombatParticles.CONTACT.get()).size() == 1, "Absorption-only accepted damage emits one mark");
            check(f.particles(player, CombatParticles.ARCANE_COLLISION.get()).isEmpty(), "Damage event alone never invents a collision");

            var archer = f.player(PlayerClass.STALKER, SkillDefinition.PIERCING_SHOT);
            archer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            archer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW, 2));
            check(key(Skills.activate(archer, SkillDefinition.PIERCING_SHOT), "used"), "Piercing arrow accepted");
            var arrow = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, archer.getBoundingBox().inflate(4),
                    entity -> entity.getOwner() == archer).getFirst();
            var victim = f.target(archer.position().add(0, 0, 2));
            victim.hurtServer(helper.getLevel(), archer.damageSources().arrow(arrow, archer), 2);
            check(f.particles(archer, CombatParticles.CONTACT.get()).size() == 1, "Marked piercing arrow confirms accepted damage");
            arrow.removeData(RpgData.SKILL_SHOT);
            var ordinaryVictim = f.target(archer.position().add(1, 0, 2));
            ordinaryVictim.hurtServer(helper.getLevel(), archer.damageSources().arrow(arrow, archer), 2);
            check(f.particles(archer, CombatParticles.CONTACT.get()).size() == 1, "Unmarked ordinary arrow gets no Covenant contact mark");
            helper.succeed();
        }
    }

    static void elementalContacts(GameTestHelper helper) {
        try (var f = new Fixture(helper, 245)) {
            for (var skill : List.of(SkillDefinition.FIRE_BOLT, SkillDefinition.ICE_BOLT, SkillDefinition.LIGHTNING_BOLT)) {
                var player = f.player(PlayerClass.WEAVER, skill);
                var target = f.target(player.position().add(0, 0, 2));
                var bolt = new ElementalBolt(skill == SkillDefinition.FIRE_BOLT ? Skills.FIRE_BOLT.get() : Skills.ICE_BOLT.get(), helper.getLevel());
                bolt.setOwner(player);
                Entity direct = skill == SkillDefinition.LIGHTNING_BOLT ? player : bolt;
                target.setInvulnerable(true);
                ElementalBolts.hit(helper.getLevel(), player, direct, target, skill, 60);
                check(f.particles(player, CombatParticles.CONTACT.get()).isEmpty(), "Immune elemental hit has no damage mark: " + skill.id());
                target.setInvulnerable(false);
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(20);
                target.setAbsorptionAmount(20);
                float health = target.getHealth();
                ElementalBolts.hit(helper.getLevel(), player, direct, target, skill, 60);
                near(target.getHealth(), health, "Elemental absorption-only hit preserves health");
                check(f.particles(player, CombatParticles.CONTACT.get()).size() == 1, "Each accepted elemental hit has one damage mark");
            }
            helper.succeed();
        }
    }

    static void lightningLinks(GameTestHelper helper) {
        try (var f = new Fixture(helper, 250)) {
            var player = f.player(PlayerClass.WEAVER, SkillDefinition.LIGHTNING_BOLT);
            FixtureSkills.learn(player, SkillDefinition.SHOCK_AURA.id(), true);
            player.setData(RpgData.SKILL_LOADOUT, player.getData(RpgData.SKILL_LOADOUT).withBinding(0, SkillDefinition.SHOCK_AURA.id()));
            check(key(Skills.activate(player, SkillDefinition.SHOCK_AURA), "used"), "Existing Shock activation accepted");
            player.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
            var one = f.target(player.position().add(0,0,4), EntityType.HUSK);
            var two = f.target(player.position().add(0,0,7), EntityType.HUSK);
            var three = f.target(player.position().add(0,0,10), EntityType.HUSK);
            var four = f.target(player.position().add(0,0,13), EntityType.HUSK);
            for (var target : List.of(one,two,three,four)) {
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
                target.setHealth(200);
            }
            reset(player, SkillDefinition.LIGHTNING_BOLT);
            check(key(Skills.activate(player, SkillDefinition.LIGHTNING_BOLT), "used"), "Lightning release accepted");
            var beams = f.beams.get(player.getUUID());
            check(beams.size() == 3, "Exactly one main ray and two real chain links");
            check(beams.getFirst().start().equals(player.getEyePosition()) && one.getBoundingBox().inflate(.01).contains(beams.getFirst().end()),
                    "Main visual ends at the actual first entity collision");
            check(beams.get(1).start().equals(one.getEyePosition()) && beams.get(1).end().equals(two.getEyePosition()), "First link joins real targets");
            check(beams.get(2).start().equals(two.getEyePosition()) && beams.get(2).end().equals(three.getEyePosition()), "Second link joins real targets");
            near(four.getHealth(),200,"No cosmetic branch creates another damaged target");
            check(f.particlePackets.get(player.getUUID()).stream().filter(packet -> packet.getParticle().getType() == CombatParticles.CONTACT.get()).count() == 3,
                    "Main ray and accepted chain hits each get one contact mark");
            check(beams.stream().allMatch(beam -> beam.dimension().equals(helper.getLevel().dimension().identifier())), "Links retain their authoritative dimension");

            var wall=BlockPos.containing(player.getX(),player.getEyeY(),player.getZ()+5);
            f.blocks.add(wall); helper.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
            one.invulnerableTime=0; beams.clear(); reset(player,SkillDefinition.LIGHTNING_BOLT);
            Skills.activate(player,SkillDefinition.LIGHTNING_BOLT);
            check(beams.size()==1,"Cover prevents both damage and invented chain links");
            for (var target : List.of(one,two,three,four)) { target.discard(); }
            beams.clear(); f.particlePackets.get(player.getUUID()).clear(); player.setYRot(180);
            reset(player,SkillDefinition.LIGHTNING_BOLT); Skills.activate(player,SkillDefinition.LIGHTNING_BOLT);
            check(beams.size()==1,"Empty aim still has exactly one main visual");
            near(beams.getFirst().start().distanceTo(beams.getFirst().end()),SkillDefinition.LIGHTNING_BOLT.range(),"Range miss stops at actual range");
            check(f.particles(player,CombatParticles.LIGHTNING_COLLISION.get()).isEmpty() && f.particles(player,CombatParticles.CONTACT.get()).isEmpty(),
                    "Range miss invents neither collision nor damage");
            helper.succeed();
        }
    }

    static void bowContacts(GameTestHelper helper) {
        try (var f = new Fixture(helper,255)) {
            for (var skill : List.of(SkillDefinition.AIMED_SHOT,SkillDefinition.ARCANE_ARROW,SkillDefinition.MULTISHOT)) {
                var player=f.player(PlayerClass.STALKER,skill);
                player.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
                player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
                player.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.ARROW,2));
                var target=f.target(player.position().add(0,0,2),EntityType.HUSK);
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
                check(key(Skills.activate(player,skill),"used"),"Bow release accepted");
                var arrows=helper.getLevel().getEntitiesOfClass(AbstractArrow.class,player.getBoundingBox().inflate(3),arrow -> arrow.getOwner()==player);
                for (var arrow:arrows) { for (int i=0;i<4&&!arrow.isRemoved();i++) { arrow.tick(); } }
                var mark=skill==SkillDefinition.ARCANE_ARROW?CombatParticles.ARCANE_CONTACT.get():CombatParticles.CONTACT.get();
                check(f.particles(player,mark).size()==1,"Native collision retains style through one accepted hit: "+skill.id());
                check(target.getHealth()<200,"Contact mark corresponds to actual native damage");
                if (skill==SkillDefinition.MULTISHOT) { check(arrows.size()==5,"Five-arrow fan is retained"); }
            }
            helper.succeed();
        }
    }

    static void bleedFeedback(GameTestHelper helper) {
        try (var f=new Fixture(helper,260)) {
            var player=f.player(PlayerClass.REAVER,SkillDefinition.REND);
            var target=f.target(player.position().add(0,0,2),EntityType.HUSK);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
            target.setInvulnerable(true); Skills.activate(player,SkillDefinition.REND);
            check(f.particles(player,CombatParticles.REND_CONTACT.get()).isEmpty(),"Prevented Rend never marks success");
            target.setInvulnerable(false); reset(player,SkillDefinition.REND); Skills.activate(player,SkillDefinition.REND);
            check(f.particles(player,CombatParticles.REND_CONTACT.get()).size()==1,"Accepted Rend gets one single-target slash");
            check(f.particles(player,CombatParticles.CLEAVE.get()).isEmpty(),"Rend never implies a broad sweep");
            Bleeding.apply(target,player,1,100); Bleeding.apply(target,player,1,100);
            check(Bleeding.count(target)==3,"Shared bleed cap unchanged");
            for(int i=0;i<20;i++){NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(target));}
            check(f.particles(player,CombatParticles.BLEED_TICK.get()).size()==1,"Three accepted stacks share one sparse pulse cue");
            target.setInvulnerable(true);
            for(int i=0;i<20;i++){NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(target));}
            check(f.particles(player,CombatParticles.BLEED_TICK.get()).size()==1,"Prevented pulse emits no success mark");
            helper.succeed();
        }
    }

    private static void reset(ServerPlayer player,SkillDefinition skill) {
        player.removeData(RpgData.SKILL_RUNTIME);
        player.setData(RpgData.SKILLS,player.getData(RpgData.SKILLS).withCooldown(skill.id(),0));
        ClassResources.set(player,PlayerProfiles.getSelectedClass(player).orElseThrow(),100);
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final Vec3 origin;
        final List<ServerPlayer> players = new ArrayList<>();
        final List<Entity> entities = new ArrayList<>();
        final List<BlockPos> blocks = new ArrayList<>();
        final List<Vec3> sounds = new ArrayList<>();
        final net.minecraft.server.level.TicketType feedbackTicket = new net.minecraft.server.level.TicketType(
                net.minecraft.server.level.TicketType.NO_TIMEOUT, net.minecraft.server.level.TicketType.FLAG_LOADING
                | net.minecraft.server.level.TicketType.FLAG_SIMULATION | net.minecraft.server.level.TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
        final java.util.Set<net.minecraft.world.level.ChunkPos> feedbackChunks = new java.util.HashSet<>();
        final java.util.Map<UUID, List<net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket>> particlePackets = new java.util.HashMap<>();
        final java.util.Map<UUID, List<LightningVisual>> beams = new java.util.HashMap<>();
        final java.util.Map<UUID, List<net.minecraft.network.protocol.game.ClientboundSystemChatPacket>> messages = new java.util.HashMap<>();
        final Consumer<PlayLevelSoundEvent.AtPosition> listener = this::sound;
        boolean closed;

        Fixture(GameTestHelper helper, int height) {
            this.helper = helper;
            var pos = helper.absolutePos(new BlockPos(2, height, 2));
            origin = new Vec3(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            NeoForge.EVENT_BUS.addListener(listener);
        }

        ServerPlayer player(PlayerClass playerClass, SkillDefinition skill) {
            var identity = UUID.randomUUID();
            var cookie = CommonListenerCookie.createInitial(new GameProfile(identity, "fb" + identity.toString().substring(0, 8)), false);
            var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var packets = new ArrayList<net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket>();
            particlePackets.put(player.getUUID(), packets);
            var links=new ArrayList<LightningVisual>(); beams.put(player.getUUID(),links);
            var notices = new ArrayList<net.minecraft.network.protocol.game.ClientboundSystemChatPacket>(); messages.put(player.getUUID(), notices);
            var connection = new Connection(PacketFlow.SERVERBOUND) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet, io.netty.channel.ChannelFutureListener listener, boolean flush) {
                    if (packet instanceof net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket particles) { packets.add(particles); }
                    if (packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket payload && payload.payload() instanceof LightningVisual beam) { links.add(beam); }
                    if (packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket notice) { notices.add(notice); }
                    super.send(packet, listener, flush);
                }
                @Override public boolean isMemoryConnection() { return true; }
            };
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
            helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            players.add(player);
            try {
                var pending = player.connection.getClass().getDeclaredField("awaitingTeleport");
                pending.setAccessible(true);
                player.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(pending.getInt(player.connection)));
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            player.setPos(origin.add((players.size() - 1) * 12, 0, 0));
            player.connection.resetPosition();
            player.setGameMode(GameType.SURVIVAL);
            player.connection.markClientLoaded();
            player.setNoGravity(true); player.setYRot(0); player.setXRot(0);
            PlayerProfiles.selectClass(player, playerClass);
            FixtureSkills.learn(player, skill.id(), true);
            player.getAttribute(RpgStats.MANA_REGENERATION).setBaseValue(0);
            ClassResources.set(player, playerClass, 100);
            return player;
        }

        void loadFeedbackArea() {
            // The one-block template can end at a chunk edge; query fixtures need real entity-ticking neighbors.
            for (int x = net.minecraft.util.Mth.floor(origin.x - 12) >> 4; x <= net.minecraft.util.Mth.floor(origin.x + 12) >> 4; x++) {
                for (int z = net.minecraft.util.Mth.floor(origin.z - 12) >> 4; z <= net.minecraft.util.Mth.floor(origin.z + 12) >> 4; z++) {
                    var chunk = new net.minecraft.world.level.ChunkPos(x, z);
                    if (feedbackChunks.add(chunk)) { helper.getLevel().getChunkSource().addTicketWithRadius(feedbackTicket, chunk, 2); }
                }
            }
        }

        void assertFeedbackReady() {
            for (var chunk : feedbackChunks) {
                helper.assertTrue(helper.getLevel().areEntitiesActuallyLoadedAndTicking(chunk), "Feedback fixture area must be entity-ticking");
            }
        }

        LivingEntity target(Vec3 position) {
            return target(position,EntityType.COW);
        }

        LivingEntity target(Vec3 position,EntityType<? extends net.minecraft.world.entity.Mob> type) {
            var target = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            target.setPos(position); target.setNoAi(true); target.setNoGravity(true);
            helper.getLevel().addFreshEntity(target); entities.add(target);
            return target;
        }

        private void sound(PlayLevelSoundEvent.AtPosition event) {
            if (event.getLevel() != helper.getLevel() || event.getSource() != SoundSource.PLAYERS || event.getSound() == null) { return; }
            if (players.stream().anyMatch(player -> event.getPosition().distanceToSqr(player.position()) < 25)) {
                check(event.getOriginalVolume() > 0 && event.getOriginalVolume() <= 1, "Skill sound uses restrained native volume");
                sounds.add(event.getPosition());
            }
        }

        long soundsAt(ServerPlayer player) { return sounds.stream().filter(pos -> pos.distanceToSqr(player.position()) < 25).count(); }

        List<net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket> particles(ServerPlayer player, net.minecraft.core.particles.ParticleType<?> type) {
            return particlePackets.get(player.getUUID()).stream().filter(packet -> packet.getParticle().getType() == type
                    && player.position().distanceToSqr(new Vec3(packet.getX(), packet.getY(), packet.getZ())) < 25).toList();
        }

        void tickPlayers() {
            if (closed) { return; }
            try { players.forEach(ServerPlayer::doTick); }
            catch (RuntimeException error) { close(); throw error; }
        }

        @Override public void close() {
            if (closed) { return; }
            closed = true;
            NeoForge.EVENT_BUS.unregister(listener);
            entities.forEach(Entity::discard);
            for (var player : players) {
                helper.getLevel().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(64),
                        projectile -> projectile.getOwner() == player).forEach(Entity::discard);
                SkillLifecycle.cancelPlayer(player, SkillLifecycle.Reason.LOGOUT);
                helper.getLevel().getServer().getPlayerList().remove(player);
            }
            blocks.forEach(pos -> helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
            feedbackChunks.forEach(chunk -> helper.getLevel().getChunkSource().removeTicketWithRadius(feedbackTicket, chunk, 2));
        }
    }

    private static boolean key(Component value, String suffix) {
        return value.getContents() instanceof TranslatableContents text && text.getKey().equals("commands.covenant.skill." + suffix);
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < .001, message + ": expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
}
