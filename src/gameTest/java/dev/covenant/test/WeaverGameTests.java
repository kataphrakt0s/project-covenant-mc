package dev.covenant.test;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.*;
import dev.covenant.rpg.stats.RpgStats;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real Survival players exercise paid casts, native damage/defenses, rolling time and native blink packets. */
final class WeaverGameTests {
    static void activation(GameTestHelper helper) {
        try (var f = new Fixture(helper, 260)) {
            var p = f.player();
            check(key(Skills.activate(p, SkillDefinition.TELEPORT), "unlearned"), "Unlearned Teleport rejected");
            f.learn(p, SkillDefinition.DISCHARGE); f.equip(p, SkillDefinition.THREEFOLD_SYSTEM, 0); f.learn(p, SkillDefinition.TELEPORT);
            check(key(Skills.activate(p, SkillDefinition.DISCHARGE), "charges"), "Empty discharge rejected");
            check(key(Skills.activate(p, SkillDefinition.THREEFOLD_SYSTEM), "reserves"), "Empty reserves rejected");
            near(f.mana(p), 100, "Empty activations spend nothing");
            Rooted.apply(p, 40);
            check(key(Skills.activate(p, SkillDefinition.TELEPORT), "rooted"), "Root blocks blink"); p.removeEffect(Rooted.EFFECT);
            ClassResources.set(p, PlayerClass.WEAVER, 24);
            check(key(Skills.activate(p, SkillDefinition.TELEPORT), "resource"), "Mana checked");
            ClassResources.set(p, PlayerClass.WEAVER, 100);
            p.getAttribute(RpgStats.RESOURCE_COST).setBaseValue(.5); p.getAttribute(RpgStats.COOLDOWN).setBaseValue(.5);
            f.use(p, SkillDefinition.TELEPORT);
            near(f.mana(p), 87.5, "Blink scales cost"); near(Skills.cooldownRemaining(p, SkillDefinition.TELEPORT), 100, "Blink scales cooldown");
            check(key(Skills.activate(p, SkillDefinition.TELEPORT), "cooldown"), "Saved cooldown enforced");
            PlayerProfiles.switchClass(p, PlayerClass.STALKER);
            check(key(Skills.activate(p, SkillDefinition.TELEPORT), "class"), "Class authority enforced");
            helper.succeed();
        }
    }

    static void dischargeCast(GameTestHelper helper) {
        var f = new Fixture(helper, 275); var p = f.player();
        p.getAttribute(RpgStats.MANA_REGENERATION).setBaseValue(0);
        f.learn(p, SkillDefinition.DISCHARGE); f.learn(p, SkillDefinition.FIRE_BOLT);
        check(key(Skills.activate(p, SkillDefinition.FIRE_BOLT), "started"), "Default bolt begins cast");
        near(f.mana(p), 100, "No cost until completion"); near(WeaverPower.charges(p, WeaverPower.Element.FIRE), 0, "No charge before completion");
        helper.onEachTick(f::tick);
        helper.runAtTickTime(12, () -> {
            near(WeaverPower.charges(p, WeaverPower.Element.FIRE), 1, "Missed completed cast still gives one charge");
            check(key(Skills.activate(p, SkillDefinition.DISCHARGE), "started"), "Discharge winds up");
            near(WeaverPower.charges(p, WeaverPower.Element.FIRE), 1, "Charge retained during cast");
            SkillLifecycle.cancelPlayer(p, SkillLifecycle.Reason.DIMENSION_CHANGE);
            check(!Skills.isCasting(p), "Lifecycle cancels pending cast");
            near(Skills.cooldownRemaining(p, SkillDefinition.DISCHARGE), 0, "Cancelled discharge starts no cooldown");
        });
        helper.runAtTickTime(25, () -> {
            near(WeaverPower.charges(p, WeaverPower.Element.FIRE), 0, "Lifecycle discards charges");
            f.charge(p, 1, 0, 0); p.getAttribute(RpgStats.CAST_TIME).setBaseValue(1);
            ClassResources.set(p, PlayerClass.WEAVER, 100); f.use(p, SkillDefinition.DISCHARGE);
            near(f.mana(p), 100, "Discharge defers spending until completion");
        });
        helper.runAtTickTime(38, () -> {
            try {
                near(f.mana(p), 70, "Completed empty-area Discharge pays once");
                near(WeaverPower.charges(p, WeaverPower.Element.FIRE), 0, "Completed empty-area Discharge consumes bank");
                check(Skills.cooldownRemaining(p, SkillDefinition.DISCHARGE) > 230, "Completion starts captured cooldown");
                check(!p.getData(RpgData.WEAVER_DISPLAY).buff(), "No lingering buff display"); helper.succeed();
            } finally { f.close(); }
        });
    }

    static void dischargeMixed(GameTestHelper helper) {
        try (var f = new Fixture(helper, 240)) {
            var p = f.player(); f.charge(p, 7, 7, 7);
            for (var e : WeaverPower.Element.values()) { near(WeaverPower.charges(p, e), 5, "Independent cap"); }
            var a = f.target(0, 0, 1); var b = f.target(0, 0, 2);
            var chain1 = f.target(0, 0, 5); var chain2 = f.target(0, 0, 8.5); var extra = f.target(0, 0, 10);
            ClassResources.set(p, PlayerClass.WEAVER, 100); f.use(p, SkillDefinition.DISCHARGE);
            near(a.getHealth(), 200 - 15 - 4 * (1 - 1.0 / 3), "All three components and falloff explosion apply");
            near(b.getHealth(), 200 - 15 - 4 * (1 - 2.0 / 3), "Other area target hit once per component");
            near(chain1.getHealth(), 197.5, "First external arc deals half lightning contribution");
            near(chain2.getHealth(), 197.5, "Second arc has no further falloff"); near(extra.getHealth(), 200, "Two external targets maximum");
            check(Auras.frozen(a) && a.getData(RpgData.AURA_FROZEN), "Max ice shares native freeze controls and sync");
            check(a.getRemainingFireTicks() > 0, "Fire contribution ignites");
            check(!a.hasEffect(DischargeSlow.EFFECT), "Full ice uses freeze instead of new slow");
            for (var e : WeaverPower.Element.values()) { near(WeaverPower.charges(p, e), 0, "Consume all charges exactly once"); }
            near(f.mana(p), 70, "Thirty Mana paid"); near(Skills.cooldownRemaining(p, SkillDefinition.DISCHARGE), 240, "Twelve-second cooldown");
            FixtureSkills.learn(p, SkillDefinition.DISCHARGE.id(), false);
            check(!Auras.frozen(a), "Unlearning removes owned discharge freeze");
            helper.succeed();
        }
    }

    static void dischargeDefenses(GameTestHelper helper) {
        try (var f = new Fixture(helper, 225)) {
            var p = f.player(); f.charge(p, 0, 1, 0); var target = f.target(0, 0, 2);
            target.invulnerableTime = 20;
            target.getAttribute(Attributes.ARMOR).setBaseValue(30);
            f.use(p, SkillDefinition.DISCHARGE);
            near(target.getHealth(), 199, "Magic bypasses armor and previous hurt window");
            near(target.invulnerableTime, 20, "Discharge preserves prior hurt cooldown");
            check(target.hasEffect(DischargeSlow.EFFECT), "Partial ice chills");
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), target.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * .7, "Thirty percent slow");
            Chilled.apply(target, p, 100);
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), target.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * .7, "Chilled does not compound");
            f.charge(p, 0, 1, 0); target.setInvulnerable(true); f.use(p, SkillDefinition.DISCHARGE);
            near(target.getHealth(), 199, "Actual invulnerability respected");
            var protectedTarget = f.target(2, 0, 0);
            f.block(1, 0, 0, Blocks.STONE); f.block(1, 1, 0, Blocks.STONE); f.block(1, 2, 0, Blocks.STONE);
            f.charge(p, 0, 1, 0); f.use(p, SkillDefinition.DISCHARGE);
            near(protectedTarget.getHealth(), 200, "Solid cover blocks radial damage");
            helper.succeed();
        }
    }

    static void threefoldDamage(GameTestHelper helper) {
        try (var f = new Fixture(helper, 210)) {
            var p = f.player(); f.learn(p, SkillDefinition.THREEFOLD_SYSTEM); var target = f.target(0, 0, 2);
            f.hit(p, target, SkillDefinition.FIRE_BOLT, 6);
            near(WeaverPower.reserve(p, WeaverPower.Element.ICE), 0, "Unassigned passive banks nothing");
            f.equip(p, SkillDefinition.THREEFOLD_SYSTEM, 0);
            target.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20); target.setAbsorptionAmount(10);
            f.hit(p, target, SkillDefinition.FIRE_BOLT, 6);
            near(target.getAbsorptionAmount(), 4, "Native absorption hit");
            near(WeaverPower.reserve(p, WeaverPower.Element.ICE), 1.5, "Absorption counts once");
            near(WeaverPower.reserve(p, WeaverPower.Element.FIRE), 0, "No same-element banking");
            f.hit(p, target, SkillDefinition.FIRE_BOLT, 1000);
            near(WeaverPower.reserve(p, WeaverPower.Element.ICE), 6, "Storage cap bounds overkill and area potential");
            ClassResources.set(p, PlayerClass.WEAVER, 100); f.use(p, SkillDefinition.THREEFOLD_SYSTEM);
            near(f.mana(p), 90, "Ten Mana paid"); near(Skills.cooldownRemaining(p, SkillDefinition.THREEFOLD_SYSTEM), 500, "Twenty-five second cooldown");
            near(WeaverPower.bonus(p, WeaverPower.Element.FIRE), 0, "Fire has no own-element reserve");
            near(WeaverPower.bonus(p, WeaverPower.Element.ICE), .3, "Full reserve grants thirty percent");
            near(WeaverPower.reserve(p, WeaverPower.Element.ICE), 0, "Activation consumes bank");
            var second = f.target(1, 0, 2); f.hit(p, second, SkillDefinition.ICE_BOLT, 6);
            near(WeaverPower.reserve(p, WeaverPower.Element.FIRE), 0, "Banking paused during active buff");
            near(AuraDamage.elementalMultiplier(p, WeaverPower.Element.ICE), 1.3, "Elemental outgoing multiplier");
            SkillLoadouts.bind(p, 0, null);
            near(WeaverPower.bonus(p, WeaverPower.Element.ICE), 0, "Unassignment clears active buff");
            check(!p.getData(RpgData.WEAVER_DISPLAY).threefold(), "Unassignment clears owner HUD");
            helper.succeed();
        }
    }

    static void threefoldAccounting(GameTestHelper helper) {
        try (var f = new Fixture(helper, 195)) {
            var p = f.player(); f.equip(p, SkillDefinition.THREEFOLD_SYSTEM, 0);
            var low = f.target(0, 0, 2); low.setHealth(2);
            f.hit(p, low, SkillDefinition.ICE_BOLT, 20);
            near(WeaverPower.reserve(p, WeaverPower.Element.FIRE), .5, "Lethal damage counts actual health without overkill");
            var immune = f.target(0, 0, 3); immune.setInvulnerable(true); f.hit(p, immune, SkillDefinition.ICE_BOLT, 20);
            near(WeaverPower.reserve(p, WeaverPower.Element.FIRE), .5, "Invulnerability creates no power");
            var ally = f.player(); ally.setPos(f.origin.add(0, 0, 2)); f.team(p, ally);
            var holder = p.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, SkillDefinition.FIRE_BOLT.id()));
            ally.hurtServer(p.level(), new DamageSource(holder, p, p), 6);
            near(WeaverPower.reserve(p, WeaverPower.Element.ICE), 0, "Allied damage excluded");
            PlayerProfiles.switchClass(ally, PlayerClass.REAVER); ClassResources.set(ally, PlayerClass.REAVER, 100);
            f.equip(ally, SkillDefinition.FORTITUDE_AURA, 0);
            f.use(ally, SkillDefinition.FORTITUDE_AURA);
            var enemy = f.player(); enemy.setPos(f.origin.add(0, 0, 4)); f.equip(enemy, SkillDefinition.THREEFOLD_SYSTEM, 0);
            f.hit(enemy, p, SkillDefinition.FIRE_BOLT, 4);
            near(Auras.shield(p), 2, "Fortitude absorbs damage");
            near(WeaverPower.reserve(enemy, WeaverPower.Element.ICE), 1, "Fortitude damage counts once");
            helper.succeed();
        }
    }

    static void threefoldExpiry(GameTestHelper helper) {
        var f = new Fixture(helper, 180); var p = f.player(); f.equip(p, SkillDefinition.THREEFOLD_SYSTEM, 0);
        var target = f.target(0, 0, 2); f.hit(p, target, SkillDefinition.FIRE_BOLT, 4);
        helper.onEachTick(f::tick);
        helper.runAtTickTime(60, () -> f.hit(p, target, SkillDefinition.ICE_BOLT, 8));
        helper.runAtTickTime(205, () -> {
            near(WeaverPower.reserve(p, WeaverPower.Element.ICE), 0, "Old contribution expires independently");
            near(WeaverPower.reserve(p, WeaverPower.Element.FIRE), 2, "New contribution survives old expiry");
            near(WeaverPower.reserve(p, WeaverPower.Element.LIGHTNING), 2, "Shared reserve loses only old contribution");
            p.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(.1); f.use(p, SkillDefinition.THREEFOLD_SYSTEM);
            near(WeaverPower.bonus(p, WeaverPower.Element.FIRE), .1, "Partial reserve bonus is linear");
        });
        helper.runAtTickTime(225, () -> {
            try {
                check(!WeaverPower.active(p), "Scaled buff expires"); f.hit(p, target, SkillDefinition.ICE_BOLT, 4);
                near(WeaverPower.reserve(p, WeaverPower.Element.FIRE), 1, "Banking resumes after expiry"); helper.succeed();
            } finally { f.close(); }
        });
    }

    static void threefoldSnapshots(GameTestHelper helper) {
        try (var f = new Fixture(helper, 165)) {
            var p = f.player(); f.equip(p, SkillDefinition.THREEFOLD_SYSTEM, 0); var target = f.target(0, 0, 2);
            f.hit(p, target, SkillDefinition.ICE_BOLT, 24); f.use(p, SkillDefinition.THREEFOLD_SYSTEM);
            f.learn(p, SkillDefinition.FIRE_BOLT); p.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
            ClassResources.setInfinite(p, true); f.use(p, SkillDefinition.FIRE_BOLT); ClassResources.setInfinite(p, false);
            var bolt = p.level().getEntitiesOfClass(ElementalBolt.class, p.getBoundingBox().inflate(2)).getFirst();
            near(bolt.getData(RpgData.AURA_PROJECTILE_POWER), 1.3, "Projectile captures elemental buff at fire time");
            var cry = f.player(); PlayerProfiles.switchClass(cry, PlayerClass.REAVER); ClassResources.set(cry, PlayerClass.REAVER, 100); f.team(p, cry); f.learn(cry, SkillDefinition.BATTLE_CRY);
            f.use(cry, SkillDefinition.BATTLE_CRY);
            near(AuraDamage.elementalMultiplier(p, WeaverPower.Element.FIRE), 1.45, "Threefold and Battle Cry add");
            SkillLoadouts.bind(p, 0, null); target.invulnerableTime = 0;
            ElementalBolts.hit(p.level(), p, bolt, target, SkillDefinition.FIRE_BOLT, 80);
            near(target.getHealth(), 200 - 24 - 7.8, "Committed projectile retains bonus and never reapplies later shout");
            near(WeaverPower.reserve(p, WeaverPower.Element.ICE), 0, "Removed Threefold does not bank");
            helper.succeed();
        }
    }

    static void teleportGeometry(GameTestHelper helper) {
        try (var f = new Fixture(helper, 295)) {
            var p = f.player(); f.learn(p, SkillDefinition.TELEPORT); ClassResources.setInfinite(p, true); Skills.setIgnoreCooldowns(p, true);
            p.setDeltaMovement(3, -10, 2); p.fallDistance = 60;
            f.use(p, SkillDefinition.TELEPORT);
            near(p.getZ() - f.origin.z, 8, "Full-range airborne blink", .002);
            check(p.getDeltaMovement().equals(Vec3.ZERO) && p.fallDistance == 0, "Blink clears momentum and fall distance");
            var endpoint = p.position(); p.connection.tick();
            near(p.position().distanceTo(endpoint), 0, "Native connection tick retains destination");
            try {
                var sequence = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");
                sequence.setAccessible(true);
                p.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(sequence.getInt(p.connection)));
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            p.connection.tick(); near(p.position().distanceTo(endpoint), 0, "Native acknowledgement retains blink endpoint");
            p.setPos(f.origin); p.setYRot(-45); p.setXRot(0);
            for (int z = -1; z <= 10; z++) { f.block(2, 0, z, Blocks.STONE); f.block(2, 1, z, Blocks.STONE); }
            f.use(p, SkillDefinition.TELEPORT);
            var movement = p.position().subtract(f.origin);
            near(movement.x, movement.z, "Diagonal blink retains aim without wall slide");
            check(movement.x > 1 && movement.x < 1.3, "Player width stops before wall");
            check(p.level().noCollision(p), "Endpoint accommodates full body");
            p.setPos(f.origin); p.setYRot(0); p.setXRot(-90); f.block(0, 3, 0, Blocks.STONE);
            f.use(p, SkillDefinition.TELEPORT);
            check(p.getY() - f.origin.y > 1 && p.getY() - f.origin.y < 1.3, "Head stops before ceiling");
            p.setPos(f.origin); p.setXRot(0); f.block(0, 0, 3, Blocks.IRON_BARS); f.block(0, 1, 3, Blocks.IRON_BARS);
            f.use(p, SkillDefinition.TELEPORT);
            check(p.getZ() - f.origin.z > 2 && p.getZ() - f.origin.z < 3, "Thin voxel shapes cannot be crossed");
            p.setPos(f.origin.x, p.level().getMaxY() - 3, f.origin.z); p.setXRot(-90);
            f.use(p, SkillDefinition.TELEPORT);
            check(p.getBoundingBox().maxY <= p.level().getMaxY(), "Full player stays inside build-height bound");
            helper.succeed();
        }
    }

    static void teleportBlocked(GameTestHelper helper) {
        try (var f = new Fixture(helper, 280)) {
            var p = f.player(); f.learn(p, SkillDefinition.TELEPORT);
            f.block(0, 0, 1, Blocks.STONE); f.block(0, 1, 1, Blocks.STONE);
            p.setPos(f.origin.add(0, 0, .2));
            check(key(Skills.activate(p, SkillDefinition.TELEPORT), "destination"), "Touching wall rejects blink");
            near(f.mana(p), 100, "Rejected destination costs nothing"); near(Skills.cooldownRemaining(p, SkillDefinition.TELEPORT), 0, "Rejected destination starts no cooldown");
            p.setPos(f.origin.add(0, 0, -.5));
            f.use(p, SkillDefinition.TELEPORT);
            check(p.getZ() < f.origin.z + .21, "Clear space behind wall never enables tunneling");
            near(f.mana(p), 75, "Short valid blink pays full cost");
            helper.succeed();
        }
    }

    static void lifecycle(GameTestHelper helper) {
        try (var f = new Fixture(helper, 250)) {
            for (var reason : SkillLifecycle.Reason.values()) {
                var p = f.player(); f.equip(p, SkillDefinition.THREEFOLD_SYSTEM, 0); var target = f.target(0, 0, 2);
                f.charge(p, 1, 0, 0); f.hit(p, target, SkillDefinition.ICE_BOLT, 24); f.use(p, SkillDefinition.THREEFOLD_SYSTEM);
                SkillLifecycle.cancelPlayer(p, reason);
                near(WeaverPower.charges(p, WeaverPower.Element.FIRE), 0, "Charges clear on " + reason);
                check(!WeaverPower.active(p), "Buff clears on " + reason);
                check(p.getData(RpgData.WEAVER_DISPLAY).equals(WeaverDisplay.EMPTY), "Owner view clears on " + reason);
                near(Skills.cooldownRemaining(p, SkillDefinition.THREEFOLD_SYSTEM), 500, "Cooldown survives " + reason);
                var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, p.registryAccess());
                p.saveWithoutId(saved);
                check(!saved.buildResult().toString().contains("weaver_display"), "Transient view never enters saved NBT");
                target.discard();
            }
            helper.succeed();
        }
    }

    static void config(GameTestHelper helper) {
        try (var reader = helper.getLevel().getServer().getResourceManager().getResourceOrThrow(WeaverSkillData.FILE).openAsReader()) {
            var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            var data = WeaverSkillData.parse(json);
            near(data.discharge().damage(), 1, "Shipped Discharge damage"); near(data.threefold().capacity(), 6, "Shipped reserve cap"); near(data.teleport().range(), 8, "Shipped Teleport range");
            for (String group : List.of("discharge", "threefold_system", "teleport")) {
                var changed = json.deepCopy(); changed.getAsJsonObject(group).addProperty("cost", -1);
                boolean rejected = false; try { WeaverSkillData.parse(changed); } catch (IllegalArgumentException expected) { rejected = true; }
                check(rejected, "Reject invalid cost in " + group);
            }
            var bad = json.deepCopy(); bad.getAsJsonObject("discharge").addProperty("max_stacks", 1.5);
            boolean rejected = false; try { WeaverSkillData.parse(bad); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Reject fractional counts");
            helper.succeed();
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }

    static void dischargeThreefold(GameTestHelper helper) {
        try (var f = new Fixture(helper, 230)) {
            var p = f.player(); f.charge(p, 5, 5, 5); f.equip(p, SkillDefinition.THREEFOLD_SYSTEM, 0);
            var target = f.target(0, 0, 1); f.use(p, SkillDefinition.DISCHARGE);
            near(WeaverPower.reserve(p, WeaverPower.Element.FIRE), 2.5, "Ice/lightning Discharge builds fire reserve");
            near(WeaverPower.reserve(p, WeaverPower.Element.ICE), 2.5 + 2.0 / 3, "Fire/lightning and explosion build ice reserve");
            near(WeaverPower.reserve(p, WeaverPower.Element.LIGHTNING), 2.5 + 2.0 / 3, "Fire/ice and explosion build lightning reserve");
            f.use(p, SkillDefinition.THREEFOLD_SYSTEM); f.charge(p, 0, 1, 0);
            double before = target.getHealth(); f.use(p, SkillDefinition.DISCHARGE);
            near(before - target.getHealth(), 1 + .3 * (2.5 + 2.0 / 3) / 6, "Discharge captures correct elemental buff");
            near(WeaverPower.reserve(p, WeaverPower.Element.FIRE), 0, "Buffed Discharge cannot refill active Threefold");
            helper.succeed();
        }
    }

    static void reload(GameTestHelper helper) {
        var server = helper.getLevel().getServer(); var packs = List.copyOf(server.getPackRepository().getSelectedIds());
        var pack = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR)
                .resolve("covenant-weaver-test-" + UUID.randomUUID()).toAbsolutePath().normalize();
        var directory = pack.resolve("data/covenant/skills");
        try (var f = new Fixture(helper, 245)) {
            try {
                var p = f.player(); f.equip(p, SkillDefinition.THREEFOLD_SYSTEM, 0); var target = f.target(0, 0, 2);
                f.hit(p, target, SkillDefinition.ICE_BOLT, 24); f.use(p, SkillDefinition.THREEFOLD_SYSTEM);
                f.charge(p, 1, 0, 0);
                var oldSkills = p.getData(RpgData.SKILLS); var oldProfile = PlayerProfiles.get(p); var oldLoadout = p.getData(RpgData.SKILL_LOADOUT);
                com.google.gson.JsonObject json;
                try (var reader = server.getResourceManager().getResourceOrThrow(WeaverSkillData.FILE).openAsReader()) { json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject(); }
                json.getAsJsonObject("threefold_system").addProperty("max_bonus", .6);
                json.getAsJsonObject("teleport").addProperty("cost", 12);
                json.getAsJsonObject("teleport").addProperty("range", 4);
                java.nio.file.Files.createDirectories(directory);
                java.nio.file.Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Disposable Weaver reload test\",\"min_format\":101,\"max_format\":101}}");
                java.nio.file.Files.writeString(directory.resolve("weaver.json"), json.toString());
                server.getPackRepository().reload(); var selected = new ArrayList<>(packs); selected.add("file/" + pack.getFileName());
                server.reloadResources(selected).join();
                var live = WeaverSkillData.get(server);
                near(live.teleport().range(), 4, "Live reload adopts Teleport range");
                near(WeaverPower.bonus(p, WeaverPower.Element.FIRE), .3, "Existing buff retains old snapshot");
                near(WeaverPower.charges(p, WeaverPower.Element.FIRE), 1, "Reload retains transient charges");
                check(p.getData(RpgData.SKILLS).equals(oldSkills) && PlayerProfiles.get(p).equals(oldProfile)
                        && p.getData(RpgData.SKILL_LOADOUT).equals(oldLoadout), "Reload preserves resources/cooldowns/progress/loadout");
                var next = f.player(); f.learn(next, SkillDefinition.TELEPORT); next.setYRot(90);
                f.use(next, SkillDefinition.TELEPORT);
                near(f.mana(next), 88, "New activation adopts reloaded cost"); near(next.position().distanceTo(f.origin), 4, "New activation adopts reloaded range", .002);
                json.getAsJsonObject("threefold_system").addProperty("capacity", 0);
                java.nio.file.Files.writeString(directory.resolve("weaver.json"), json.toString());
                boolean rejected = false;
                try { server.reloadResources(selected).join(); } catch (java.util.concurrent.CompletionException expected) { rejected = true; }
                check(rejected && WeaverSkillData.get(server) == live, "Invalid reload preserves live snapshot"); helper.succeed();
            } finally {
                server.reloadResources(packs).join();
                java.nio.file.Files.deleteIfExists(directory.resolve("weaver.json")); java.nio.file.Files.deleteIfExists(pack.resolve("pack.mcmeta"));
                java.nio.file.Files.deleteIfExists(directory); java.nio.file.Files.deleteIfExists(directory.getParent());
                java.nio.file.Files.deleteIfExists(pack.resolve("data")); java.nio.file.Files.deleteIfExists(pack); server.getPackRepository().reload();
            }
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper; final Vec3 origin;
        final List<ServerPlayer> players = new ArrayList<>(); final List<Entity> entities = new ArrayList<>();
        final java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> blocks = new java.util.HashMap<>();
        final List<net.minecraft.world.scores.PlayerTeam> teams = new ArrayList<>();
        Fixture(GameTestHelper helper, int height) { this.helper = helper; origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, height, 1))); }
        ServerPlayer player() {
            var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), "wea" + UUID.randomUUID().toString().substring(0, 8)), false);
            var p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND); new io.netty.channel.embedded.EmbeddedChannel(connection);
            helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
            p.setGameMode(GameType.SURVIVAL); p.connection.markClientLoaded(); p.setNoGravity(true); p.setPos(origin);
            p.setYRot(0); p.setXRot(0); p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            PlayerProfiles.selectClass(p, PlayerClass.WEAVER); ClassResources.set(p, PlayerClass.WEAVER, 100); players.add(p); return p;
        }
        LivingEntity target(double x, double y, double z) {
            var t = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND); t.setPos(origin.add(x, y, z)); t.setNoAi(true); t.setNoGravity(true);
            t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); t.setHealth(200); t.getAttribute(Attributes.ARMOR).setBaseValue(0);
            t.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); helper.getLevel().addFreshEntity(t); entities.add(t); return t;
        }
        void learn(ServerPlayer p, SkillDefinition skill) { FixtureSkills.learn(p, skill.id(), true); }
        void equip(ServerPlayer p, SkillDefinition skill, int slot) { learn(p, skill); SkillLoadouts.bind(p, slot, skill.id()); }
        void use(ServerPlayer p, SkillDefinition skill) {
            resetThrottle(p); var message = Skills.activate(p, skill); check(key(message, "used") || key(message, "started"), "Activation: " + message.getString());
            entities.addAll(p.level().getEntitiesOfClass(ElementalBolt.class, p.getBoundingBox().inflate(3)));
        }
        void charge(ServerPlayer p, int fire, int ice, int lightning) {
            learn(p, SkillDefinition.DISCHARGE); p.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
            ClassResources.setInfinite(p, true); Skills.setIgnoreCooldowns(p, true); float pitch = p.getXRot(); p.setXRot(-90);
            int[] counts = {fire, ice, lightning}; SkillDefinition[] bolts = {SkillDefinition.FIRE_BOLT, SkillDefinition.ICE_BOLT, SkillDefinition.LIGHTNING_BOLT};
            for (int e = 0; e < 3; e++) { if (counts[e] > 0) { learn(p, bolts[e]); } for (int i = 0; i < counts[e]; i++) { use(p, bolts[e]); } }
            p.setXRot(pitch); ClassResources.setInfinite(p, false); // Cooldown bypass remains for multiple independent assertions.
        }
        void hit(ServerPlayer p, LivingEntity target, SkillDefinition skill, float amount) {
            target.invulnerableTime = 0;
            var holder = p.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, skill.id()));
            target.hurtServer(p.level(), new DamageSource(holder, p, p), amount);
        }
        void block(int x, int y, int z, net.minecraft.world.level.block.Block block) {
            var pos = BlockPos.containing(origin).offset(x, y, z); blocks.putIfAbsent(pos, helper.getLevel().getBlockState(pos)); helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        }
        void team(ServerPlayer... members) {
            var board = helper.getLevel().getScoreboard(); var team = board.addPlayerTeam("w" + UUID.randomUUID().toString().substring(0, 8)); teams.add(team);
            for (var p : members) { board.addPlayerToTeam(p.getScoreboardName(), team); }
        }
        double mana(ServerPlayer p) { return ClassResources.current(p, PlayerClass.WEAVER); }
        void tick() { for (var p : players) { p.doTick(); } }
        @Override public void close() {
            for (var p : players) {
                entities.addAll(p.level().getEntitiesOfClass(ElementalBolt.class, p.getBoundingBox().inflate(64), bolt -> bolt.getOwner() == p));
                SkillLifecycle.cancelPlayer(p, SkillLifecycle.Reason.LOGOUT); helper.getLevel().getServer().getPlayerList().remove(p);
            }
            entities.forEach(Entity::discard); blocks.forEach((pos, state) -> helper.getLevel().setBlockAndUpdate(pos, state));
            teams.forEach(helper.getLevel().getScoreboard()::removePlayerTeam);
        }
    }
    private static void resetThrottle(ServerPlayer p) {
        try { var field = SkillRuntime.class.getDeclaredField("nextActivation"); field.setAccessible(true); field.setLong(p.getData(RpgData.SKILL_RUNTIME), 0); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    private static boolean key(Component c, String key) { return c.getContents() instanceof TranslatableContents t && t.getKey().equals("commands.covenant.skill." + key); }
    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
    private static void near(double actual, double expected, String message) { near(actual, expected, message, .001); }
    private static void near(double actual, double expected, String message, double epsilon) { check(Math.abs(actual - expected) < epsilon, message + ": " + actual + " != " + expected); }
}
