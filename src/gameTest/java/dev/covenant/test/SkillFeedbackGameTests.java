package dev.covenant.test;

import com.mojang.authlib.GameProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.ProjectileCombat;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLifecycle;
import dev.covenant.rpg.skills.Skills;
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

/** Native sound emission boundaries; these checks do not establish audible or rendered client quality. */
final class SkillFeedbackGameTests {
    private SkillFeedbackGameTests() {}

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
            check(key(Skills.activate(whiff, SkillDefinition.CLEAVE), "used"), "Whiff is still an accepted strike");
            check(f.soundsAt(whiff) == 1, "Whiff has a swing cue but no damage confirmation");

            var striker = f.player(PlayerClass.REAVER, SkillDefinition.CLEAVE);
            var first = f.target(striker.position().add(-.5, 0, 2));
            var second = f.target(striker.position().add(.5, 0, 2));
            float firstHealth = first.getHealth(), secondHealth = second.getHealth();
            check(key(Skills.activate(striker, SkillDefinition.CLEAVE), "used"), "Area strike accepted");
            check(first.getHealth() < firstHealth && second.getHealth() < secondHealth, "Both in-arc targets receive damage");
            check(f.soundsAt(striker) == 2, "Multi-target strike adds one aggregate damage confirmation");

            var blocked = f.player(PlayerClass.REAVER, SkillDefinition.CLEAVE);
            var immune = f.target(blocked.position().add(0, 0, 2)); immune.setInvulnerable(true);
            float immuneHealth = immune.getHealth();
            check(key(Skills.activate(blocked, SkillDefinition.CLEAVE), "used"), "Protected target does not reject the swing");
            near(immune.getHealth(), immuneHealth, "Protected target receives no damage");
            check(f.soundsAt(blocked) == 1, "Fully prevented damage produces no success cue");
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
            helper.onEachTick(f::tickPlayers);
            helper.runAtTickTime(15, () -> {
                try (f) {
                    check(!Skills.isCasting(completed), "Normal cast completes");
                    check(f.soundsAt(completed) == 2, "Completion emits one release after its single start cue");
                    check(f.soundsAt(cancelled) == 1, "Cancelled cast never emits its release cue");
                    near(ClassResources.current(completed, PlayerClass.WEAVER), 80, "Completed cast spends normal Mana");
                    near(ClassResources.current(cancelled, PlayerClass.WEAVER), 100, "Cancelled cast preserves Mana");
                    check(Skills.cooldownRemaining(completed, SkillDefinition.ARCANE_BOLT) > 0, "Completion retains cooldown");
                    check(Skills.cooldownRemaining(cancelled, SkillDefinition.ARCANE_BOLT) == 0, "Cancellation commits no cooldown");
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

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final Vec3 origin;
        final List<ServerPlayer> players = new ArrayList<>();
        final List<Entity> entities = new ArrayList<>();
        final List<BlockPos> blocks = new ArrayList<>();
        final List<Vec3> sounds = new ArrayList<>();
        final Consumer<PlayLevelSoundEvent.AtPosition> listener = this::sound;
        boolean closed;

        Fixture(GameTestHelper helper, int height) {
            this.helper = helper;
            var pos = helper.absolutePos(new BlockPos(2, height, 2));
            origin = new Vec3(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            NeoForge.EVENT_BUS.addListener(listener);
        }

        ServerPlayer player(PlayerClass playerClass, SkillDefinition skill) {
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "feedback-test"), false);
            var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
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

        LivingEntity target(Vec3 position) {
            var target = EntityType.COW.create(helper.getLevel(), EntitySpawnReason.COMMAND);
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
