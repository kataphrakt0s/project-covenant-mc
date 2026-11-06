package dev.covenant.test;

import com.mojang.authlib.GameProfile;
import dev.covenant.mixin.ArrowPiercingAccessor;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.AuraDamage;
import dev.covenant.rpg.skills.Auras;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.ProjectileCombat;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLoadouts;
import dev.covenant.rpg.skills.Skills;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Native entity serialization followed by native collision and damage, including piercing history. */
final class ProjectilePersistenceGameTests {
    private ProjectilePersistenceGameTests() {}

    static void run(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var shooter = fixture.player(PlayerClass.REAVER, 0);
            var enchanter = fixture.player(PlayerClass.WEAVER, 1);
            var board = helper.getLevel().getScoreboard();
            var team = board.addPlayerTeam("shot" + UUID.randomUUID().toString().substring(0, 8));
            fixture.team = team;
            board.addPlayerToTeam(shooter.getScoreboardName(), team);
            board.addPlayerToTeam(enchanter.getScoreboardName(), team);
            fixture.aura(shooter, SkillDefinition.RAGING_AURA);
            fixture.aura(enchanter, SkillDefinition.ENCHANTING_AURA);
            var target = fixture.target(4);
            var other = fixture.target(8);

            var arrow = fixture.arrow(shooter);
            AuraDamage.snapshot(arrow, shooter, 1);
            near(arrow.getData(RpgData.AURA_PROJECTILE_POWER), 1.2, "Firing captures Raging power");
            near(arrow.getData(RpgData.ENCHANTED_SHOT), 2.4, "Firing captures Enchanting with Raging");
            arrow = fixture.reload(arrow);
            check(arrow.getOwner() == shooter, "Native owner resolves after reload");
            near(((ArrowPiercingAccessor) arrow).covenant$getBaseDamage(), 6, "Native arrow base retains captured power once");
            fixture.impact(arrow, target);
            near(target.getHealth(), 191.6, "Reloaded impact retains six native plus captured 2.4 magic damage");
            near(ClassResources.current(shooter, PlayerClass.REAVER), 6, "Ordinary arrow still grants native-hit Rage");

            // Replacing the native arrow loses vanilla's transient piercing ID set. Our own bonus
            // history must still prevent a second award for the same saved target UUID.
            arrow = fixture.reload(arrow);
            fixture.impact(arrow, target);
            near(target.getHealth(), 185.6, "Reloaded piercing shot cannot repeat Enchanting on its prior target");
            fixture.impact(arrow, other);
            near(other.getHealth(), 191.6, "Another target still receives the captured bonus");

            var skillArrow = fixture.arrow(shooter);
            AuraDamage.snapshot(skillArrow, shooter, 1);
            ProjectileCombat.markSkill(skillArrow);
            skillArrow.setData(RpgData.SKILL_SHOT, true);
            skillArrow = fixture.reload(skillArrow);
            check(ProjectileCombat.isSkill(skillArrow), "Saved skill provenance survives reload");
            check(!skillArrow.hasData(RpgData.SKILL_SHOT), "The visual trail remains transient");
            ClassResources.set(shooter, PlayerClass.REAVER, 0);
            fixture.impact(skillArrow, other);
            near(other.getHealth(), 185.6, "Reloaded skill arrow excludes ordinary-shot Enchanting");
            near(ClassResources.current(shooter, PlayerClass.REAVER), 0, "Reloaded skill arrow cannot refund Rage");

            var legacy = fixture.reload(fixture.arrow(shooter));
            check(!legacy.hasData(RpgData.AURA_PROJECTILE_POWER) && !legacy.hasData(RpgData.PROJECTILE_COMBAT),
                    "Missing old metadata is not invented during native load");
            check(!ProjectileCombat.isSkill(legacy), "Unknown legacy provenance uses ordinary-shot behavior");
            near(((ArrowPiercingAccessor) legacy).covenant$getBaseDamage(), 5, "Legacy native base damage is untouched");
            check(fixture.reporter.isEmpty(), "Native projectile save/load reports no errors");
            helper.succeed();
        }
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final ProblemReporter.Collector reporter = new ProblemReporter.Collector();
        final List<ServerPlayer> players = new ArrayList<>();
        final List<Entity> entities = new ArrayList<>();
        final Vec3 origin;
        net.minecraft.world.scores.PlayerTeam team;

        Fixture(GameTestHelper helper) {
            this.helper = helper;
            origin = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 125, 2)));
        }

        ServerPlayer player(PlayerClass type, double z) {
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),
                    "shot" + UUID.randomUUID().toString().substring(0, 8)), false);
            var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            players.add(player);
            player.setGameMode(GameType.SURVIVAL); player.connection.markClientLoaded();
            player.setPos(origin.add(0, 0, z)); player.setNoGravity(true);
            PlayerProfiles.selectClass(player, type);
            return player;
        }

        void aura(ServerPlayer player, SkillDefinition skill) {
            SkillLoadouts.learn(player, skill.id(), true);
            SkillLoadouts.bind(player, 0, skill.id());
            ClassResources.set(player, PlayerProfiles.getSelectedClass(player).orElseThrow(), 100);
            check(Skills.activate(player, skill).getString().contains("Used"), "Required aura activates");
            ClassResources.set(player, PlayerProfiles.getSelectedClass(player).orElseThrow(), 0);
        }

        LivingEntity target(double z) {
            var target = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            target.setPos(origin.add(0, 0, z)); target.setNoGravity(true); target.setNoAi(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            target.setHealth(200);
            helper.getLevel().addFreshEntity(target); entities.add(target);
            return target;
        }

        Arrow arrow(ServerPlayer shooter) {
            var arrow = new Arrow(helper.getLevel(), shooter, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            arrow.setBaseDamage(5); arrow.setNoGravity(true); arrow.setCritArrow(false);
            ((ArrowPiercingAccessor) arrow).covenant$setPierceLevel((byte) 3);
            entities.add(arrow);
            return arrow;
        }

        Arrow reload(Arrow original) {
            var output = TagValueOutput.createWithContext(reporter, helper.getLevel().registryAccess());
            original.saveWithoutId(output);
            var saved = output.buildResult();
            original.discard();
            var loaded = EntityType.ARROW.create(helper.getLevel(), EntitySpawnReason.LOAD);
            loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), saved));
            check(helper.getLevel().addFreshEntity(loaded), "Reloaded arrow enters the native level");
            entities.add(loaded);
            return loaded;
        }

        void impact(Arrow arrow, LivingEntity target) {
            target.invulnerableTime = 0;
            arrow.setPos(target.position().add(0, 1, -1));
            arrow.setDeltaMovement(0, 0, 1);
            arrow.tick();
            check(!arrow.isRemoved(), "Native piercing arrow remains after its impact");
        }

        @Override public void close() {
            for (var player : players) { Auras.clear(player); helper.getLevel().getServer().getPlayerList().remove(player); }
            entities.forEach(Entity::discard);
            if (team != null) { helper.getLevel().getScoreboard().removePlayerTeam(team); }
        }
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < .001, message + ": " + actual + " != " + expected);
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
    }
}
