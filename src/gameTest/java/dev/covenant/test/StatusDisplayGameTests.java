package dev.covenant.test;

import dev.covenant.test.fixture.TestCleanup;
import dev.covenant.test.fixture.TestPlayers;
import dev.covenant.test.fixture.FixtureSkills;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.Auras;
import dev.covenant.rpg.skills.BuffDisplay;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLifecycle;
import dev.covenant.rpg.skills.SkillLoadouts;
import dev.covenant.rpg.skills.Skills;
import dev.covenant.rpg.stats.RpgStats;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;

/** Authoritative buff receipt, consumption, wire and persistence boundaries; rendering remains a client check. */
final class StatusDisplayGameTests {
    private StatusDisplayGameTests() {}

    static void transitions(GameTestHelper helper) {
        var f = new Fixture(helper);
        try {
            var source = f.player(PlayerClass.STALKER, 0);
            var friend = f.player(null, 2); f.team(source, friend);
            f.equip(source, SkillDefinition.SWIFTNESS_AURA);
            source.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(.1);
            Auras.refresh(source);
            check(PlayerProfiles.getSelectedClass(friend).isEmpty(), "Recipient remains classless");
            var passive = row(friend, SkillDefinition.SWIFTNESS_AURA);
            check(passive.passive() && passive.expiresAt() == 0, "Classless ally receives passive aura icon state");
            var original = friend.getData(RpgData.BUFF_DISPLAY);
            Auras.refresh(source);
            check(friend.getData(RpgData.BUFF_DISPLAY) == original, "Equal passive refresh retains the existing synced snapshot");
            f.cast(source, SkillDefinition.SWIFTNESS_AURA);
            long firstDeadline = row(friend, SkillDefinition.SWIFTNESS_AURA).expiresAt();
            check(firstDeadline > ClassResources.now(source) && row(friend, SkillDefinition.SWIFTNESS_AURA).passive(),
                    "Receipt includes timed active and passive together");
            friend.setGameMode(GameType.SPECTATOR); BuffDisplay.sync(friend);
            check(row(friend, SkillDefinition.SWIFTNESS_AURA).expiresAt() == firstDeadline,
                    "Spectator visibility does not discard an unchanged authoritative active receipt");
            friend.setGameMode(GameType.SURVIVAL); BuffDisplay.sync(friend);
            check(row(friend, SkillDefinition.SWIFTNESS_AURA).expiresAt() == firstDeadline,
                    "Returning to Survival retains the buff without waiting for a new grant");
            helper.onEachTick(f::tick);
            helper.runAtTickTime(3, () -> {
                try {
                    f.cast(source, SkillDefinition.SWIFTNESS_AURA);
                    check(row(friend, SkillDefinition.SWIFTNESS_AURA).expiresAt() > firstDeadline,
                            "Reactivating an existing buff refreshes its displayed deadline");
                } catch (RuntimeException error) { f.close(); throw error; }
            });
            helper.runAtTickTime(15, () -> {
                try (f) {
                    check(!Auras.active(friend, Auras.Kind.SWIFTNESS), "Short active buff expires");
                    var retained = row(friend, SkillDefinition.SWIFTNESS_AURA);
                    check(retained.passive() && retained.expiresAt() == 0, "Expiry removes active timer but retains live passive receipt");
                    SkillLoadouts.bind(source, 0, null);
                    check(friend.getData(RpgData.BUFF_DISPLAY).equals(BuffDisplay.EMPTY), "Removing the last source clears remote buff icons");
                    helper.succeed();
                }
            });
        } catch (RuntimeException error) { f.close(); throw error; }
    }

    static void consumption(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var guard = f.player(PlayerClass.REAVER, 0);
            var scout = f.player(PlayerClass.STALKER, 1);
            var friend = f.player(null, 2); f.team(guard, scout, friend);
            f.equip(guard, SkillDefinition.FORTITUDE_AURA); Auras.refresh(guard);
            f.cast(guard, SkillDefinition.FORTITUDE_AURA);
            check(row(friend, SkillDefinition.FORTITUDE_AURA).expiresAt() > 0 && friend.getData(RpgData.FORTITUDE_CAPACITY) == 6,
                    "Shield receipt has a timer and capacity");
            friend.hurtServer(helper.getLevel(), friend.damageSources().generic(), 12);
            check(Auras.shield(friend) == 0 && friend.getData(RpgData.FORTITUDE_CAPACITY) == 0,
                    "Native damage immediately clears depleted shield capacity");
            check(row(friend, SkillDefinition.FORTITUDE_AURA).expiresAt() == 0,
                    "Depleted shield has no stale active timer while passive armor remains");
            f.equip(scout, SkillDefinition.DODGE_AURA); Auras.refresh(scout);
            f.cast(scout, SkillDefinition.DODGE_AURA);
            check(row(friend, SkillDefinition.DODGE_AURA).expiresAt() > 0, "Guaranteed Dodge has an active timer");
            check(Auras.consumeDodge(friend), "Guaranteed Dodge consumption succeeds");
            var dodge = row(friend, SkillDefinition.DODGE_AURA);
            check(dodge.passive() && dodge.expiresAt() == 0, "Consuming Dodge clears the guarantee immediately, preserving passive chance");
            SkillLifecycle.cancelPlayer(friend, SkillLifecycle.Reason.DIMENSION_CHANGE);
            check(friend.getData(RpgData.BUFF_DISPLAY).equals(BuffDisplay.EMPTY), "Recipient lifecycle change clears all transient buff icons");
            helper.succeed();
        }
    }

    @SuppressWarnings("unchecked")
    static void storage(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var owner = f.player(null, 0);
            var other = f.player(null, 2);
            var view = new BuffDisplay(37, List.of(
                    new BuffDisplay.Row(SkillDefinition.SWIFTNESS_AURA.id(), true, 1000, 0),
                    new BuffDisplay.Row(SkillDefinition.BLOOD_FEVER.id(), true, 0, 5)));
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                BuffDisplay.STREAM_CODEC.encode(buffer, view);
                check(BuffDisplay.STREAM_CODEC.decode(buffer).equals(view), "Buff view retains deadlines, presence and stacks on the wire");
                for (int count : new int[] {-1, 33}) {
                    buffer.clear(); buffer.writeLong(0); buffer.writeVarInt(count);
                    reject(() -> BuffDisplay.STREAM_CODEC.decode(buffer));
                }
            } finally { buffer.release(); }
            reject(() -> new BuffDisplay.Row(SkillDefinition.BLOOD_FEVER.id(), true, 0, 33));
            try {
                var field = RpgData.BUFF_DISPLAY.get().getClass().getDeclaredField("syncHandler"); field.setAccessible(true);
                var handler = (AttachmentSyncHandler<BuffDisplay>) field.get(RpgData.BUFF_DISPLAY.get());
                check(handler.sendToPlayer(owner, owner) && !handler.sendToPlayer(owner, other), "Native attachment sync targets only its owner");
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            owner.setData(RpgData.BUFF_DISPLAY, view);
            var reporter = new ProblemReporter.Collector();
            var output = TagValueOutput.createWithContext(reporter, owner.registryAccess());
            owner.saveWithoutId(output);
            var saved = output.buildResult(); saved.remove("UUID");
            var loaded = f.player(null, 4);
            loaded.load(TagValueInput.create(reporter, loaded.registryAccess(), saved));
            check(reporter.isEmpty() && loaded.getData(RpgData.BUFF_DISPLAY).equals(BuffDisplay.EMPTY), "Native player save/load omits transient buff display");
            check(Auras.Kind.SWIFTNESS.passiveRadius() == Auras.RADIUS
                            && Auras.Kind.SHOCK.passiveRadius() == Auras.RADIUS
                            && Auras.Kind.IMMOLATION.passiveRadius() == Auras.RADIUS,
                    "Area aura presentation exposes the authoritative six-block passive field");
            check(Auras.Kind.RAGING.passiveRadius() == 0 && Auras.Kind.ICY_VEINS.passiveRadius() == 0
                            && Auras.Kind.FLAME_CLOAK.passiveRadius() == 0 && Auras.Kind.BATTLE_CRY.passiveRadius() == 0,
                    "Self-only auras and one-shot shouts do not claim a persistent range");
            helper.succeed();
        }
    }

    private static BuffDisplay.Row row(ServerPlayer player, SkillDefinition skill) {
        return player.getData(RpgData.BUFF_DISPLAY).rows().stream().filter(row -> row.skill().equals(skill.id())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing displayed buff " + skill.id()));
    }

    private static final class Fixture implements AutoCloseable {
        private boolean cleanupComplete;
        final GameTestHelper helper;
        final Vec3 origin;
        final List<ServerPlayer> players = new ArrayList<>();
        final List<PlayerTeam> teams = new ArrayList<>();
        boolean closed;
        Fixture(GameTestHelper helper) {
            TestCleanup.onFinish(helper, this::close);
            this.helper = helper;
            origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 245, 1)));
        }
        ServerPlayer player(PlayerClass type, double z) {
            var player = TestPlayers.create(helper); players.add(player);
            try {
                var pending = player.connection.getClass().getDeclaredField("awaitingTeleport"); pending.setAccessible(true);
                player.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(pending.getInt(player.connection)));
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            player.setGameMode(GameType.SURVIVAL); player.connection.markClientLoaded();
            player.setPos(origin.add(0, 0, z)); player.connection.resetPosition(); player.setNoGravity(true);
            if (type != null) { PlayerProfiles.selectClass(player, type); }
            return player;
        }
        void team(ServerPlayer... members) {
            var board = helper.getLevel().getScoreboard(); var team = board.addPlayerTeam("s" + UUID.randomUUID().toString().substring(0, 8)); teams.add(team);
            for (var member : members) { board.addPlayerToTeam(member.getScoreboardName(), team); }
        }
        void equip(ServerPlayer player, SkillDefinition skill) {
            FixtureSkills.learn(player, skill.id(), true); SkillLoadouts.bind(player, 0, skill.id());
            Skills.setIgnoreCooldowns(player, true); Auras.reconcile(player);
        }
        void cast(ServerPlayer player, SkillDefinition skill) {
            ClassResources.set(player, PlayerProfiles.getSelectedClass(player).orElseThrow(), 100);
            var result = Skills.activate(player, skill);
            check(result.getContents() instanceof TranslatableContents value && value.getKey().equals("commands.covenant.skill.used"), "Aura activation accepted: " + result.getString());
        }
        void tick() {
            if (closed) { return; }
            try { players.forEach(ServerPlayer::doTick); }
            catch (RuntimeException error) { close(); throw error; }
        }
        @Override public void close() {
            if (cleanupComplete) { return; }
            cleanupComplete = true;
            if (closed) { return; } closed = true;
            for (var player : players) {
                SkillLifecycle.cancelPlayer(player, SkillLifecycle.Reason.LOGOUT);
                helper.getLevel().getServer().getPlayerList().remove(player);
            }
            teams.forEach(helper.getLevel().getScoreboard()::removePlayerTeam);
        }
    }
    private static void reject(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new IllegalStateException("Expected invalid display payload to be rejected");
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
}
