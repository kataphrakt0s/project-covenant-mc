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
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

/** Survival players exercise the production activation, native damage, attribution and lifecycle boundaries. */
final class ReaverGameTests {
    static void cry(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var one = f.player(PlayerClass.REAVER, 0);
            var two = f.player(PlayerClass.REAVER, 1);
            var ally = f.player(PlayerClass.WEAVER, 2);
            var outsider = f.player(PlayerClass.STALKER, 3);
            f.team(one, two, ally);
            var pet = EntityType.WOLF.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            pet.tame(ally); pet.setNoAi(true); pet.setNoGravity(true); pet.setPos(f.origin.add(0, 0, 3));
            helper.getLevel().addFreshEntity(pet); f.entities.add(pet);
            f.learn(one, SkillDefinition.BATTLE_CRY); f.learn(two, SkillDefinition.BATTLE_CRY);
            f.use(one, SkillDefinition.BATTLE_CRY);
            near(ClassResources.current(one, PlayerClass.REAVER), 70, "Shout costs 30 Rage");
            check(Skills.cooldownRemaining(one, SkillDefinition.BATTLE_CRY) == 600, "Shout cooldown");
            f.use(two, SkillDefinition.BATTLE_CRY);
            near(AuraDamage.multiplier(ally, false), 1.15, "Overlapping cries do not stack");
            near(AuraDamage.multiplier(pet, false), 1.15, "Allied owned pet receives shout");
            near(AuraDamage.multiplier(outsider, false), 1, "Unteamed player excluded");
            var target = f.target(4);
            target.hurtServer(one.level(), one.damageSources().playerAttack(one), 10);
            near(target.getHealth(), 188.5, "Ordinary attack amplified exactly once");
            near(ClassResources.current(one, PlayerClass.REAVER), 81.5, "Bonus basic damage generates ordinary Rage");
            near(AuraDamage.bleedPower(one), .115, "New bleed captures shout once");
            target.invulnerableTime = 0;
            var bleedType = one.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(Bleeding.DAMAGE_TYPE);
            target.hurtServer(one.level(), new net.minecraft.world.damagesource.DamageSource(bleedType, null, one), .115f);
            near(target.getHealth(), 188.385, "Captured bleed is not amplified again");
            var arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(one.level(), ally,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
            AuraDamage.snapshot(arrow, ally, 1);
            near(arrow.getData(RpgData.AURA_PROJECTILE_POWER), 1.15, "Projectile captures shout");
            ally.setPos(f.origin.add(0, 0, 20));
            near(AuraDamage.multiplier(ally, false), 1.15, "Moving away retains timed grant");
            SkillLifecycle.cancelPlayer(one, SkillLifecycle.Reason.LOGOUT);
            near(AuraDamage.multiplier(ally, false), 1.15, "Other caster's grant survives logout");
            FixtureSkills.learn(two, SkillDefinition.BATTLE_CRY.id(), false);
            near(AuraDamage.multiplier(ally, false), 1, "Removing final source cleans remote recipients");
            near(arrow.getData(RpgData.AURA_PROJECTILE_POWER), 1.15, "Committed projectile retains bonus");
            arrow.discard();
            helper.succeed();
        }
    }

    static void cryDuration(GameTestHelper helper) {
        var f = new Fixture(helper);
        var source = f.player(PlayerClass.REAVER, 0); var ally = f.player(PlayerClass.WEAVER, 2); f.team(source, ally);
        f.learn(source, SkillDefinition.BATTLE_CRY);
        source.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(2);
        f.use(source, SkillDefinition.BATTLE_CRY);
        var late = f.player(PlayerClass.STALKER, 3); f.team(late);
        // Joining the original team after the shout never retroactively grants it.
        source.level().getScoreboard().addPlayerToTeam(late.getScoreboardName(), source.getTeam());
        near(AuraDamage.multiplier(late, false), 1, "Late entrant is excluded");
        helper.onEachTick(f::tick);
        helper.runAtTickTime(125, () -> near(AuraDamage.multiplier(ally, false), 1.15, "Duration multiplier extends shout"));
        helper.runAtTickTime(245, () -> {
            try { near(AuraDamage.multiplier(ally, false), 1, "Scaled shout expires"); helper.succeed(); }
            finally { f.close(); }
        });
    }

    static void fever(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var source = f.player(PlayerClass.REAVER, 0); var friend = f.player(PlayerClass.REAVER, 1); f.team(source, friend);
            f.learn(source, SkillDefinition.BLOOD_FEVER);
            var target = f.target(2); Bleeding.apply(target, friend, .1f, 100);
            BloodFever.refresh(source);
            near(source.getAttributeValue(Attributes.MOVEMENT_SPEED), .1, "Learned but unassigned passive grants nothing");
            SkillLoadouts.bind(source, 0, SkillDefinition.BLOOD_FEVER.id());
            BloodFever.refresh(source);
            near(source.getAttributeValue(Attributes.MOVEMENT_SPEED), .102, "Shared teammate bleed counts");
            near(source.getAttributeValue(Attributes.ATTACK_SPEED), 4.12, "Attack speed uses native base");
            Bleeding.apply(target, friend, .1f, 100); Bleeding.apply(target, source, .1f, 100);
            BloodFever.refresh(source); check(BloodFever.count(source) == 1, "Three stacks still one enemy");
            Bleeding.apply(friend, source, .1f, 100); BloodFever.refresh(source);
            check(BloodFever.count(source) == 1, "Bleeding allies excluded");
            for (int i = 0; i < 24; i++) { Bleeding.apply(f.target(2.5), friend, .1f, 100); }
            BloodFever.refresh(source);
            near(source.getAttributeValue(Attributes.MOVEMENT_SPEED), .11, "Movement caps at ten percent");
            near(source.getAttributeValue(Attributes.ATTACK_SPEED), 4.6, "Attack speed caps at fifteen percent");
            double rage = ClassResources.current(source, PlayerClass.REAVER);
            Skills.activate(source, SkillDefinition.BLOOD_FEVER);
            near(ClassResources.current(source, PlayerClass.REAVER), rage, "Passive key spends nothing");
            check(Skills.cooldownRemaining(source, SkillDefinition.BLOOD_FEVER) == 0, "Passive has no cooldown");
            for (int i = 0; i < 100; i++) { BloodFever.refresh(source); }
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) { BloodFever.refresh(source); }
            dev.covenant.Covenant.LOGGER.info("Blood Fever fixture: 1000 local refreshes, 25 bleeding enemies, {} microseconds/refresh", (System.nanoTime() - start) / 1_000_000.0);
            SkillLoadouts.bind(source, 0, null);
            near(source.getAttributeValue(Attributes.MOVEMENT_SPEED), .1, "Unbinding clears immediately");
            SkillLoadouts.bind(source, 0, SkillDefinition.BLOOD_FEVER.id()); BloodFever.refresh(source);
            PlayerProfiles.switchClass(source, PlayerClass.WEAVER);
            near(source.getAttributeValue(Attributes.ATTACK_SPEED), 4, "Class change removes passive");
            helper.succeed();
        }
    }

    static void feverUpdates(GameTestHelper helper) {
        var f = new Fixture(helper); var source = f.player(PlayerClass.REAVER, 0);
        f.learn(source, SkillDefinition.BLOOD_FEVER); SkillLoadouts.bind(source, 0, SkillDefinition.BLOOD_FEVER.id());
        var target = f.target(2); Bleeding.apply(target, source, .1f, 12);
        helper.onEachTick(f::tick);
        helper.runAtTickTime(7, () -> { check(BloodFever.count(source) == 1, "Periodic scan discovers bleed"); target.setPos(f.origin.add(0, 0, 9)); });
        helper.runAtTickTime(13, () -> { check(BloodFever.count(source) == 0, "Leaving radius updates within five ticks"); target.setPos(f.origin.add(0, 0, 2)); });
        helper.runAtTickTime(20, () -> {
            try { check(BloodFever.count(source) == 0, "Expired bleed does not reenter count"); helper.succeed(); }
            finally { f.close(); }
        });
    }

    static void stormCast(GameTestHelper helper) {
        var f = new Fixture(helper); var source = f.player(PlayerClass.REAVER, 0); f.learn(source, SkillDefinition.RAGE_STORM);
        source.getAttribute(RpgStats.RESOURCE_COST).setBaseValue(.655);
        source.getAttribute(RpgStats.COOLDOWN).setBaseValue(.655);
        source.getAttribute(RpgStats.CAST_TIME).setBaseValue(.31);
        f.use(source, SkillDefinition.RAGE_STORM);
        check(Skills.isCasting(source), "Storm has scaled wind-up");
        near(ClassResources.current(source, PlayerClass.REAVER), 100, "Wind-up does not spend Rage");
        source.setYRot(90);
        helper.onEachTick(f::tick);
        helper.runAtTickTime(7, () -> {
            try {
                check(!Skills.isCasting(source), "Scaled wind-up completes");
                var storm = f.storm(source);
                near(ClassResources.current(source, PlayerClass.REAVER), 100 - 35 * .655, "Scaled cost commits on release");
                check(Skills.cooldownRemaining(source, SkillDefinition.RAGE_STORM) > 150, "Scaled cooldown commits");
                check(storm.getX() < source.getX() && Math.abs(storm.getZ() - source.getZ()) < .01, "Direction captured at completion");
                SkillLifecycle.cancelPlayer(source, SkillLifecycle.Reason.DIMENSION_CHANGE);
                check(storm.isRemoved(), "Dimension change cleans storm immediately");
                check(Skills.cooldownRemaining(source, SkillDefinition.RAGE_STORM) > 0, "Cleanup preserves cooldown");
                helper.succeed();
            } finally { f.close(); }
        });
    }

    static void stormPulses(GameTestHelper helper) {
        var f = new Fixture(helper); var source = f.player(PlayerClass.REAVER, 0); f.learn(source, SkillDefinition.RAGE_STORM);
        var target = f.target(1);
        var armored = f.target(1); armored.getAttribute(Attributes.ARMOR).setBaseValue(20);
        var immune = f.target(1); immune.setInvulnerable(true);
        f.use(source, SkillDefinition.RAGE_STORM);
        helper.onEachTick(f::tick);
        helper.runAtTickTime(17, () -> {
            var storm = f.storm(source);
            near(target.getHealth(), 197, "Immediate release pulse");
            check(armored.getHealth() < 200 && armored.getHealth() > 197, "Native armor mitigates storm damage");
            near(immune.getHealth(), 200, "Actual invulnerability stops storm damage");
            near(ClassResources.current(source, PlayerClass.REAVER), 65, "Storm damage generates no outgoing Rage");
            try {
                // A slower supported tuning keeps the same enemy inside long enough to exercise the cap.
                var field = RageStorm.class.getDeclaredField("config"); field.setAccessible(true);
                field.set(storm, new ReaverSkillData.Storm(35, 240, 16, .01, 8, 2, 3, 10, 4));
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            target.invulnerableTime = 20;
        });
        helper.runAtTickTime(65, () -> {
            try {
                var storm = f.storm(source);
                check(storm.attempts(target) == 4, "Per-target four-attempt cap");
                check(storm.attempts(immune) == 4, "Defended attempts also consume per-target cap");
                near(target.getHealth(), 188, "Independent pulses deal full damage despite native hurt cooldown");
                var reporter = new net.minecraft.util.ProblemReporter.Collector();
                var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, source.level().registryAccess());
                check(!storm.save(output), "Storm entity is never saved");
                FixtureSkills.learn(source, SkillDefinition.RAGE_STORM.id(), false);
                check(storm.isRemoved(), "Unlearning removes released storm");
                helper.succeed();
            } finally { f.close(); }
        });
    }

    static void stormTerrain(GameTestHelper helper) {
        var f = new Fixture(helper); var source = f.player(PlayerClass.REAVER, 0); f.learn(source, SkillDefinition.RAGE_STORM);
        var target = f.target(1.9);
        // Full wall between release point and an enemy otherwise within pulse radius.
        for (int x = -2; x <= 2; x++) { for (int y = 0; y <= 3; y++) {
            var pos = BlockPos.containing(f.origin.add(x, y, 1)); f.blocks.add(pos);
            helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        } }
        f.use(source, SkillDefinition.RAGE_STORM);
        helper.onEachTick(f::tick);
        helper.runAtTickTime(22, () -> {
            try {
                near(target.getHealth(), 200, "Solid cover blocks pulses");
                check(f.storms().isEmpty(), "Storm stops at solid terrain");
                near(ClassResources.current(source, PlayerClass.REAVER), 65, "Blocked storm is still paid");
                helper.succeed();
            } finally { f.close(); }
        });
    }

    static void stormSafety(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var source = f.player(PlayerClass.REAVER, 0); f.learn(source, SkillDefinition.RAGE_STORM);
            for (var reason : SkillLifecycle.Reason.values()) {
                f.use(source, SkillDefinition.RAGE_STORM);
                SkillLifecycle.cancelPlayer(source, reason);
                check(!Skills.isCasting(source) && f.storms().isEmpty(), "Lifecycle cancels wind-up: " + reason);
                near(ClassResources.current(source, PlayerClass.REAVER), 100, "Cancelled wind-up spends nothing");
                check(Skills.cooldownRemaining(source, SkillDefinition.RAGE_STORM) == 0, "Cancelled wind-up has no cooldown");
            }
            source.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> reject = event -> {
                if (event.getEntity() instanceof RageStorm) { event.setCanceled(true); }
            };
            NeoForge.EVENT_BUS.addListener(reject);
            try {
                Skills.activate(source, SkillDefinition.RAGE_STORM);
                near(ClassResources.current(source, PlayerClass.REAVER), 100, "Rejected entity admission spends nothing");
                check(Skills.cooldownRemaining(source, SkillDefinition.RAGE_STORM) == 0 && f.storms().isEmpty(), "Admission rejection commits no timer or entity");
            } finally { NeoForge.EVENT_BUS.unregister(reject); }
            helper.succeed();
        }
    }

    static void config(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var source = f.player(PlayerClass.REAVER, 0);
            dev.covenant.rpg.PlayerProfiles.update(source, profile -> dev.covenant.rpg.progression.Progression.atLevel(profile, 5));
            SkillLoadouts.apply(source, java.util.Map.of(), java.util.Map.of(SkillDefinition.BLOOD_FEVER.id(), 1));
            check(source.getData(RpgData.SKILL_LOADOUT).learned().isEmpty(), "Blood Fever requires Rend");
            SkillLoadouts.apply(source, java.util.Map.of(), java.util.Map.of(SkillDefinition.REND.id(), 1, SkillDefinition.BLOOD_FEVER.id(), 1,
                    SkillDefinition.CLEAVE.id(), 1, SkillDefinition.RAGE_STORM.id(), 1, SkillDefinition.BATTLE_CRY.id(), 1));
            check(source.getData(RpgData.SKILL_LOADOUT).learned().size() == 5, "Five-point playable branch purchases");
            try (var reader = helper.getLevel().getServer().getServerResources().resourceManager().getResourceOrThrow(ReaverSkillData.FILE).openAsReader()) {
                var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                var parsed = ReaverSkillData.parse(json);
                near(parsed.storm().speed(), .35, "Default speed");
                json.getAsJsonObject("rage_storm").addProperty("pulse_ticks", 0);
                boolean rejected = false;
                try { ReaverSkillData.parse(json); } catch (IllegalArgumentException expected) { rejected = true; }
                check(rejected, "Zero pulse interval rejected");
                json.getAsJsonObject("rage_storm").addProperty("pulse_ticks", 10.5);
                rejected = false;
                try { ReaverSkillData.parse(json); } catch (IllegalArgumentException expected) { rejected = true; }
                check(rejected, "Fractional tick interval rejected");
            } catch (java.io.IOException error) { throw new IllegalStateException(error); }
            helper.succeed();
        }
    }

    static void reload(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var packs = List.copyOf(server.getPackRepository().getSelectedIds());
        var pack = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR)
                .resolve("covenant-reaver-test-" + UUID.randomUUID()).toAbsolutePath().normalize();
        var directory = pack.resolve("data/covenant/skills");
        try (var f = new Fixture(helper)) {
            try {
                var source = f.player(PlayerClass.REAVER, 0); var mover = f.player(PlayerClass.REAVER, 1); f.team(source, mover);
                f.learn(source, SkillDefinition.BATTLE_CRY); f.learn(mover, SkillDefinition.RAGE_STORM);
                mover.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
                f.use(source, SkillDefinition.BATTLE_CRY); f.use(mover, SkillDefinition.RAGE_STORM);
                var oldStorm = f.storms().getFirst();
                var oldProfile = PlayerProfiles.get(source); var oldLoadout = source.getData(RpgData.SKILL_LOADOUT);
                var oldSkills = source.getData(RpgData.SKILLS);
                com.google.gson.JsonObject json;
                try (var reader = server.getResourceManager().getResourceOrThrow(ReaverSkillData.FILE).openAsReader()) {
                    json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                }
                json.getAsJsonObject("battle_cry").addProperty("cost", 10);
                json.getAsJsonObject("battle_cry").addProperty("damage_bonus", .4);
                json.getAsJsonObject("rage_storm").addProperty("speed", .7);
                java.nio.file.Files.createDirectories(directory);
                java.nio.file.Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Disposable Reaver reload test\",\"min_format\":101,\"max_format\":101}}");
                java.nio.file.Files.writeString(directory.resolve("reaver.json"), json.toString());
                server.getPackRepository().reload(); var selected = new ArrayList<>(packs); selected.add("file/" + pack.getFileName());
                server.reloadResources(selected).join();
                var live = ReaverSkillData.get(server);
                near(live.storm().speed(), .7, "Reload adopts storm tuning");
                check(PlayerProfiles.get(source).equals(oldProfile) && source.getData(RpgData.SKILL_LOADOUT).equals(oldLoadout)
                        && source.getData(RpgData.SKILLS).equals(oldSkills), "Reload preserves progression, allocations, resources and cooldowns");
                near(Auras.battleCryBonus(mover), .15, "Existing shout retains old power snapshot");
                var previous = oldStorm.position(); oldStorm.tick();
                near(oldStorm.position().distanceTo(previous), .35, "Existing storm retains old speed snapshot");
                var next = f.player(PlayerClass.REAVER, 2); f.learn(next, SkillDefinition.BATTLE_CRY); f.use(next, SkillDefinition.BATTLE_CRY);
                near(ClassResources.current(next, PlayerClass.REAVER), 90, "New activation adopts new cost");
                near(Auras.battleCryBonus(next), .4, "New shout adopts new strength");
                json.getAsJsonObject("rage_storm").addProperty("pulse_ticks", 0);
                java.nio.file.Files.writeString(directory.resolve("reaver.json"), json.toString());
                boolean rejected = false;
                try { server.reloadResources(selected).join(); } catch (java.util.concurrent.CompletionException expected) { rejected = true; }
                check(rejected && ReaverSkillData.get(server) == live, "Invalid reload preserves live snapshot");
                helper.succeed();
            } finally {
                server.reloadResources(packs).join();
                java.nio.file.Files.deleteIfExists(directory.resolve("reaver.json")); java.nio.file.Files.deleteIfExists(pack.resolve("pack.mcmeta"));
                java.nio.file.Files.deleteIfExists(directory); java.nio.file.Files.deleteIfExists(directory.getParent());
                java.nio.file.Files.deleteIfExists(pack.resolve("data")); java.nio.file.Files.deleteIfExists(pack);
                server.getPackRepository().reload();
            }
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final List<ServerPlayer> players = new ArrayList<>();
        final List<Entity> entities = new ArrayList<>();
        final List<BlockPos> blocks = new ArrayList<>();
        final List<net.minecraft.world.scores.PlayerTeam> teams = new ArrayList<>();
        final Vec3 origin;
        Fixture(GameTestHelper helper) { this.helper = helper; origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 150, 1))); }
        ServerPlayer player(PlayerClass type, double z) {
            var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), "rev" + UUID.randomUUID().toString().substring(0, 8)), false);
            var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            player.setGameMode(GameType.SURVIVAL); player.connection.markClientLoaded(); player.setNoGravity(true);
            player.setPos(origin.add(0, 0, z)); player.setYRot(0); player.setXRot(0);
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            PlayerProfiles.selectClass(player, type); ClassResources.set(player, type, 100); players.add(player); return player;
        }
        LivingEntity target(double z) {
            var target = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            target.setPos(origin.add(0, 0, z)); target.setNoGravity(true); target.setNoAi(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); target.setHealth(200);
            helper.getLevel().addFreshEntity(target); entities.add(target); return target;
        }
        void learn(ServerPlayer source, SkillDefinition skill) {
            SkillTree.find(skill.id()).orElseThrow().prerequisite().ifPresent(id -> learn(source, SkillDefinition.find(id).orElseThrow()));
            FixtureSkills.learn(source, skill.id(), true);
        }
        void use(ServerPlayer source, SkillDefinition skill) {
            // Isolate activation spending from the independent out-of-combat Rage decay clock.
            ClassResources.markCombat(source);
            var result = Skills.activate(source, skill).getString();
            check(result.startsWith("Used") || result.startsWith("Casting"), "Activation: " + result);
        }
        void team(ServerPlayer... members) {
            var board = helper.getLevel().getScoreboard(); var team = board.addPlayerTeam("r" + UUID.randomUUID().toString().substring(0, 8)); teams.add(team);
            for (var member : members) { board.addPlayerToTeam(member.getScoreboardName(), team); }
        }
        List<RageStorm> storms() { return helper.getLevel().getEntitiesOfClass(RageStorm.class, new net.minecraft.world.phys.AABB(origin.add(-16, -4, -16), origin.add(16, 4, 16)), storm -> players.contains(storm.getOwner())); }
        RageStorm storm(ServerPlayer player) { return storms().stream().filter(storm -> storm.getOwner() == player).findFirst().orElseThrow(() -> new IllegalStateException("Expected released storm")); }
        void tick() { for (var player : players) { player.doTick(); } }
        @Override public void close() {
            for (var player : players) { SkillLifecycle.cancelPlayer(player, SkillLifecycle.Reason.LOGOUT); helper.getLevel().getServer().getPlayerList().remove(player); }
            entities.forEach(Entity::discard);
            blocks.forEach(pos -> helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
            teams.forEach(helper.getLevel().getScoreboard()::removePlayerTeam);
        }
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
    private static void near(double actual, double expected, String message) { check(Math.abs(actual - expected) < .001, message + ": " + actual + " != " + expected); }
}
