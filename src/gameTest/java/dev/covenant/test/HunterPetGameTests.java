package dev.covenant.test;

import com.google.gson.JsonParser;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.*;
import dev.covenant.rpg.stats.RpgStats;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Dedicated-server regressions for the real companion, native AI and owner save/load boundary. */
final class HunterPetGameTests {
    static void activation(GameTestHelper helper) {
        try (var f = new Fixture(helper, 115)) {
            var p = f.player();
            check(key(Skills.activate(p, SkillDefinition.HUNTERS_PET), "unlearned"), "Unlearned summon rejected");
            FixtureSkills.learn(p, SkillDefinition.HUNTERS_PET.id(), true);
            check(key(Skills.activate(p, SkillDefinition.HUNTERS_PET), "assigned"), "Learned alone gives no companion");
            SkillLoadouts.bind(p, 0, SkillDefinition.HUNTERS_PET.id());
            ClassResources.set(p, PlayerClass.STALKER, 34);
            check(key(Skills.activate(p, SkillDefinition.HUNTERS_PET), "resource"), "Focus required");
            ClassResources.set(p, PlayerClass.STALKER, 100);
            p.getAttribute(RpgStats.RESOURCE_COST).setBaseValue(.5); p.getAttribute(RpgStats.COOLDOWN).setBaseValue(.5);
            f.use(p); var pet = HunterPets.current(p);
            check(pet != null && pet.getOwner() == p && pet.isTame(), "Native tamed ownership");
            near(pet.getHealth(), 40, "Default health"); near(pet.getAttributeValue(Attributes.ATTACK_DAMAGE), 3, "Independent damage");
            near(ClassResources.current(p, PlayerClass.STALKER), 82.5, "Scaled summon cost once");
            near(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET), 0, "Summoning starts no cooldown");
            ClassResources.set(p, PlayerClass.STALKER, 0); resetThrottle(p);
            check(key(Skills.activate(p, SkillDefinition.HUNTERS_PET), "pet_dismissed"), "Dismissal needs no Focus");
            check(pet.isRemoved() && !HunterPets.active(p), "Dismissed companion removed");
            near(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET), 300, "Summon-time scaled cooldown");
            resetThrottle(p); check(key(Skills.activate(p, SkillDefinition.HUNTERS_PET), "cooldown"), "Cooldown prevents replacement");
            helper.succeed();
        }
    }
    static void rejectedSpawn(GameTestHelper helper) {
        try (var f = new Fixture(helper, 130)) {
            var p = f.ready();
            Consumer<EntityJoinLevelEvent> reject = event -> {
                if (event.getEntity() instanceof HunterPet pet && pet.getOwner() == p) {
                    check(key(Skills.activate(p, SkillDefinition.HUNTERS_PET), "busy"), "Join hooks cannot recursively summon");
                    event.setCanceled(true);
                }
            };
            NeoForge.EVENT_BUS.addListener(reject);
            try { check(key(Skills.activate(p, SkillDefinition.HUNTERS_PET), "unavailable"), "Cancelled native join rejects summon"); }
            finally { NeoForge.EVENT_BUS.unregister(reject); }
            check(!HunterPets.active(p), "Failed add leaves no active snapshot");
            near(ClassResources.current(p, PlayerClass.STALKER), 100, "Failed add spends nothing");
            near(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET), 0, "Failed add starts no cooldown");
            for (int x = -3; x <= 3; x++) { for (int z = -3; z <= 3; z++) { for (int y = -1; y <= 2; y++) { f.block(x, y, z, Blocks.AIR); } } }
            check(key(Skills.activate(p, SkillDefinition.HUNTERS_PET), "destination"), "No floor rejects safely");
            near(ClassResources.current(p, PlayerClass.STALKER), 100, "Unsafe placement spends nothing");
            helper.succeed();
        }
    }
    static void lifecycle(GameTestHelper helper) {
        var f = new Fixture(helper, 145); var p = f.ready(); f.use(p); var original = HunterPets.current(p);
        original.setHealth(17); original.addEffect(new MobEffectInstance(MobEffects.POISON, 160));
        SkillLoadouts.bind(p, 1, SkillDefinition.HUNTERS_PET.id()); SkillLoadouts.bind(p, 0, null);
        check(HunterPets.current(p) == original, "Changing slots while still assigned preserves pet");
        SkillLifecycle.cancelPlayer(p, SkillLifecycle.Reason.DIMENSION_CHANGE);
        check(original.isRemoved() && HunterPets.current(p) == null && HunterPets.active(p), "Dimension boundary stows companion");
        near(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET), 0, "Transfer has no cooldown");
        helper.onEachTick(f::tick);
        helper.runAtTickTime(8, () -> {
            var restored = HunterPets.current(p);
            check(restored != null && restored != original && restored.getUUID().equals(original.getUUID()), "Restore retains one companion identity");
            near(restored.getHealth(), 17, "Transfer retains health");
            check(restored.hasEffect(MobEffects.POISON) && restored.getTarget() == null, "Transfer retains effects and clears target");
            near(ClassResources.current(p, PlayerClass.STALKER), 65, "Restoration does not spend Focus");
            SkillLoadouts.bind(p, 1, null);
            check(!HunterPets.active(p) && restored.isRemoved(), "Last slot removal dismisses immediately");
            check(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET) == 600, "Slot removal starts cooldown");
            try { helper.succeed(); } finally { f.close(); }
        });
    }
    static void persistence(GameTestHelper helper) {
        var f = new Fixture(helper, 160); var p = f.ready(); f.use(p); var original = HunterPets.current(p);
        original.setHealth(13); original.setRemainingFireTicks(73); original.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140));
        var reporter = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(reporter, p.registryAccess()); p.saveWithoutId(output);
        var saved = output.buildResult();
        check(saved.toString().contains("covenant:hunters_pet"), "Ordinary player save captures active native pet");
        SkillLifecycle.cancelPlayer(p, SkillLifecycle.Reason.LOGOUT);
        check(original.isRemoved() && HunterPets.current(p) == null && HunterPets.active(p), "Logout leaves only owner snapshot");
        // Native owner loading, with the same registry context used by actual player storage.
        p.load(TagValueInput.create(reporter, p.registryAccess(), saved));
        check(reporter.isEmpty(), "Native player/pet serialization has no reported problems: " + reporter.getReport());
        check(HunterPets.current(p) == null && HunterPets.active(p), "Loading does not pre-spawn a second pet");
        helper.onEachTick(f::tick);
        helper.runAtTickTime(8, () -> {
            var pet = HunterPets.current(p);
            check(pet != null && pet.getUUID().equals(original.getUUID()), "Loaded snapshot restores identity");
            near(pet.getHealth(), 13, "Native save retains wounded health");
            check(pet.getRemainingFireTicks() > 0 && pet.hasEffect(MobEffects.WEAKNESS), "Native fire and effects survive save/load");
            check(f.companions(p) == 1, "Exactly one materialized pet after restore");
            near(ClassResources.current(p, PlayerClass.STALKER), 65, "Save/restore does not charge again");
            HunterPets.store(p);
            var roundTrip = HunterPets.State.CODEC.parse(NbtOps.INSTANCE,
                    HunterPets.State.CODEC.encodeStart(NbtOps.INSTANCE, p.getData(RpgData.HUNTER_PET)).getOrThrow()).getOrThrow();
            p.setData(RpgData.HUNTER_PET, roundTrip);
        });
        helper.runAtTickTime(15, () -> {
            check(f.companions(p) == 1 && HunterPets.current(p) != null, "Repeated store/codec round trip cannot duplicate pet");
            try { helper.succeed(); } finally { f.close(); }
        });
    }
    static void targeting(GameTestHelper helper) {
        try (var f = new Fixture(helper, 175)) {
            var p = f.ready(); f.use(p); var pet = HunterPets.current(p);
            var enemy = f.mob(EntityType.HUSK, 0, 3); var creeper = f.mob(EntityType.CREEPER, 2, 3);
            check(pet.getTarget() == null, "No autonomous hunting");
            var arrow = EntityType.ARROW.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            enemy.hurtServer(helper.getLevel(), p.damageSources().arrow(arrow, p), 1);
            check(pet.getTarget() == enemy, "Landed ranged damage directs companion");
            creeper.hurtServer(helper.getLevel(), p.damageSources().playerAttack(p), 1);
            check(pet.getTarget() == creeper, "Creepers are valid engaged enemies");
            enemy.invulnerableTime = 0; p.hurtServer(helper.getLevel(), enemy.damageSources().mobAttack(enemy), 1);
            check(pet.getTarget() == enemy, "Owner defense targets attacker");
            pet.invulnerableTime = 0; pet.hurtServer(helper.getLevel(), creeper.damageSources().mobAttack(creeper), 1);
            check(pet.getTarget() == creeper, "Self defense targets attacker");
            var ally = f.player();
            var team = helper.getLevel().getScoreboard().addPlayerTeam("pet" + UUID.randomUUID().toString().substring(0, 8)); f.teams.add(team);
            for (var member : List.of(p, ally)) { helper.getLevel().getScoreboard().addPlayerToTeam(member.getScoreboardName(), team); }
            check(!pet.canAttack(p) && !pet.canAttack(ally), "Owner and teammates protected");
            var owned = f.mob(EntityType.WOLF, 3, 0); owned.setTame(true, false); owned.setOwner(ally);
            check(!pet.canAttack(owned), "Teammate-owned pets protected");
            pet.setTarget(null); enemy.setInvulnerable(true);
            enemy.invulnerableTime = 0; enemy.hurtServer(helper.getLevel(), p.damageSources().playerAttack(p), 1);
            check(pet.getTarget() == null, "Undamaging attempted attacks do not direct pet");
            helper.succeed();
        }
    }
    static void nativeAi(GameTestHelper helper) {
        var f = new Fixture(helper, 190); var p = f.ready(); f.use(p); var pet = HunterPets.current(p);
        var enemy = f.mob(EntityType.HUSK, -1, -2); enemy.setNoAi(false); enemy.setTarget(p);
        enemy.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0); pet.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        var attacks = new ArrayList<Long>();
        var amounts = new ArrayList<Float>();
        Consumer<LivingDamageEvent.Post> observer = event -> {
            if (event.getSource().getEntity() == pet && event.getEntity() == enemy && AuraDamage.landedDamage(event) > 0) {
                attacks.add(helper.getLevel().getGameTime()); amounts.add((float) AuraDamage.landedDamage(event));
            }
        };
        NeoForge.EVENT_BUS.addListener(observer);
        enemy.hurtServer(helper.getLevel(), p.damageSources().playerAttack(p), 1);
        enemy.invulnerableTime = 0;
        helper.onEachTick(f::tick);
        helper.runAtTickTime(65, () -> {
            check(attacks.size() >= 2, "Native melee goal repeatedly bites engaged enemy: " + attacks);
            amounts.forEach(amount -> near(amount, 3, "Native pet bite damage"));
            for (int i = 1; i < attacks.size(); i++) { check(attacks.get(i) - attacks.get(i - 1) >= 30, "Configured attack interval enforced"); }
            check(enemy.getTarget() == pet, "Normal hostile retaliation draws aggro to companion");
            check(ClassResources.inCombat(p), "Pet combat refreshes owner combat timer");
            pet.setTarget(null); enemy.setNoAi(true); enemy.setTarget(null);
            p.setPos(f.origin.add(15, 0, 0));
        });
        helper.runAtTickTime(88, () -> {
            try {
                check(pet.distanceToSqr(p) < 100, "Native follow/teleport catches owner without forced chunks");
                helper.succeed();
            } finally { NeoForge.EVENT_BUS.unregister(observer); f.close(); }
        });
    }
    static void deathAndFeeding(GameTestHelper helper) {
        try (var f = new Fixture(helper, 205)) {
            var p = f.ready(); f.use(p); var pet = HunterPets.current(p);
            pet.setHealth(10); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF, 8));
            int before = p.getMainHandItem().getCount(); pet.mobInteract(p, InteractionHand.MAIN_HAND);
            check(pet.getHealth() > 10 && p.getMainHandItem().getCount() < before, "Native wolf food heals and consumes food");
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); pet.mobInteract(p, InteractionHand.MAIN_HAND);
            check(!pet.isOrderedToSit() && !pet.canMate(pet) && !pet.canFallInLove(), "No sitting or breeding");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOLF_ARMOR)); pet.mobInteract(p, InteractionHand.MAIN_HAND);
            check(!pet.isWearingBodyArmor(), "No wolf armor interaction");
            Consumer<LivingDeathEvent> cancel = event -> { if (event.getEntity() == pet) { pet.setHealth(1); event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(cancel);
            try { pet.hurtServer(helper.getLevel(), pet.damageSources().genericKill(), 1000); }
            finally { NeoForge.EVENT_BUS.unregister(cancel); }
            check(HunterPets.current(p) == pet && pet.isAlive(), "Cancelled pet death preserves companion");
            near(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET), 0, "Cancelled pet death starts no cooldown");
            pet.invulnerableTime = 0; pet.hurtServer(helper.getLevel(), pet.damageSources().genericKill(), 1000);
            check(!HunterPets.active(p), "Confirmed pet defeat clears active companion");
            near(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET), 600, "Pet defeat starts cooldown");
            Skills.setIgnoreCooldowns(p, true); ClassResources.set(p, PlayerClass.STALKER, 100); f.use(p);
            long xp = PlayerProfiles.get(p).experience();
            var enemy = f.mob(EntityType.HUSK, 1, 3); enemy.setHealth(2);
            HunterPets.current(p).doHurtTarget(helper.getLevel(), enemy);
            check(PlayerProfiles.get(p).experience() > xp, "Pet killing blow grants owner Covenant XP");
            SkillLifecycle.confirmedDeath(p);
            check(!HunterPets.active(p), "Confirmed owner death dismisses pet");
            near(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET), 600, "Owner death starts cooldown");
            helper.succeed();
        }
    }
    static void config(GameTestHelper helper) {
        var data = JsonParser.parseString("{\"cost\":35,\"cooldown_ticks\":600,\"health\":40,\"damage\":3,\"attack_interval_ticks\":30}").getAsJsonObject();
        near(HunterPetData.parse(data).health(), 40, "Default data parses");
        for (var field : List.of("health", "damage", "attack_interval_ticks", "cost", "cooldown_ticks")) {
            var invalid = data.deepCopy(); invalid.addProperty(field, -1);
            boolean rejected = false; try { HunterPetData.parse(invalid); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Reject negative " + field);
        }
        data.addProperty("attack_interval_ticks", 2.5);
        boolean rejected = false; try { HunterPetData.parse(data); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Reject fractional tick interval"); helper.succeed();
    }

    /** Exercises the registered controller when native limb-speed detection is absent on a tracked wolf. */
    @SuppressWarnings("unchecked")
    static void locomotion(GameTestHelper helper) {
        try (var f = new Fixture(helper, 280)) {
            var p = f.ready(); f.use(p); var pet = HunterPets.current(p);
            pet.setNoAi(true); pet.setNoGravity(true); pet.tick();
            var manager = new com.geckolib.animatable.manager.AnimatableManager<HunterPet>(pet);
            var controller = manager.getAnimationControllers().get("main");
            var field = com.geckolib.animation.AnimationController.class.getDeclaredField("stateHandler"); field.setAccessible(true);
            var handler = (com.geckolib.animation.AnimationController.AnimationStateHandler<HunterPet>) field.get(controller);
            var renderState = new com.geckolib.renderer.base.GeoRenderState.Impl();
            renderState.addGeckolibData(com.geckolib.constant.DataTickets.IS_MOVING, false);
            var test = new com.geckolib.animation.state.AnimationTest<>(pet, renderState, manager, controller);
            var idle = com.geckolib.animation.RawAnimation.begin().thenLoop("animation.original_wolf.idle");
            var walk = com.geckolib.animation.RawAnimation.begin().thenLoop("animation.original_wolf.walk");
            handler.handle(test); check(controller.getCurrentRawAnimation().equals(idle), "Stationary wolf selects idle");
            pet.setPos(pet.position().add(.12, 0, 0)); pet.tick();
            near(pet.walkAnimation.speed(), 0, "Fixture has no client native limb-speed flag");
            handler.handle(test); check(controller.getCurrentRawAnimation().equals(walk), "Actual horizontal travel selects walk even without native moving flag");
            pet.tick(); handler.handle(test);
            check(controller.getCurrentRawAnimation().equals(idle), "Stopped wolf returns to idle");
            pet.setPos(pet.position().add(0, .2, 0)); pet.tick(); handler.handle(test);
            check(controller.getCurrentRawAnimation().equals(idle), "Vertical movement alone does not walk");
            pet.setPos(pet.position().add(5, 0, 0)); pet.tick(); handler.handle(test);
            check(controller.getCurrentRawAnimation().equals(idle), "Recall-sized discontinuity does not trigger walk fallback");
            renderState.addGeckolibData(com.geckolib.constant.DataTickets.IS_MOVING, true); handler.handle(test);
            check(controller.getCurrentRawAnimation().equals(walk), "Native moving flag still selects walk");
            pet.hurtTime = 3; handler.handle(test);
            check(controller.getCurrentRawAnimation().equals(com.geckolib.animation.RawAnimation.begin().thenPlay("animation.original_wolf.hurt")), "Hurt keeps priority over locomotion");
            pet.hurtTime = 0;
            CompanionAnimationFixture.checkActionRecovery(pet, manager, "original_wolf");
            helper.succeed();
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    static void storedSafety(GameTestHelper helper) {
        var f = new Fixture(helper, 220); var p = f.ready(); f.use(p); var original = HunterPets.current(p);
        original.setHealth(19);
        original.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        check(HunterPets.current(p) == null && HunterPets.active(p), "Chunk unloading retains owner snapshot");
        for (int x = -3; x <= 3; x++) { for (int z = -3; z <= 3; z++) { f.block(x, -1, z, Blocks.AIR); } }
        helper.onEachTick(f::tick);
        helper.runAtTickTime(8, () -> {
            check(HunterPets.active(p) && HunterPets.current(p) == null, "No safe floor keeps companion stored");
            near(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET), 0, "Unloading/unsafe restore starts no cooldown");
            near(ClassResources.current(p, PlayerClass.STALKER), 65, "Stored pet has no recurring cost");
            f.block(-2, -1, -2, Blocks.STONE);
        });
        helper.runAtTickTime(32, () -> {
            try {
                var restored = HunterPets.current(p);
                check(restored != null && f.companions(p) == 1, "Safe floor restores exactly one companion");
                near(restored.getHealth(), 19, "Waiting does not regenerate health");
                PlayerProfiles.switchClass(p, PlayerClass.REAVER);
                check(restored.isRemoved() && !HunterPets.active(p), "Class switch dismisses immediately");
                near(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET), 600, "Class switch starts cooldown");
                helper.succeed();
            } finally { f.close(); }
        });
    }

    static void travel(GameTestHelper helper) {
        var f = new Fixture(helper, 235); var p = f.ready(); f.use(p); var original = HunterPets.current(p); original.setHealth(23);
        var other = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        check(other != null, "Native Nether available");
        var center = new BlockPos(BlockPos.containing(f.origin).getX(), 150, BlockPos.containing(f.origin).getZ());
        var remote = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        for (int dx = -3; dx <= 3; dx++) { for (int dz = -3; dz <= 3; dz++) {
            var pos = center.offset(dx, -1, dz); remote.put(pos, other.getBlockState(pos)); other.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        } }
        f.cleanups.add(() -> remote.forEach(other::setBlockAndUpdate));
        p.teleportTo(other, center.getX() + .5, 150, center.getZ() + .5, Set.of(), 0, 0, false); p.connection.markClientLoaded();
        check(p.level() == other && original.isRemoved() && HunterPets.current(p) == null, "Native dimension event stows old instance");
        helper.onEachTick(f::tick);
        helper.runAtTickTime(10, () -> {
            try {
                var restored = HunterPets.current(p);
                check(restored != null && restored.level() == other && restored.getOwner() == p, "Companion follows actual owner dimension transition");
                near(restored.getHealth(), 23, "Actual transfer preserves health");
                near(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET), 0, "Actual transfer has no cooldown");
                near(ClassResources.current(p, PlayerClass.STALKER), 65, "Actual transfer has no Focus cost");
                helper.succeed();
            } finally { f.close(); }
        });
    }

    static void reload(GameTestHelper helper) {
        var server = helper.getLevel().getServer(); var packs = List.copyOf(server.getPackRepository().getSelectedIds());
        var pack = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR)
                .resolve("covenant-pet-test-" + UUID.randomUUID()).toAbsolutePath().normalize();
        var directory = pack.resolve("data/covenant/skills");
        try (var f = new Fixture(helper, 250)) {
            try {
                var p = f.ready(); f.use(p); var pet = HunterPets.current(p); pet.setHealth(17);
                var json = JsonParser.parseString("{\"cost\":10,\"cooldown_ticks\":100,\"health\":60,\"damage\":5,\"attack_interval_ticks\":40}").getAsJsonObject();
                java.nio.file.Files.createDirectories(directory);
                java.nio.file.Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Disposable Hunter pet reload test\",\"min_format\":101,\"max_format\":101}}");
                java.nio.file.Files.writeString(directory.resolve("hunters_pet.json"), json.toString());
                server.getPackRepository().reload(); var selected = new ArrayList<>(packs); selected.add("file/" + pack.getFileName());
                server.reloadResources(selected).join(); var live = HunterPetData.get(server);
                near(live.health(), 60, "Valid reload adopts health");
                near(pet.getHealth(), 17, "Reload does not heal existing pet"); near(pet.getMaxHealth(), 40, "Existing attributes retain snapshot");
                HunterPets.dismiss(p); near(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET), 600, "Existing pet retains original cooldown snapshot");
                Skills.setIgnoreCooldowns(p, true); ClassResources.set(p, PlayerClass.STALKER, 100); f.use(p);
                near(HunterPets.current(p).getHealth(), 60, "Next summon adopts health");
                near(HunterPets.current(p).getAttributeValue(Attributes.ATTACK_DAMAGE), 5, "Next summon adopts damage");
                near(ClassResources.current(p, PlayerClass.STALKER), 90, "Next summon adopts Focus cost");
                HunterPets.dismiss(p); near(Skills.cooldownRemaining(p, SkillDefinition.HUNTERS_PET), 100, "Next summon adopts cooldown");
                json.addProperty("health", 0); java.nio.file.Files.writeString(directory.resolve("hunters_pet.json"), json.toString());
                boolean rejected = false;
                try { server.reloadResources(selected).join(); } catch (java.util.concurrent.CompletionException expected) { rejected = true; }
                check(rejected && HunterPetData.get(server) == live, "Invalid reload preserves live balance"); helper.succeed();
            } finally {
                server.reloadResources(packs).join();
                java.nio.file.Files.deleteIfExists(directory.resolve("hunters_pet.json")); java.nio.file.Files.deleteIfExists(pack.resolve("pack.mcmeta"));
                java.nio.file.Files.deleteIfExists(directory); java.nio.file.Files.deleteIfExists(directory.getParent());
                java.nio.file.Files.deleteIfExists(pack.resolve("data")); java.nio.file.Files.deleteIfExists(pack); server.getPackRepository().reload();
            }
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }

    /** Retains an actual native player file outside the disposable world, then verifies it in a new JVM. */
    static void restart(GameTestHelper helper) {
        var f = new Fixture(helper, 265); var p = f.ready(); var server = helper.getLevel().getServer();
        var checkpoint = java.nio.file.Path.of("hunter-pet-restart.properties");
        var retained = java.nio.file.Path.of("hunter-pet-restart-player.dat"); var expected = new Properties();
        boolean verify = "verify".equals(System.getenv("COVENANT_PET_RESTART_CHECK"));
        try {
            if (verify) {
                try (var reader = java.nio.file.Files.newBufferedReader(checkpoint)) { expected.load(reader); }
                check(!Long.toString(ProcessHandle.current().pid()).equals(expected.getProperty("process")), "Restart verification requires a fresh JVM");
                var identity = new net.minecraft.server.players.NameAndId(UUID.fromString(expected.getProperty("player")), "pet-restart");
                var path = server.getPlayerList().getPlayerIo().getPlayerDir().toPath().resolve(identity.id() + ".dat");
                java.nio.file.Files.copy(retained, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                var saved = server.getPlayerList().loadPlayerData(identity).orElseThrow(); saved.remove("UUID");
                var reporter = new ProblemReporter.Collector(); p.load(TagValueInput.create(reporter, p.registryAccess(), saved)); p.setPos(f.origin);
                check(reporter.isEmpty() && HunterPets.active(p) && HunterPets.current(p) == null, "Fresh server reads stored companion from native player file");
            } else {
                f.use(p); var pet = HunterPets.current(p); pet.setHealth(11);
                server.getPlayerList().saveAll();
                var path = server.getPlayerList().getPlayerIo().getPlayerDir().toPath().resolve(p.getUUID() + ".dat");
                java.nio.file.Files.copy(path, retained, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                expected.setProperty("player", p.getUUID().toString()); expected.setProperty("pet", pet.getUUID().toString());
                expected.setProperty("process", Long.toString(ProcessHandle.current().pid()));
                try (var writer = java.nio.file.Files.newBufferedWriter(checkpoint)) { expected.store(writer, "Hunter's Pet native player-file restart fixture"); }
            }
            helper.onEachTick(f::tick);
            helper.runAtTickTime(10, () -> {
                try {
                    var pet = HunterPets.current(p);
                    check(pet != null && pet.getUUID().toString().equals(expected.getProperty("pet")), "One original companion identity survives restart");
                    near(pet.getHealth(), 11, "Restart preserves wounded health");
                    near(ClassResources.current(p, PlayerClass.STALKER), 65, "Restart does not charge summon again");
                    check(f.companions(p) == 1, "Restart materializes exactly one pet"); helper.succeed();
                } finally { f.close(); }
            });
        } catch (java.io.IOException | RuntimeException error) { f.close(); throw new IllegalStateException("Pet restart fixture failed", error); }
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper; final Vec3 origin;
        final List<ServerPlayer> players = new ArrayList<>(); final List<Entity> entities = new ArrayList<>();
        final Map<BlockPos, net.minecraft.world.level.block.state.BlockState> blocks = new HashMap<>();
        final List<net.minecraft.world.scores.PlayerTeam> teams = new ArrayList<>();
        final List<Runnable> cleanups = new ArrayList<>();
        Fixture(GameTestHelper helper, int height) {
            this.helper = helper; origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, height, 1)));
            for (int x = -4; x <= 19; x++) { for (int z = -4; z <= 4; z++) { block(x, -1, z, Blocks.STONE); } }
        }
        ServerPlayer player() {
            var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), "pet" + UUID.randomUUID().toString().substring(0, 8)), false);
            var p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                // EmbeddedChannel is in-process but is not Netty's LocalChannel. Declare that
                // explicitly so NeoForge does not expect an external config-sync handshake.
                @Override public boolean isMemoryConnection() { return true; }
            };
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
            // Animated pets send real GeckoLib payloads to tracking players; use NeoForge's
            // fully negotiated mock setup instead of a vanilla-only embedded connection.
            net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
            p.setGameMode(GameType.SURVIVAL); p.connection.markClientLoaded(); p.setNoGravity(true); p.setPos(origin);
            p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            PlayerProfiles.selectClass(p, PlayerClass.STALKER); ClassResources.set(p, PlayerClass.STALKER, 100); players.add(p);
            p.getAttribute(RpgStats.FOCUS_REGENERATION).setBaseValue(0);
            return p;
        }
        ServerPlayer ready() { var p = player(); FixtureSkills.learn(p, SkillDefinition.HUNTERS_PET.id(), true); SkillLoadouts.bind(p, 0, SkillDefinition.HUNTERS_PET.id()); return p; }
        void use(ServerPlayer p) { resetThrottle(p); check(key(Skills.activate(p, SkillDefinition.HUNTERS_PET), "used"), "Summon accepted"); }
        <T extends Mob> T mob(EntityType<T> type, double x, double z) {
            var mob = type.create(helper.getLevel(), EntitySpawnReason.COMMAND); mob.setPos(origin.add(x, 0, z));
            mob.setNoAi(true); mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); mob.setHealth(200);
            if (mob.getAttribute(Attributes.ARMOR) != null) { mob.getAttribute(Attributes.ARMOR).setBaseValue(0); }
            mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            helper.getLevel().addFreshEntity(mob); entities.add(mob); return mob;
        }
        void block(int x, int y, int z, net.minecraft.world.level.block.Block block) {
            var pos = BlockPos.containing(origin).offset(x, y, z); blocks.putIfAbsent(pos, helper.getLevel().getBlockState(pos)); helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        }
        long companions(ServerPlayer p) { return helper.getLevel().getEntitiesOfClass(HunterPet.class, p.getBoundingBox().inflate(40), pet -> pet.isAlive() && pet.getOwner() == p).size(); }
        void tick() { for (var p : players) { p.doTick(); } }
        @Override public void close() {
            for (var p : players) { HunterPets.dismiss(p); helper.getLevel().getServer().getPlayerList().remove(p); }
            entities.forEach(Entity::discard); blocks.forEach((pos, state) -> helper.getLevel().setBlockAndUpdate(pos, state));
            teams.forEach(helper.getLevel().getScoreboard()::removePlayerTeam);
            cleanups.forEach(Runnable::run);
        }
    }
    private static void resetThrottle(ServerPlayer p) {
        try { var field = SkillRuntime.class.getDeclaredField("nextActivation"); field.setAccessible(true); field.setLong(p.getData(RpgData.SKILL_RUNTIME), 0); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    private static boolean key(Component c, String key) { return c.getContents() instanceof TranslatableContents t && t.getKey().equals("commands.covenant.skill." + key); }
    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
    private static void near(double actual, double expected, String message) { check(Math.abs(actual - expected) < .001, message + ": " + actual + " != " + expected); }
}
