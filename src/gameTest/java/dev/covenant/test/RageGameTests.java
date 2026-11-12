package dev.covenant.test;

import dev.covenant.test.fixture.TestPlayers;
import dev.covenant.test.fixture.FixtureSkills;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.Bleeding;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLoadouts;
import dev.covenant.rpg.skills.Skills;
import dev.covenant.rpg.stats.RpgStats;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Real native damage events, including Survival defenses and skill refund exclusions. */
final class RageGameTests {
    private RageGameTests() {}

    static void run(GameTestHelper helper) {
        var player = TestPlayers.create(helper);
        var target = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        try {
            var origin = helper.absolutePos(new BlockPos(2, 32, 2));
            player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
            player.setNoGravity(true);
            player.setGameMode(GameType.SURVIVAL);
            player.connection.markClientLoaded();
            player.setYRot(0); player.setYHeadRot(0); player.setXRot(0);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            player.setHealth(200);
            player.getAttribute(RpgStats.DAMAGE).setBaseValue(7);
            PlayerProfiles.selectClass(player, PlayerClass.REAVER);
            target.setPos(player.getX(), player.getY(), player.getZ() + 2);
            target.setNoAi(true); target.setNoGravity(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            target.setHealth(200);
            helper.getLevel().addFreshEntity(target);

            target.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 7);
            near(player, 7, "Seven post-defense outgoing damage grants seven Rage");
            target.invulnerableTime = 0;
            target.setAbsorptionAmount(4);
            target.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 2.5f);
            near(player, 9.5, "Absorption damage counts and fractional Rage is preserved");
            target.invulnerableTime = 0;
            target.setAbsorptionAmount(0);
            target.getAttribute(Attributes.ARMOR).setBaseValue(15);
            float before = target.getHealth();
            target.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 7);
            near(player, 9.5 + before - target.getHealth(), "Outgoing armor reduction is applied before Rage");
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);

            ClassResources.set(player, PlayerClass.REAVER, 0);
            player.setAbsorptionAmount(4);
            player.hurtServer(helper.getLevel(), target.damageSources().mobAttack(target), 4);
            near(player, 8, "Four absorbed incoming damage grants eight Rage");
            player.invulnerableTime = 0;
            player.getAttribute(Attributes.ARMOR).setBaseValue(15);
            before = player.getHealth();
            player.hurtServer(helper.getLevel(), target.damageSources().mobAttack(target), 4);
            near(player, 8 + 2 * (before - player.getHealth()), "Incoming armor reduction is applied before Rage");
            player.getAttribute(Attributes.ARMOR).setBaseValue(0);

            ClassResources.set(player, PlayerClass.REAVER, 0);
            player.invulnerableTime = 0;
            player.hurtServer(helper.getLevel(), player.damageSources().fall(), 1);
            player.invulnerableTime = 0;
            player.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 1);
            target.invulnerableTime = 0;
            target.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 0);
            target.setInvulnerable(true);
            target.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 7);
            target.setInvulnerable(false);
            near(player, 0, "Environment, self, zero, and immune damage grant no Rage");
            player.invulnerableTime = 0;
            player.getAttribute(RpgStats.DODGE).setBaseValue(1); player.getRandom().setSeed(0);
            player.hurtServer(helper.getLevel(), target.damageSources().mobAttack(target), 4);
            near(player, 0, "Dodged damage grants no Rage");
            player.getAttribute(RpgStats.DODGE).setBaseValue(0);

            var arrow = EntityType.ARROW.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            arrow.setOwner(player);
            target.invulnerableTime = 0;
            target.hurtServer(helper.getLevel(), player.damageSources().arrow(arrow, player), 3);
            near(player, 3, "Normal arrows grant outgoing Rage");
            dev.covenant.rpg.skills.ProjectileCombat.markSkill(arrow);
            target.invulnerableTime = 0;
            target.hurtServer(helper.getLevel(), player.damageSources().arrow(arrow, player), 3);
            near(player, 3, "Marked skill arrows grant no outgoing Rage");
            player.getAttribute(RpgStats.RAGE_GENERATION).setBaseValue(2);
            target.invulnerableTime = 0;
            target.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 3);
            near(player, 9, "Existing generation attribute scales outgoing gains");
            player.invulnerableTime = 0;
            player.hurtServer(helper.getLevel(), target.damageSources().mobAttack(target), 2);
            near(player, 17, "Existing generation attribute scales incoming gains");
            player.getAttribute(RpgStats.RAGE_GENERATION).setBaseValue(1);
            player.invulnerableTime = 0;
            player.hurtServer(helper.getLevel(), target.damageSources().indirectMagic(target, target), 3);
            near(player, 23, "Enemy attributed magic grants incoming Rage");

            FixtureSkills.learn(player, SkillDefinition.CLEAVE.id(), true);
            FixtureSkills.learn(player, SkillDefinition.REND.id(), true);
            ClassResources.set(player, PlayerClass.REAVER, 100);
            target.invulnerableTime = 0;
            Skills.activate(player, SkillDefinition.CLEAVE);
            near(player, 80, "Cleave damage does not refund its cost");
            player.doTick();
            target.invulnerableTime = 0;
            // Release the same-tick activation gate without advancing the server clock.
            helper.runAtTickTime(2, () -> {
                try {
                    target.invulnerableTime = 0;
                    Skills.activate(player, SkillDefinition.REND);
                    near(player, 60, "Rend direct damage does not refund its cost");
                    check(Bleeding.count(target) == 1, "Rend actually hit and applied bleed");
                    for (int i = 0; i < 20; i++) { NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(target)); }
                    near(player, 60, "Bleed damage grants no outgoing Rage");
                    check(ClassResources.inCombat(player), "Skill damage refreshes decay grace");
                    ClassResources.set(player, PlayerClass.REAVER, 99);
                    target.invulnerableTime = 0;
                    target.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 7);
                    near(player, 100, "Damage gains clamp to capacity");
                    PlayerProfiles.switchClass(player, PlayerClass.STALKER);
                    ClassResources.set(player, PlayerClass.REAVER, 10);
                    target.invulnerableTime = 0;
                    target.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 7);
                    near(player, 10, "Inactive Reaver balance does not gain Rage");
                    helper.succeed();
                } finally {
                    target.discard();
                    helper.getLevel().getServer().getPlayerList().remove(player);
                }
            });
        } catch (RuntimeException | Error failure) {
            target.discard();
            helper.getLevel().getServer().getPlayerList().remove(player);
            throw failure;
        }
    }

    private static void near(ServerPlayer player, double expected, String message) {
        double actual = ClassResources.current(player, PlayerClass.REAVER);
        check(Math.abs(actual - expected) < 0.0001, message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean value, String message) {
        if (!value) { throw new IllegalStateException(message); }
    }
}
