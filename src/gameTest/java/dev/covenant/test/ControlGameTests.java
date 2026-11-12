package dev.covenant.test;

import dev.covenant.test.fixture.TestChunks;
import dev.covenant.test.fixture.TestCleanup;
import dev.covenant.test.fixture.TestPlayers;
import dev.covenant.test.fixture.FixtureSkills;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.Auras;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.Combos;
import dev.covenant.rpg.skills.DashMotion;
import dev.covenant.rpg.skills.DashPrediction;
import dev.covenant.rpg.skills.Rooted;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLoadouts;
import dev.covenant.rpg.skills.Skills;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Exercises the native packet and enclosing connection tick boundaries, not only input helper methods. */
final class ControlGameTests {
    private ControlGameTests() {}

    static void movement(GameTestHelper helper) {
        var fixture = new Fixture(helper, 280);
        fixture.runWhenReady(() -> {
        try (fixture) {
            for (boolean frozen : new boolean[] {false, true}) {
                var player = fixture.player();
                fixture.floor(player);
                if (frozen) { fixture.freeze(player); } else { Rooted.apply(player, 100); }
                Vec3 start = player.position();
                for (int i = 0; i < 6; i++) {
                    player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(
                            start.add(.1, .1, 0), 25, 10, false, false));
                    near(player.position().distanceTo(start), 0, "Untrusted small walking/jump packets cannot move a controlled player");
                }
                near(player.getYRot(), 25, "Controlled players may look around");
                player.setNoGravity(false);
                player.setDeltaMovement(.3, .3, 0);
                player.connection.tick();
                check(player.getX() > start.x + .2 && player.getY() > start.y + .2,
                        "Native external impulse survives the full connection tick");
                Vec3 afterImpulse = player.position();
                player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(start, 25, 10, false, false));
                check(player.position().equals(afterImpulse), "A stale packet cannot rewind an external impulse");
                fixture.acceptTeleport(player);
                player.setDeltaMovement(0, -.3, 0);
                player.connection.tick();
                check(player.getY() < afterImpulse.y, "Controlled native gravity/falling remains active");
                player.setDeltaMovement(Vec3.ZERO);
                player.setNoGravity(true);
                if (frozen) { Auras.clear(player); } else { player.removeEffect(Rooted.EFFECT); }
                fixture.acceptTeleport(player);
                Vec3 released = player.position();
                player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(released.add(0, 0, .1), 0, 0, false, false));
                near(player.getZ(), released.z + .1, "Ordinary position handling resumes when control ends");
            }

            var delayed = fixture.player(); Rooted.apply(delayed, 100);
            Vec3 origin = delayed.position();
            delayed.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(origin.add(2, 0, 0), 0, 0, false, false));
            int correction = (int) field(delayed.connection, "awaitingTeleport");
            check(field(delayed.connection, "awaitingPositionFromClient") != null, "Control divergence uses native teleport acknowledgement");
            delayed.setDeltaMovement(.15, .2, 0);
            for (int i = 0; i < 6; i++) {
                delayed.connection.tick();
                delayed.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(origin.add(2, 0, 0), 0, 0, false, false));
                check((int) field(delayed.connection, "awaitingTeleport") == correction, "Pending correction ID is not replaced by packet traffic");
            }
            Vec3 moved = delayed.position();
            check(moved.distanceToSqr(origin) > .1, "External physics continues while correction acknowledgement is delayed");
            fixture.acceptTeleport(delayed);
            check(delayed.position().equals(moved), "Delayed correction acknowledgement does not rewind authoritative physics");
            check(field(delayed.connection, "awaitingPositionFromClient") == null, "Native acknowledgement clears its pending teleport");

            Vec3 teleported = moved.add(0, 0, 3);
            delayed.connection.teleport(teleported.x, teleported.y, teleported.z, 0, 0);
            delayed.setPos(teleported.add(1, 0, 0));
            fixture.acceptTeleport(delayed);
            check(delayed.position().equals(teleported), "An unrelated native teleport retains its own authoritative endpoint");
            helper.succeed();
        }
        });
    }

    static void actions(GameTestHelper helper) {
        var fixture = new Fixture(helper, 295);
        fixture.runWhenReady(() -> {
        try (fixture) {
            var player = fixture.player();
            fixture.freeze(player);
            for (var item : List.of(Items.SPLASH_POTION, Items.ENDER_PEARL, Items.SNOWBALL)) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
                long before = fixture.projectiles(player);
                player.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 1, 0, 0));
                check(fixture.projectiles(player) == before && player.getMainHandItem().getCount() == 2,
                        "Freeze rejects native instant-use packet before spawn or consumption: " + item);
            }
            var victim = fixture.target(player.position().add(0, 0, 2.5));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SPEAR));
            setField(player, "attackStrengthTicker", 100);
            var stab = new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STAB, player.blockPosition(), Direction.UP);
            player.connection.handlePlayerAction(stab);
            near(victim.getHealth(), 200, "Freeze rejects native spear STAB packets");
            Auras.clear(player);
            Rooted.apply(player, 60);
            setField(player, "attackStrengthTicker", 100);
            player.connection.handlePlayerAction(stab);
            check(victim.getHealth() < 200, "The same native spear packet damages normally after freeze ends");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SNOWBALL, 2));
            long before = fixture.projectiles(player);
            player.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 2, 0, 0));
            check(fixture.projectiles(player) == before + 1 && player.getMainHandItem().getCount() == 1,
                    "Root still permits item actions through the same packet path");
            helper.succeed();
        }
        });
    }

    static void dashHandoff(GameTestHelper helper) {
        var fixture = new Fixture(helper, 310);
        fixture.runWhenReady(() -> {
        try (fixture) {
            var prediction = new DashPrediction();
            var motion = new DashMotion(1, helper.getLevel().dimension().identifier(), Vec3.ZERO, new Vec3(0, 0, 1), 3, false, Vec3.ZERO);
            prediction.accept(motion, Vec3.ZERO); prediction.cancel();
            check(!prediction.ownsTravel() && prediction.delta(new Vec3(30, 0, 0)).equals(Vec3.ZERO),
                    "Native teleport cancellation cannot converge toward an obsolete dash endpoint");
            check(!prediction.accept(motion, new Vec3(30, 0, 0)), "A canceled prediction retains sequence protection");
            var player = fixture.player();
            PlayerProfiles.selectClass(player, PlayerClass.REAVER);
            FixtureSkills.learn(player, SkillDefinition.LUNGING_COMBO.id(), true);
            ClassResources.set(player, PlayerClass.REAVER, 100);
            Skills.activate(player, SkillDefinition.LUNGING_COMBO);
            for (int i = 0; i < 3; i++) {
                var runtime = player.getData(RpgData.SKILL_RUNTIME);
                Object movement = field(runtime, "movement");
                if (movement != null) { setField(movement, "lastTick", Long.MIN_VALUE); }
                player.connection.tick();
            }
            check(Combos.awaitingMovement(player), "Completed dash awaits the owner acknowledgement");
            Vec3 endpoint = player.position();
            player.move(MoverType.PISTON, new Vec3(.2, 0, 0));
            check(player.getX() > endpoint.x + .1, "Unacknowledged endpoint allows authoritative piston displacement");
            player.setNoGravity(false);
            player.setDeltaMovement(.2, .25, 0);
            player.connection.tick();
            check(player.getX() > endpoint.x + .25 && player.getY() > endpoint.y + .2,
                    "Unacknowledged endpoint applies knockback through native travel");
            Vec3 moved = player.position();
            player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(endpoint, 0, 0, false, false));
            check(player.position().equals(moved), "Late dash reports cannot overwrite external displacement");
            long sequence = (long) field(player.getData(RpgData.SKILL_RUNTIME), "awaitingDashSequence");
            Combos.acknowledgeMovement(player, sequence, endpoint);
            check(!Combos.blocksMove(player) && field(player.connection, "awaitingPositionFromClient") != null,
                    "Stale endpoint releases the dash hold through one native correction");
            player.setDeltaMovement(.15, .2, 0); player.connection.tick(); moved = player.position();
            fixture.acceptTeleport(player);
            check(player.position().equals(moved), "Dash correction acknowledgement retains subsequent external physics");
            player.setDeltaMovement(Vec3.ZERO);
            player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(moved.add(0, 0, .1), 0, 0, false, false));
            near(player.getZ(), moved.z + .1, "Walking resumes after a physically moved dash endpoint");

            var invalid = fixture.player(); PlayerProfiles.selectClass(invalid, PlayerClass.REAVER);
            FixtureSkills.learn(invalid, SkillDefinition.LUNGING_COMBO.id(), true);
            ClassResources.set(invalid, PlayerClass.REAVER, 100); Skills.activate(invalid, SkillDefinition.LUNGING_COMBO);
            invalid.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(new Vec3(Double.NaN, 0, 0), 0, 0, false, false));
            check(!invalid.connection.isAcceptingMessages(), "Active dash does not bypass vanilla invalid-coordinate disconnection");
            helper.succeed();
        }
        });
    }

    private static Object field(Object owner, String name) {
        try { var field = findField(owner.getClass(), name); field.setAccessible(true); return field.get(owner); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }

    private static void setField(Object owner, String name, Object value) {
        try { var field = findField(owner.getClass(), name); field.setAccessible(true); field.set(owner, value); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }

    private static java.lang.reflect.Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try { return current.getDeclaredField(name); } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < .001, message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean value, String message) { if (!value) { throw new IllegalStateException(message); } }

    private static final class Fixture implements AutoCloseable {
        private boolean cleanupComplete;
        // Independent fixture ownership prevents another fixture from removing our shared lane's ticket.
        final TestChunks laneTicket;
        final GameTestHelper helper;
        final List<ServerPlayer> players = new ArrayList<>();
        final List<BlockPos> blocks = new ArrayList<>();
        final List<LivingEntity> targets = new ArrayList<>();
        final java.util.Set<net.minecraft.world.level.ChunkPos> forced = new java.util.HashSet<>();
        final Vec3 origin;

        Fixture(GameTestHelper helper, int height) {
            laneTicket = new TestChunks(helper);
            TestCleanup.onFinish(helper, this::close);
            this.helper = helper;
            var position = helper.absolutePos(new BlockPos(2, height, 2));
            origin = Vec3.atBottomCenterOf(position);
        }

        void runWhenReady(Runnable test) {
            // Player lanes and the spear/projectile targets extend beyond the tiny template.
            var chunks = new ArrayList<net.minecraft.world.level.ChunkPos>();
            for (int x = net.minecraft.util.Mth.floor(origin.x - 4) >> 4; x <= net.minecraft.util.Mth.floor(origin.x + 40) >> 4; x++) {
                for (int z = net.minecraft.util.Mth.floor(origin.z - 4) >> 4; z <= net.minecraft.util.Mth.floor(origin.z + 12) >> 4; z++) {
                    var chunk = new net.minecraft.world.level.ChunkPos(x, z); chunks.add(chunk);
                    if (forced.add(chunk)) { laneTicket.load(chunk); }
                }
            }
            helper.runBeforeTestEnd(() -> {
                close();
                helper.fail("Control fixture chunks did not become entity-ticking before timeout");
            });
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                    chunks.stream().allMatch(helper.getLevel()::areEntitiesActuallyLoadedAndTicking),
                    "Control fixture chunks must be loaded and entity-ticking")).thenExecute(test);
        }

        ServerPlayer player() {
            var player = TestPlayers.create(helper);
            acceptTeleport(player);
            player.setPos(origin.add(players.size() * 12, 0, 0)); players.add(player);
            player.connection.resetPosition(); player.connection.markClientLoaded();
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setYRot(0); player.setXRot(0);
            return player;
        }

        void freeze(ServerPlayer target) {
            var source = player(); source.setPos(target.position().add(1, 0, 0)); source.connection.resetPosition();
            PlayerProfiles.selectClass(source, PlayerClass.WEAVER);
            FixtureSkills.learn(source, SkillDefinition.ICY_VEINS.id(), true);
            FixtureSkills.learn(source, SkillDefinition.FREEZING_AURA.id(), true);
            SkillLoadouts.bind(source, 0, SkillDefinition.FREEZING_AURA.id());
            check(helper.getLevel().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox()).contains(target),
                    "Freeze fixture target is available to native entity queries");
            Skills.activate(source, SkillDefinition.FREEZING_AURA);
            check(Auras.frozen(target), "Freeze fixture applies");
        }

        void acceptTeleport(ServerPlayer player) {
            if (field(player.connection, "awaitingPositionFromClient") != null) {
                player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket((int) field(player.connection, "awaitingTeleport")));
            }
        }

        LivingEntity target(Vec3 position) {
            var target = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            target.setPos(position); target.setNoAi(true); target.setNoGravity(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); target.setHealth(200);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0); helper.getLevel().addFreshEntity(target); targets.add(target);
            return target;
        }

        void floor(ServerPlayer player) {
            for (int x = -2; x <= 3; x++) { for (int z = -2; z <= 2; z++) {
                var position = BlockPos.containing(player.position().add(x, -1, z));
                helper.getLevel().setBlockAndUpdate(position, Blocks.STONE.defaultBlockState()); blocks.add(position);
            } }
        }

        long projectiles(ServerPlayer player) {
            return helper.getLevel().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(4)).size();
        }

        @Override public void close() {
            if (cleanupComplete) { return; }
            cleanupComplete = true;
            targets.forEach(LivingEntity::discard);
            for (var player : players) {
                helper.getLevel().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(6)).forEach(Projectile::discard);
                if (helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player) {
                    helper.getLevel().getServer().getPlayerList().remove(player);
                }
            }
            blocks.forEach(position -> helper.getLevel().setBlockAndUpdate(position, Blocks.AIR.defaultBlockState()));
            laneTicket.close(); forced.clear();
        }
    }
}
