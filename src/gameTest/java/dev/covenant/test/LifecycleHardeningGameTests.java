package dev.covenant.test;

import dev.covenant.test.fixture.TestChunks;
import dev.covenant.test.fixture.TestCleanup;
import dev.covenant.test.fixture.TestPlayers;
import dev.covenant.test.fixture.FixtureSkills;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.ArmorShred;
import dev.covenant.rpg.skills.Auras;
import dev.covenant.rpg.skills.Bleeding;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.Combos;
import dev.covenant.rpg.skills.Rooted;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLifecycle;
import dev.covenant.rpg.skills.SkillLoadouts;
import dev.covenant.rpg.skills.Skills;
import dev.covenant.rpg.skills.TrapEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Exercises the completed native death decision, including the independent ServerPlayer override. */
final class LifecycleHardeningGameTests {
    private LifecycleHardeningGameTests() {}

    static void deathDecision(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var caster = f.player(PlayerClass.WEAVER);
            f.learn(caster, SkillDefinition.FLAME_CLOAK, SkillDefinition.ARCANE_BOLT);
            SkillLoadouts.bind(caster, 0, SkillDefinition.FLAME_CLOAK.id());
            used(caster, SkillDefinition.FLAME_CLOAK);
            ready(caster);
            check(key(Skills.activate(caster, SkillDefinition.ARCANE_BOLT), "started"), "Caster begins wind-up");

            var combo = f.player(PlayerClass.REAVER);
            f.learn(combo, SkillDefinition.LUNGING_COMBO);
            used(combo, SkillDefinition.LUNGING_COMBO);
            var progress = combo.getData(RpgData.COMBO_DISPLAY);

            var trapper = f.player(PlayerClass.STALKER);
            f.learn(trapper, SkillDefinition.BEAR_TRAP);
            used(trapper, SkillDefinition.BEAR_TRAP);
            var trap = helper.getLevel().getEntitiesOfClass(TrapEntity.class,
                    trapper.getBoundingBox().inflate(4), entity -> !entity.isRemoved()).getFirst();
            var mob = f.mob();
            for (var target : List.of(caster, mob)) {
                Rooted.apply(target, 100);
                ArmorShred.apply(target, combo, 100);
                Bleeding.apply(target, combo, 1, 100);
            }
            var survivors = List.<LivingEntity>of(caster, combo, trapper, mob);
            Consumer<LivingDeathEvent> reviveLate = event -> {
                if (survivors.contains(event.getEntity())) {
                    event.getEntity().setHealth(event.getEntity().getMaxHealth());
                    event.setCanceled(true);
                }
            };
            // Registered after the mod's listeners at the latest priority: cleanup must await dispatch completion.
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, reviveLate);
            try {
                survivors.forEach(LifecycleHardeningGameTests::lethal);
                check(survivors.stream().allMatch(LivingEntity::isAlive), "Late death cancellation revives every fixture");
                check(Skills.isCasting(caster), "Cancelled death preserves pending cast");
                check(Auras.active(caster, Auras.Kind.FLAME_CLOAK), "Cancelled death preserves aura grants");
                check(Combos.moving(combo) && combo.getData(RpgData.COMBO_DISPLAY).equals(progress),
                        "Cancelled death preserves combo movement and progression");
                check(Skills.cooldownRemaining(combo, SkillDefinition.LUNGING_COMBO) == 0,
                        "Cancelled death cannot commit deferred combo cooldown");
                check(!trap.isRemoved(), "Cancelled death preserves owned trap");
                for (var target : List.of(caster, mob)) {
                    check(Rooted.contains(target) && target.hasEffect(ArmorShred.EFFECT) && Bleeding.count(target) == 1,
                            "Cancelled death preserves target-owned root, shred, and wounds");
                }
            } finally { NeoForge.EVENT_BUS.unregister(reviveLate); }

            survivors.forEach(LifecycleHardeningGameTests::lethal);
            check(!Skills.isCasting(caster) && !caster.hasData(RpgData.AURAS), "Confirmed death cancels casts and auras immediately");
            check(trap.isRemoved(), "Confirmed death removes owned trap");
            check(!Combos.moving(combo) && combo.getData(RpgData.COMBO_DISPLAY).rows().isEmpty(),
                    "Confirmed death clears combo movement and display");
            check(Skills.cooldownRemaining(combo, SkillDefinition.LUNGING_COMBO) == 240,
                    "Confirmed death commits deferred cooldown before cloning");
            for (var target : List.of(caster, mob)) {
                check(!Rooted.contains(target) && !target.hasEffect(ArmorShred.EFFECT) && Bleeding.count(target) == 0,
                        "Confirmed death clears target-owned statuses in both native die paths");
            }
            var replacement = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    combo.getGameProfile(), combo.clientInformation());
            replacement.connection = combo.connection;
            replacement.restoreFrom(combo, false);
            check(Skills.cooldownRemaining(replacement, SkillDefinition.LUNGING_COMBO) == 240
                    && replacement.getData(RpgData.COMBO_DISPLAY).rows().isEmpty(),
                    "Native death copy preserves committed cooldown without transient progression");
            helper.succeed();
        }
    }

    static void cancellationRoutes(GameTestHelper helper) {
        var f = new Fixture(helper);
        f.forceLanes();
        helper.runBeforeTestEnd(f::close);
        helper.startSequence().thenWaitUntil(() -> {
            for (var chunk : f.forced) {
                helper.assertTrue(helper.getLevel().areEntitiesActuallyLoadedAndTicking(chunk), "Lifecycle fixture lanes must be entity-ticking");
            }
        }).thenExecute(() -> {
        try (f) {
            for (int route = 0; route < 3; route++) {
                var combo = f.player(PlayerClass.REAVER);
                f.learn(combo, SkillDefinition.LUNGING_COMBO, SkillDefinition.FORTITUDE_AURA);
                SkillLoadouts.bind(combo, 0, SkillDefinition.FORTITUDE_AURA.id());
                used(combo, SkillDefinition.FORTITUDE_AURA);
                ready(combo);
                used(combo, SkillDefinition.LUNGING_COMBO);
                var target = f.mob();
                Bleeding.apply(target, combo, 1, 100);
                Bleeding.apply(combo, target, 1, 100);
                cancel(combo, route);
                check(!Combos.moving(combo) && combo.getData(RpgData.COMBO_DISPLAY).rows().isEmpty()
                        && !combo.hasData(RpgData.AURAS), "Central route clears movement and aura state: " + route);
                check(Skills.cooldownRemaining(combo, SkillDefinition.LUNGING_COMBO) == 240,
                        "Central route commits deferred cooldown: " + route);
                check(Bleeding.count(target) == 1 && Bleeding.count(combo) == 1,
                        "Caster departure preserves both remote and recipient target-owned wounds: " + route);

                var caster = f.player(PlayerClass.WEAVER);
                f.learn(caster, SkillDefinition.ARCANE_BOLT);
                check(key(Skills.activate(caster, SkillDefinition.ARCANE_BOLT), "started"), "Begin route cast");
                cancel(caster, route);
                check(!Skills.isCasting(caster), "Central route cancels pending casts: " + route);

                var trapper = f.player(PlayerClass.STALKER);
                f.learn(trapper, SkillDefinition.BEAR_TRAP);
                used(trapper, SkillDefinition.BEAR_TRAP);
                var traps = helper.getLevel().getEntitiesOfClass(TrapEntity.class,
                        trapper.getBoundingBox().inflate(4), entity -> !entity.isRemoved());
                check(!traps.isEmpty(), "Deployed fixture trap is queryable at route " + route + " and " + trapper.position());
                var trap = traps.getFirst();
                cancel(trapper, route);
                check(trap.isRemoved(), "Central route removes owned traps: " + route);
            }
            var unlearned = f.player(PlayerClass.WEAVER);
            f.learn(unlearned, SkillDefinition.ARCANE_BOLT);
            check(key(Skills.activate(unlearned, SkillDefinition.ARCANE_BOLT), "started"), "Begin unlearning cast");
            FixtureSkills.learn(unlearned, SkillDefinition.ARCANE_BOLT.id(), true);
            SkillLoadouts.bind(unlearned, 7, null);
            check(Skills.isCasting(unlearned), "No-op learn and binding requests preserve pending activity");
            FixtureSkills.learn(unlearned, SkillDefinition.ARCANE_BOLT.id(), false);
            check(!Skills.isCasting(unlearned), "Actual unlearning routes pending-cast cancellation");
            helper.succeed();
        } catch (RuntimeException error) { helper.fail(error.toString()); }
        });
    }

    private static void cancel(ServerPlayer player, int route) {
        switch (route) {
            case 0 -> PlayerProfiles.switchClass(player, PlayerClass.WEAVER == PlayerProfiles.getSelectedClass(player).orElseThrow()
                    ? PlayerClass.REAVER : PlayerClass.WEAVER);
            case 1 -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
            case 2 -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player, Level.OVERWORLD, Level.NETHER));
            default -> throw new IllegalArgumentException("Unknown lifecycle route");
        }
    }

    private static void lethal(LivingEntity target) {
        target.invulnerableTime = 0;
        target.hurtServer((net.minecraft.server.level.ServerLevel) target.level(), target.damageSources().genericKill(), Float.MAX_VALUE);
    }

    private static void ready(ServerPlayer player) {
        try {
            var field = player.getData(RpgData.SKILL_RUNTIME).getClass().getDeclaredField("nextActivation");
            field.setAccessible(true);
            field.setLong(player.getData(RpgData.SKILL_RUNTIME), 0);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    private static void used(ServerPlayer player, SkillDefinition skill) {
        check(key(Skills.activate(player, skill), "used"), "Accepted fixture skill: " + skill.id());
    }

    private static boolean key(Component result, String suffix) {
        return result.getContents() instanceof TranslatableContents text
                && text.getKey().equals("commands.covenant.skill." + suffix);
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
    }

    private static final class Fixture implements AutoCloseable {
        private boolean cleanupComplete;
        final GameTestHelper helper;
        final List<ServerPlayer> players = new ArrayList<>();
        final List<Entity> entities = new ArrayList<>();
        final java.util.Set<net.minecraft.world.level.ChunkPos> forced = new java.util.HashSet<>();
        final TestChunks laneTicket;
        boolean closed;
        final Vec3 origin;

        Fixture(GameTestHelper helper) {
            laneTicket = new TestChunks(helper);
            TestCleanup.onFinish(helper, this::close);
            this.helper = helper;
            origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 130, 1)));
        }
        void forceLanes() {
            // All three routes need native entity queries in lanes beyond the template's forced chunks.
            for (int x = net.minecraft.util.Mth.floor(origin.x - 4) >> 4; x <= net.minecraft.util.Mth.floor(origin.x + 128) >> 4; x++) {
                for (int z = net.minecraft.util.Mth.floor(origin.z - 4) >> 4; z <= net.minecraft.util.Mth.floor(origin.z + 24) >> 4; z++) {
                    var chunk = new net.minecraft.world.level.ChunkPos(x, z);
                    if (forced.add(chunk)) { laneTicket.load(chunk); }
                }
            }
        }

        ServerPlayer player(PlayerClass type) {
            var player = TestPlayers.create(helper);
            player.setGameMode(GameType.SURVIVAL);
            player.connection.markClientLoaded();
            player.setPos(origin.add(players.size() * 12, 0, 0));
            player.setNoGravity(true);
            player.setYRot(0);
            player.setXRot(0);
            PlayerProfiles.selectClass(player, type);
            ClassResources.set(player, type, 100);
            players.add(player);
            return player;
        }

        LivingEntity mob() {
            var target = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            target.setPos(origin.add(0, 0, 12 + entities.size() * 3));
            target.setNoGravity(true);
            target.setNoAi(true);
            helper.getLevel().addFreshEntity(target);
            entities.add(target);
            return target;
        }

        void learn(ServerPlayer player, SkillDefinition... skills) {
            for (var skill : skills) { FixtureSkills.learn(player, skill.id(), true); }
        }

        @Override public void close() {
            if (cleanupComplete) { return; }
            cleanupComplete = true;
            if (closed) { return; }
            closed = true;
            for (var player : players) {
                SkillLifecycle.cancelPlayer(player, SkillLifecycle.Reason.LOGOUT);
                helper.getLevel().getServer().getPlayerList().remove(player);
            }
            entities.forEach(Entity::discard);
            laneTicket.close(); forced.clear();
        }
    }
}
