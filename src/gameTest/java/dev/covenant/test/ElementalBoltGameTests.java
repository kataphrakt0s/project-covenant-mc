package dev.covenant.test;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.*;
import dev.covenant.rpg.stats.RpgStats;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Exercises native collision, actual activation, positive-damage gating, and saved status timers. */
final class ElementalBoltGameTests {
    static void statuses(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var target = f.target(3);
            double speed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
            ElementalBolts.hit(helper.getLevel(), f.player, f.player, target, SkillDefinition.ICE_BOLT, 60);
            near(target.getHealth(), 194, "Ice hit damage");
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), speed * 0.7, "Chilled slows by 30 percent");
            Chilled.apply(target, f.player, 30);
            check(target.getEffect(Chilled.EFFECT).getDuration() == 60, "Shorter chill cannot shorten timer");
            Chilled.apply(target, f.player, 90);
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), speed * 0.7, "Refresh does not stack");
            var reporter = new ProblemReporter.Collector();
            var output = TagValueOutput.createWithContext(reporter, helper.getLevel().registryAccess());
            target.saveWithoutId(output);
            var data = output.buildResult(); data.remove("UUID");
            var loaded = f.target(6);
            loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), data));
            check(loaded.getEffect(Chilled.EFFECT).getDuration() == 90, "Chill persists with relative timer");
            near(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED), speed * 0.7, "Load restores one slow");
            for (int i = 0; i < 90; i++) { loaded.tick(); }
            check(!loaded.hasEffect(Chilled.EFFECT), "Chill expires");
            near(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED), speed, "Expiration restores movement");
            target.removeEffect(Chilled.EFFECT); target.invulnerableTime = 0;
            ElementalBolts.hit(helper.getLevel(), f.player, f.player, target, SkillDefinition.FIRE_BOLT, 80);
            check(target.getRemainingFireTicks() == 80, "Fire applies native burning");
            target.igniteForTicks(20);
            check(target.getRemainingFireTicks() == 80, "Shorter native ignition retains longer burn");
            target.clearFire(); target.setInvulnerable(true); target.invulnerableTime = 0;
            ElementalBolts.hit(helper.getLevel(), f.player, f.player, target, SkillDefinition.FIRE_BOLT, 80);
            ElementalBolts.hit(helper.getLevel(), f.player, f.player, target, SkillDefinition.ICE_BOLT, 60);
            check(!target.isOnFire() && !target.hasEffect(Chilled.EFFECT), "Rejected damage applies no status");
            target.setInvulnerable(false); target.invulnerableTime = 0; target.setAbsorptionAmount(20);
            ElementalBolts.hit(helper.getLevel(), f.player, f.player, target, SkillDefinition.ICE_BOLT, 60);
            check(target.hasEffect(Chilled.EFFECT) && target.getAbsorptionAmount() < 20, "Absorption damage applies chill");
            target.clearFire(); target.invulnerableTime = 0;
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 100));
            float health = target.getHealth(); float absorption = target.getAbsorptionAmount();
            ElementalBolts.hit(helper.getLevel(), f.player, f.player, target, SkillDefinition.FIRE_BOLT, 80);
            near(target.getHealth(), health, "Fire Resistance prevents Fire Bolt health damage");
            near(target.getAbsorptionAmount(), absorption, "Fire Resistance prevents absorption damage");
            check(!target.isOnFire(), "Fire Resistance prevents bolt ignition");
            target.removeEffect(Chilled.EFFECT); target.invulnerableTime = 0;
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> cancel = event -> {
                if (event.getEntity() == target) { event.setCanceled(true); }
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
            try {
                ElementalBolts.hit(helper.getLevel(), f.player, f.player, target, SkillDefinition.ICE_BOLT, 60);
                check(!target.hasEffect(Chilled.EFFECT), "Canceled impact cannot chill");
            } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel); }
            f.player.getAttribute(RpgStats.DODGE).setBaseValue(1); f.player.getRandom().setSeed(0);
            ElementalBolts.hit(helper.getLevel(), target, target, f.player, SkillDefinition.ICE_BOLT, 60);
            check(!f.player.hasEffect(Chilled.EFFECT), "Dodged direct skill cannot chill");
            check(reporter.isEmpty(), "Native status serialization reports no errors");
            helper.succeed();
        }
    }

    static void collision(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var first = f.target(4);
            var second = f.target(7);
            f.player.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
            f.player.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(2);
            Skills.activate(f.player, SkillDefinition.ICE_BOLT);
            var bolts = helper.getLevel().getEntitiesOfClass(ElementalBolt.class, f.player.getBoundingBox().inflate(2));
            check(bolts.size() == 1, "Activation spawns one bolt");
            var bolt = bolts.getFirst(); f.entities.add(bolt);
            for (int i = 0; i < 10 && !bolt.isRemoved(); i++) { bolt.tick(); }
            check(bolt.isRemoved(), "Bolt removed on collision");
            near(first.getHealth(), 194, "First target takes hit");
            near(second.getHealth(), 200, "Projectile cannot pierce");
            check(first.getEffect(Chilled.EFFECT).getDuration() == 120, "Duration captured from caster");
            near(ClassResources.current(f.player, PlayerClass.WEAVER), 80, "Spends 20 Mana");
            check(Skills.cooldownRemaining(f.player, SkillDefinition.ICE_BOLT) == 40, "Starts independent cooldown");
            first.discard(); second.discard();
            f.wall = BlockPos.containing(f.player.getX(), f.player.getEyeY(), f.player.getZ() + 2);
            helper.getLevel().setBlockAndUpdate(f.wall, Blocks.STONE.defaultBlockState());
            var fire = new ElementalBolt(Skills.FIRE_BOLT.get(), helper.getLevel());
            fire.setOwner(f.player); fire.setPos(f.player.getEyePosition()); fire.setDeltaMovement(0, 0, 1.5);
            helper.getLevel().addFreshEntity(fire); f.entities.add(fire);
            for (int i = 0; i < 5 && !fire.isRemoved(); i++) { fire.tick(); }
            check(fire.isRemoved(), "Solid block removes projectile");
            helper.succeed();
        }
    }

    static void lightning(GameTestHelper helper) {
        var f = new Fixture(helper);
        try {
            var first = f.target(4); var second = f.target(7);
            helper.onEachTick(f.player::doTick);
            Skills.activate(f.player, SkillDefinition.LIGHTNING_BOLT);
            near(first.getHealth(), 200, "Lightning waits for wind-up");
            near(ClassResources.current(f.player, PlayerClass.WEAVER), 100, "Cast start does not spend");
            helper.runAtTickTime(15, () -> {
                try {
                    near(first.getHealth(), 194, "Lightning hits immediately on completion");
                    near(second.getHealth(), 200, "Lightning stops at first enemy");
                    check(!first.isOnFire() && !first.hasEffect(Chilled.EFFECT), "Lightning has no status");
                    f.player.getAttribute(RpgStats.CAST_TIME).setBaseValue(0);
                    first.discard();
                    f.wall = BlockPos.containing(f.player.getX(), f.player.getEyeY(), f.player.getZ() + 2);
                    helper.getLevel().setBlockAndUpdate(f.wall, Blocks.STONE.defaultBlockState());
                    // Keep the real activation path while allowing a second cast within this short fixture.
                    f.player.setData(dev.covenant.rpg.RpgData.SKILLS, f.player.getData(dev.covenant.rpg.RpgData.SKILLS)
                            .withCooldown(SkillDefinition.LIGHTNING_BOLT.id(), 0));
                    Skills.activate(f.player, SkillDefinition.LIGHTNING_BOLT);
                    near(second.getHealth(), 200, "Solid cover stops lightning");
                    check(Skills.cooldownRemaining(f.player, SkillDefinition.LIGHTNING_BOLT) > 0, "Miss still commits cooldown");
                    helper.succeed();
                } finally { f.close(); }
            });
        } catch (RuntimeException error) { f.close(); throw error; }
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final ServerPlayer player;
        final ArrayList<Entity> entities = new ArrayList<>();
        BlockPos wall;
        Fixture(GameTestHelper helper) {
            this.helper = helper;
            var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "bolt-test-player"), false);
            player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            player.setGameMode(GameType.SURVIVAL);
            player.connection.markClientLoaded();
            player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 90, 1))));
            player.setNoGravity(true); player.setYRot(0); player.setXRot(0);
            PlayerProfiles.selectClass(player, PlayerClass.WEAVER);
            for (var skill : new SkillDefinition[]{SkillDefinition.FIRE_BOLT, SkillDefinition.ICE_BOLT, SkillDefinition.LIGHTNING_BOLT}) {
                SkillLoadouts.learn(player, skill.id(), true);
            }
            ClassResources.set(player, PlayerClass.WEAVER, 100);
        }
        LivingEntity target(double distance) {
            var target = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            target.setPos(player.position().add(0, 0, distance)); target.setNoGravity(true); target.setNoAi(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0); target.setHealth(200);
            helper.getLevel().addFreshEntity(target); entities.add(target); return target;
        }
        @Override public void close() {
            entities.forEach(Entity::discard);
            helper.getLevel().getServer().getPlayerList().remove(player);
            if (wall != null) { helper.getLevel().setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState()); }
        }
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.001, message + ": " + actual + " != " + expected);
    }
}
