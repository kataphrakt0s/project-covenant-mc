package dev.covenant.test;

import dev.covenant.rpg.skills.*;

import com.mojang.authlib.GameProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.stats.RpgStats;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Dedicated tests use real entities, native damage/effect persistence, and the production activation boundary. */
public final class TrapGameTests {
    private TrapGameTests() {}

    public static void deployment(GameTestHelper helper) {
        try (var f = new Fixture(helper, 35)) {
            var owner = f.player(0);
            var enemy = f.target(owner.position().add(0, 0, 3));
            var trap = f.throwSkill(owner, SkillDefinition.BEAR_TRAP);
            near(ClassResources.current(owner, PlayerClass.STALKER), 80, "Successful throw spends exactly once");
            check(Skills.cooldownRemaining(owner, SkillDefinition.BEAR_TRAP) == 160, "Throw starts cooldown");
            var state = owner.getData(RpgData.SKILLS);
            Skills.activate(owner, SkillDefinition.BEAR_TRAP);
            check(owner.getData(RpgData.SKILLS).equals(state) && !trap.isRemoved(), "Rejected cooldown preserves deployment/state");
            int flight = 0;
            while (trap.phase() == TrapEntity.Phase.FLIGHT && !trap.isRemoved() && flight++ < 100) {
                trap.tick(); near(enemy.getHealth(), 200, "No flight hit");
            }
            check(trap.phase() == TrapEntity.Phase.ARMING, "Lands on solid support");
            near(trap.getZ() - owner.getZ(), 3, "Clear-ground horizontal toss is three blocks");
            enemy.setPos(trap.position());
            ticks(trap, 9);
            check(trap.phase() == TrapEntity.Phase.ARMING, "Nine landed ticks do not arm");
            near(enemy.getHealth(), 200, "No arming damage");
            trap.tick();
            check(trap.isRemoved(), "Enemy already present triggers on tenth tick");
            near(enemy.getHealth(), 196, "Single snap uses fixed four damage");
            check(Rooted.contains(enemy), "Snap roots");

            var empty = f.ground(owner, TrapKind.BEAR, owner.position().add(4, 0, 0));
            ticks(empty, 10 + 599);
            check(!empty.isRemoved() && empty.phase() == TrapEntity.Phase.ARMED, "Armed trap lasts 599 subsequent ticks");
            empty.tick(); check(empty.isRemoved(), "Expires at 600 armed ticks");

            reset(owner, SkillDefinition.BEAR_TRAP);
            var previous = f.throwSkill(owner, SkillDefinition.BEAR_TRAP);
            reset(owner, SkillDefinition.BEAR_TRAP);
            ClassResources.set(owner, PlayerClass.STALKER, 0);
            var noMoney = owner.getData(RpgData.SKILLS);
            Skills.activate(owner, SkillDefinition.BEAR_TRAP);
            check(!previous.isRemoved() && owner.getData(RpgData.SKILLS).equals(noMoney), "Insufficient Focus preserves pending flight");
            ClassResources.set(owner, PlayerClass.STALKER, 100);
            var beforeRejectedAdd = owner.getData(RpgData.SKILLS);
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> reject = event -> {
                if (event.getEntity() instanceof TrapEntity) { event.setCanceled(true); }
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(reject);
            try { Skills.activate(owner, SkillDefinition.BEAR_TRAP); }
            finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(reject); }
            check(!previous.isRemoved() && owner.getData(RpgData.SKILLS).equals(beforeRejectedAdd),
                    "Rejected entity add preserves old deployment, Focus, and cooldown");
            var replacement = f.throwSkill(owner, SkillDefinition.BEAR_TRAP);
            check(previous.isRemoved() && replacement.phase() == TrapEntity.Phase.FLIGHT, "Accepted throw replaces pending flight");
            SkillLoadouts.bind(owner, 0, SkillDefinition.BEAR_TRAP.id());
            SkillLoadouts.bind(owner, 0, null);
            check(!replacement.isRemoved(), "Unbinding is not unlearning");
            FixtureSkills.learn(owner, SkillDefinition.BEAR_TRAP.id(), false);
            check(replacement.isRemoved(), "Unlearning removes deployment");
            check(Rooted.contains(enemy), "Applied root outlives owner cleanup");
            helper.succeed();
        }
    }

    public static void terrain(GameTestHelper helper) {
        try (var f = new Fixture(helper, 45)) {
            var owner = f.player(0);
            var wall = BlockPos.containing(owner.position().add(0, 0, 2));
            for (int y = 0; y < 4; y++) { f.block(wall.above(y), Blocks.STONE.defaultBlockState()); }
            var trap = f.throwSkill(owner, SkillDefinition.BEAR_TRAP);
            ticks(trap, 50);
            check(!trap.isRemoved() && trap.phase() == TrapEntity.Phase.ARMED && trap.getZ() < wall.getZ(), "Wall shortens toss and trap drops");
            trap.discard();
            for (int y = 0; y < 4; y++) { f.block(wall.above(y), Blocks.AIR.defaultBlockState()); }
            var releaseWall = BlockPos.containing(owner.position().add(0, 0, 1));
            for (int y = 0; y < 3; y++) { f.block(releaseWall.above(y), Blocks.STONE.defaultBlockState()); }
            var originalPosition = owner.position();
            owner.setPos(owner.getX(), owner.getY(), releaseWall.getZ() - 0.301);
            reset(owner, SkillDefinition.BEAR_TRAP);
            var closeWall = f.throwSkill(owner, SkillDefinition.BEAR_TRAP); ticks(closeWall, 50);
            check(!closeWall.isRemoved() && closeWall.getZ() < releaseWall.getZ(), "Release against a wall cannot start inside or tunnel through it");
            closeWall.discard(); owner.setPos(originalPosition);
            for (int y = 0; y < 3; y++) { f.block(releaseWall.above(y), Blocks.AIR.defaultBlockState()); }
            var point = owner.position().add(4, 0, 0);
            var floor = BlockPos.containing(point).below();
            f.block(floor, Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            var slab = f.ground(owner, TrapKind.BEAR, new Vec3(point.x, floor.getY() + 0.5, point.z));
            ticks(slab, 10); check(slab.phase() == TrapEntity.Phase.ARMED, "Half slab supports trap at collision height");
            f.block(floor, Blocks.AIR.defaultBlockState()); slab.tick();
            check(slab.isRemoved(), "Removing support fizzles trap");
            f.block(floor, Blocks.STONE_STAIRS.defaultBlockState());
            var stair = f.ground(owner, TrapKind.BEAR, new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 0.75));
            ticks(stair, 10); check(!stair.isRemoved(), "Stair collision surface supports trap"); stair.discard();
            f.block(floor, Blocks.STONE.defaultBlockState());
            var flooded = f.ground(owner, TrapKind.BEAR, point);
            f.block(BlockPos.containing(point), Blocks.WATER.defaultBlockState()); flooded.tick();
            check(flooded.isRemoved(), "Flooding placement fizzles trap");
            var wetThrow = f.ground(owner, TrapKind.BEAR, point); wetThrow.tick();
            check(wetThrow.isRemoved(), "Fluid before landing fizzles throw");
            owner.setPos(owner.position().add(-4, 0, 0));
            for (int z = 1; z <= 4; z++) {
                f.block(BlockPos.containing(owner.position().add(0, -1, z)), Blocks.AIR.defaultBlockState());
                f.block(BlockPos.containing(owner.position().add(0, -4, z)), Blocks.STONE.defaultBlockState());
            }
            reset(owner, SkillDefinition.BEAR_TRAP);
            var ledge = f.throwSkill(owner, SkillDefinition.BEAR_TRAP); ticks(ledge, 50);
            check(!ledge.isRemoved() && ledge.phase() == TrapEntity.Phase.ARMED, "Ledge throw falls to lower support and arms");
            near(ledge.getZ() - owner.getZ(), 3, "Falling does not extend forward throw beyond three blocks");
            near(ledge.getY(), owner.getY() - 3, "Trap lands on lower collision surface");
            helper.succeed();
        }
    }

    public static void bear(GameTestHelper helper) {
        try (var f = new Fixture(helper, 55)) {
            var owner = f.player(0);
            var target = f.target(owner.position().add(0, 0, 3));
            var trap = f.ground(owner, TrapKind.BEAR, target.position());
            ticks(trap, 11);
            near(target.getHealth(), 196, "Bear base damage independent of weapon");
            check(target.getEffect(Rooted.EFFECT).getDuration() == 60, "Base three-second root");
            target.setDeltaMovement(Vec3.ZERO); target.moveRelative(1, new Vec3(0, 0, 1));
            check(target.getDeltaMovement().equals(Vec3.ZERO), "Root blocks walking input");
            target.jumpFromGround(); check(target.getDeltaMovement().equals(Vec3.ZERO), "Root blocks jumping");
            target.push(0.3, 0.2, 0); var external = target.getDeltaMovement();
            target.moveRelative(1, new Vec3(0, 0, 1));
            check(target.getDeltaMovement().equals(external), "Root preserves external knockback");
            Rooted.apply(target, 20); check(target.getEffect(Rooted.EFFECT).getDuration() == 60, "Short root never shortens");
            Rooted.apply(target, 72); check(target.getEffect(Rooted.EFFECT).getDuration() == 72, "Longer root replaces rather than adds");
            target.invulnerableTime = 0;
            target.hurtServer(helper.getLevel(), owner.damageSources().playerAttack(owner), 1);
            check(target.getEffect(Rooted.EFFECT).getDuration() == 72, "Damage does not break root");

            var absorb = f.target(owner.position().add(4, 0, 0));
            absorb.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(20); absorb.setAbsorptionAmount(20);
            ticks(f.ground(owner, TrapKind.BEAR, absorb.position()), 11);
            near(absorb.getHealth(), 200, "Absorption protects health"); near(absorb.getAbsorptionAmount(), 16, "Snap consumes absorption");
            check(Rooted.contains(absorb), "Positive absorption damage applies root");
            var immune = f.target(owner.position().add(-4, 0, 0)); immune.setInvulnerable(true);
            ticks(f.ground(owner, TrapKind.BEAR, immune.position()), 11);
            check(!Rooted.contains(immune) && immune.getHealth() == 200, "Invulnerability prevents damage/root");
            var armored = f.target(owner.position().add(0, 0, -4)); armored.getAttribute(Attributes.ARMOR).setBaseValue(20);
            ticks(f.ground(owner, TrapKind.BEAR, armored.position()), 11);
            check(armored.getHealth() > 196 && armored.getHealth() < 200 && Rooted.contains(armored), "Armor mitigates without blocking root");
            var dodger = f.player(5); dodger.getAttribute(RpgStats.DODGE).setBaseValue(1); dodger.getRandom().setSeed(0);
            ticks(f.ground(owner, TrapKind.BEAR, dodger.position()), 11);
            check(dodger.getHealth() == 20 && !Rooted.contains(dodger), "Bear participates in guaranteed Dodge");
            var boss = EntityType.WITHER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            f.entities.add(boss); boss.setPos(owner.position().add(-5, 0, -5)); boss.setNoAi(true); boss.setNoGravity(true);
            helper.getLevel().addFreshEntity(boss);
            ticks(f.ground(owner, TrapKind.BEAR, boss.position()), 11);
            check(Rooted.contains(boss), "Eligible boss receives root without creature exemptions");
            helper.succeed();
        }
    }

    public static void rootPersistence(GameTestHelper helper) {
        var f = new Fixture(helper, 65);
        var owner = f.player(0);
        var target = f.target(owner.position().add(0, 0, 3));
        Rooted.apply(target, 72); Rooted.apply(owner, 60);
        var reporter = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(reporter, helper.getLevel().registryAccess());
        target.saveWithoutId(output); var saved = output.buildResult(); saved.remove("UUID");
        var loaded = f.target(owner.position().add(4, 0, 0));
        loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), saved));
        loaded.setPos(owner.position().add(4, 0, 0));
        // Added test instances can place this off-template target across a chunk boundary.
        var rootChunk = new net.minecraft.world.level.ChunkPos(net.minecraft.util.Mth.floor(loaded.getX()) >> 4,
                net.minecraft.util.Mth.floor(loaded.getZ()) >> 4);
        boolean forcedRootChunk = helper.getLevel().setChunkForced(rootChunk.x(), rootChunk.z(), true);
        check(loaded.getEffect(Rooted.EFFECT).getDuration() == 72 && reporter.isEmpty(), "Native mob effect serialization retains root");
        var playerList = helper.getLevel().getServer().getPlayerList();
        playerList.saveAll(); var disk = playerList.loadPlayerData(owner.nameAndId()).orElseThrow(); disk.remove("UUID");
        var recipient = f.player(6);
        recipient.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
        check(Rooted.contains(recipient), "Native player file restores root");
        var dead = f.player(-6); dead.restoreFrom(owner, false);
        check(!Rooted.contains(dead), "Death player replacement does not copy root");
        var inactive = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        inactive.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), saved));
        f.entities.add(inactive);
        helper.runAtTickTime(10, () -> {
            try {
                check(inactive.getEffect(Rooted.EFFECT).getDuration() == 72, "Inactive target timer pauses");
                check(loaded.getEffect(Rooted.EFFECT).getDuration() < 72, "Ticking target counts root down");
                target.hurtServer(helper.getLevel(), target.damageSources().genericKill(), Float.MAX_VALUE);
                check(!Rooted.contains(target), "Death clears root");
                helper.succeed();
            } finally {
                f.close();
                if (forcedRootChunk) { helper.getLevel().setChunkForced(rootChunk.x(), rootChunk.z(), false); }
            }
        });
    }

    public static void frost(GameTestHelper helper) {
        try (var f = new Fixture(helper, 75)) {
            var owner = f.player(0); var other = f.player(6);
            var target = f.target(owner.position().add(0, 0, 3));
            double base = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
            var first = f.ground(owner, TrapKind.FROST, target.position()); ticks(first, 10);
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base * 0.5, "Frost slows by half on trigger");
            var second = f.ground(other, TrapKind.FROST, target.position()); ticks(second, 10);
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base * 0.5, "Two owners never stack Frost slows");
            first.discard(); near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base * 0.5, "One removal preserves other source");
            var center = target.position();
            target.setPos(center.add(3.25, 0, 0)); second.tick();
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base * 0.5, "Frost reaches the expanded outer area");
            target.setPos(center.add(3.5, 0, 0)); second.tick();
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base, "Frost excludes the exact outer boundary");
            target.setPos(center); second.tick();
            target.setPos(center.add(4, 0, 0));
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(target));
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base, "Last exit removes slow after target movement");
            target.setPos(center); second.tick();
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base * 0.5, "Reentry restores slow");
            Rooted.apply(target, 60); target.moveRelative(1, new Vec3(0, 0, 1));
            check(target.getDeltaMovement().equals(Vec3.ZERO), "Root still blocks movement inside Frost");

            var pending = f.ground(other, TrapKind.FROST, center.add(3, 0, 0)); ticks(pending, 10);
            check(!second.isRemoved(), "Arming new Frost does not remove old patch");
            target.setPos(pending.position()); pending.tick();
            check(second.isRemoved() && pending.phase() == TrapEntity.Phase.TRIGGERED, "New trigger replaces owner's old patch");
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base * 0.5, "Replacement retains new slow");
            FixtureSkills.learn(other, TrapKind.BEAR.skillId, false);
            check(pending.isRemoved(), "Deallocating Bear cleans up dependent Frost");
            near(target.getAttributeValue(Attributes.MOVEMENT_SPEED), base, "Dependent cleanup removes final slow");

            var patch = f.ground(owner, TrapKind.FROST, center); target.setPos(center); ticks(patch, 10);
            var covered = f.target(center.add(0, 0, 2));
            var wall = BlockPos.containing(center.add(0, 0, 1));
            f.block(wall, Blocks.STONE.defaultBlockState()); patch.tick();
            near(covered.getAttributeValue(Attributes.MOVEMENT_SPEED), base, "Solid wall blocks Frost");
            var above = f.target(center.add(0, 2, 0));
            f.block(BlockPos.containing(center.add(0, 1, 0)), Blocks.STONE.defaultBlockState()); patch.tick();
            near(above.getAttributeValue(Attributes.MOVEMENT_SPEED), base, "Solid floor blocks Frost");
            f.block(wall, Blocks.AIR.defaultBlockState()); patch.tick();
            near(covered.getAttributeValue(Attributes.MOVEMENT_SPEED), base * 0.5, "Removing cover restores eligible membership");
            var reporter = new ProblemReporter.Collector();
            var save = TagValueOutput.createWithContext(reporter, helper.getLevel().registryAccess()); covered.saveWithoutId(save);
            var saved = save.buildResult(); saved.remove("UUID");
            var loaded = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND); f.entities.add(loaded);
            loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), saved));
            near(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED), base, "Transient Frost is absent after native save/load");
            ticks(patch, 160); check(patch.isRemoved(), "Frost expires at its duration");
            near(covered.getAttributeValue(Attributes.MOVEMENT_SPEED), base, "Expiry removes final slow");
            helper.succeed();
        }
    }

    public static void flame(GameTestHelper helper) {
        var f = new Fixture(helper, 85);
        var owner = f.player(0); var other = f.player(6);
        var target = f.target(owner.position().add(0, 0, 3));
        var center = target.position();
        var credits = new ArrayList<UUID>();
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post> listener = event -> {
            if (event.getEntity() == target && event.getSource().is(TrapKind.FLAME.damageType)) {
                credits.add(event.getSource().getEntity().getUUID());
            }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener); f.listeners.add(listener);
        var first = f.ground(owner, TrapKind.FLAME, center); ticks(first, 10);
        near(target.getHealth(), 197, "First entry immediately takes three fire damage");
        var outside = f.target(center.add(3.25, 0, 0)); first.tick();
        near(outside.getHealth(), 200, "Flame retains its three-block effect radius");
        var second = f.ground(other, TrapKind.FLAME, center); ticks(second, 10);
        near(target.getHealth(), 194, "Another owner's initial hit is independent of hurt cooldown");
        check(!target.isOnFire(), "Flame never ignites targets");
        target.setPos(center.add(4, 0, 0));
        f.at(5, () -> target.setPos(center));
        f.at(10, () -> near(target.getHealth(), 194, "Exit and reentry do not accelerate per-patch hits"));
        f.at(15, () -> target.hurtServer(helper.getLevel(), owner.damageSources().playerAttack(owner), 2));
        f.at(22, () -> near(target.getHealth(), 186, "Both due hits survive unrelated native hurt immunity"));
        f.at(42, () -> near(target.getHealth(), 180, "Both owners deliver each one-second interval"));
        f.at(122, () -> {
            try {
                check(first.isRemoved() && second.isRemoved(), "Patches expire at six seconds");
                near(target.getHealth(), 162, "Each patch gives exactly six hits / eighteen damage, with no expiry hit");
                check(credits.stream().filter(owner.getUUID()::equals).count() == 6
                        && credits.stream().filter(other.getUUID()::equals).count() == 6, "Each patch retains its own owner credit");
                check(!target.isOnFire(), "No lingering burn");
                helper.succeed();
            } finally { f.close(); }
        });
    }

    public static void flameDefenses(GameTestHelper helper) {
        var f = new Fixture(helper, 95);
        var owner = f.player(0);
        var center = owner.position().add(0, 0, 3);
        var trigger = f.target(center);
        var protectedTarget = f.target(center.add(1, 0, 0));
        protectedTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 200));
        var immune = EntityType.MAGMA_CUBE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        immune.setPos(center.add(-1, 0, 0)); immune.setNoAi(true); immune.setNoGravity(true); f.entities.add(immune);
        helper.getLevel().addFreshEntity(immune); float immuneHealth = immune.getHealth();
        var invulnerable = f.target(center.add(0, 0, 1)); invulnerable.setInvulnerable(true);
        var behindWall = f.target(center.add(0, 0, 2));
        f.block(BlockPos.containing(center.add(0, 0, 1)), Blocks.STONE.defaultBlockState());
        var shield = f.player(2); shield.setPos(center.add(2, 0, 0)); shield.getAttribute(RpgStats.DODGE).setBaseValue(1); shield.getRandom().setSeed(0);
        shield.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD));
        shield.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
        helper.onEachTick(shield::doTick);
        f.at(6, () -> {
            check(shield.isBlocking(), "Shield fixture is actively blocking");
            var patch = f.ground(owner, TrapKind.FLAME, center); ticks(patch, 10);
            near(protectedTarget.getHealth(), 200, "Fire Resistance prevents Flame");
            near(immune.getHealth(), immuneHealth, "Natural fire immunity prevents Flame");
            near(invulnerable.getHealth(), 200, "True invulnerability prevents Flame");
            near(behindWall.getHealth(), 200, "Wall blocks Flame");
            near(shield.getHealth(), 17, "Flame bypasses shield and guaranteed Dodge");
            near(trigger.getHealth(), 197, "Eligible target is hit");
            var pending = f.ground(owner, TrapKind.FLAME, center.add(4, 0, 0)); ticks(pending, 10);
            check(!patch.isRemoved(), "Next Flame throw/arming retains old patch");
            trigger.setPos(pending.position()); pending.tick();
            check(patch.isRemoved() && pending.phase() == TrapEntity.Phase.TRIGGERED, "Next Flame trigger replaces old patch");
            PlayerProfiles.switchClass(owner, PlayerClass.REAVER);
            check(pending.isRemoved(), "Class switch cleans patch");
            helper.succeed(); f.close();
        });
    }

    public static void blast(GameTestHelper helper) {
        var f = new Fixture(helper, 105);
        var owner = f.player(0); var center = owner.position().add(0, 0, 3);
        var middle = f.target(center);
        var edge = f.target(center.add(3.15, 0, 0));
        var expanded = f.target(center.add(3.25, 0, 0));
        var outside = f.target(center.add(3.5, 0, 0));
        var rooted = f.target(center.add(1, 0, -1)); Rooted.apply(rooted, 60);
        var resistant = f.target(center.add(-1, 0, -1)); resistant.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        var explosionResistant = f.target(center.add(-1, 0, 0));
        explosionResistant.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE).setBaseValue(1);
        var armored = f.target(center.add(0, 0, -2)); armored.getAttribute(Attributes.ARMOR).setBaseValue(20);
        var unenchanted = f.target(center.add(2, 0, -1));
        var enchanted = f.target(center.add(-2, 0, -1));
        var helmet = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET);
        unenchanted.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helmet.copy());
        helmet.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BLAST_PROTECTION), 4);
        enchanted.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helmet);
        var hidden = f.target(center.add(0, 0, 2));
        var wall = BlockPos.containing(center.add(0, 0, 1)); f.block(wall, Blocks.STONE.defaultBlockState());
        var immune = f.target(center.add(-2, 0, 0)); immune.setInvulnerable(true);
        var shield = f.player(2); shield.setPos(center.add(1, 0, 0)); shield.setYRot(90);
        shield.getAttribute(RpgStats.DODGE).setBaseValue(1); shield.getRandom().setSeed(0);
        shield.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD));
        shield.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND); helper.onEachTick(shield::doTick);
        var credits = new ArrayList<UUID>();
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post> listener = event -> {
            if (event.getEntity() == middle && event.getSource().is(TrapKind.BLAST.damageType)) {
                check(event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION), "Blast uses native explosion defenses");
                credits.add(event.getSource().getEntity().getUUID());
            }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener); f.listeners.add(listener);
        f.at(6, () -> {
            check(shield.isBlocking(), "Blast shield fixture blocks");
            shield.getAttribute(RpgStats.DODGE).setBaseValue(0); shield.setYRot(-90);
            // Isolate the shield bearer from the other Blast targets within the wider contact box.
            var bear = f.ground(owner, TrapKind.BEAR, shield.position().add(0.6, 0, 0.6)); ticks(bear, 10);
            near(shield.getHealth(), 16, "Bear bypasses shield");
            check(Rooted.contains(shield), "Shield does not prevent root");
            shield.getAttribute(RpgStats.DODGE).setBaseValue(1); shield.getRandom().setSeed(0); shield.setYRot(90); shield.invulnerableTime = 0;
            float shieldHealth = shield.getHealth();
            var trap = f.ground(owner, TrapKind.BLAST, center); ticks(trap, 10);
            check(trap.isRemoved(), "Blast is consumed once");
            near(middle.getHealth(), 190, "Center receives ten base damage");
            near(edge.getHealth(), 199, "Damage falls toward zero at edge");
            near(expanded.getHealth(), 200 - 10 * (1 - 3.25 / 3.5), "Blast reaches the expanded area with scaled falloff");
            check(expanded.getDeltaMovement().lengthSqr() > 0, "Expanded Blast area also applies knockback");
            near(outside.getHealth(), 200, "Outside radius receives no damage");
            near(hidden.getHealth(), 200, "Wall blocks Blast damage");
            check(hidden.getDeltaMovement().equals(Vec3.ZERO), "Wall blocks Blast displacement");
            check(rooted.getDeltaMovement().lengthSqr() > 0 && Rooted.contains(rooted), "Blast displaces rooted target without ending root");
            var motion = rooted.getDeltaMovement(); rooted.moveRelative(1, new Vec3(0, 0, 1));
            check(rooted.getDeltaMovement().equals(motion), "Pushed root still blocks walking");
            check(resistant.getDeltaMovement().equals(Vec3.ZERO) && explosionResistant.getDeltaMovement().equals(Vec3.ZERO),
                    "Both native knockback resistance attributes are respected");
            check(armored.getHealth() > 200 - 10.0 / 3 && armored.getHealth() < 200, "Armor mitigates Blast");
            check(shield.getHealth() < shieldHealth, "Blast bypasses shield and guaranteed Dodge");
            check(enchanted.getHealth() > unenchanted.getHealth() && enchanted.getHealth() < 200,
                    "Native Blast Protection further reduces explosion damage");
            check(immune.getHealth() == 200 && immune.getDeltaMovement().equals(Vec3.ZERO), "Invulnerability prevents damage and shove");
            check(credits.equals(List.of(owner.getUUID())), "One explosion with owner credit");
            check(helper.getLevel().getBlockState(wall).is(Blocks.STONE), "Blast does not destroy cover");
            helper.succeed(); f.close();
        });
    }

    public static void lifecycle(GameTestHelper helper) {
        try (var f = new Fixture(helper, 115)) {
            var owner = f.player(0);
            var center = owner.position().add(0, 0, 3);
            owner.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(1.2);
            owner.getAttribute(RpgStats.RESOURCE_COST).setBaseValue(0.5);
            owner.getAttribute(RpgStats.COOLDOWN).setBaseValue(0.5);
            Rooted.apply(owner, 60);
            var trap = f.throwSkill(owner, SkillDefinition.BEAR_TRAP);
            check(trap.effectDuration() == 72, "Duration snapshots on throw");
            near(ClassResources.current(owner, PlayerClass.STALKER), 90, "Trap cost scales through native multiplier");
            check(Skills.cooldownRemaining(owner, SkillDefinition.BEAR_TRAP) == 80, "Trap cooldown scales");
            owner.getAttribute(RpgStats.SKILL_DURATION).setBaseValue(0.5);
            var target = f.target(center);
            trap.setPos(center.add(0, 0.05, 0)); trap.setDeltaMovement(0, -0.1, 0); ticks(trap, 11);
            check(target.getEffect(Rooted.EFFECT).getDuration() == 72, "Trigger does not recalculate duration");
            check(Rooted.contains(owner), "Root permits skill activation");

            var frost = f.ground(owner, TrapKind.FROST, center); ticks(frost, 10);
            var flame = f.ground(owner, TrapKind.FLAME, center); ticks(flame, 10);
            check(!frost.isRemoved() && !flame.isRemoved(), "Different active patch types coexist");
            var save = TagValueOutput.createWithContext(new ProblemReporter.Collector(), helper.getLevel().registryAccess());
            check(!frost.save(save) && !flame.save(save), "Native save excludes deployments and patches");
            frost.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            check(!target.hasData(RpgData.FROST_SOURCES), "Chunk-removal path clears Frost immediately");
            FixtureSkills.learn(owner, TrapKind.FLAME.skillId, false);
            check(flame.isRemoved() && Rooted.contains(target), "Unlearning removes patch but not applied root");

            var pending = f.ground(owner, TrapKind.BEAR, center.add(4, 0, 0));
            PlayerProfiles.switchClass(owner, PlayerClass.REAVER);
            check(pending.isRemoved(), "Class change cleans pending deployment");
            PlayerProfiles.switchClass(owner, PlayerClass.STALKER);
            var dimension = f.ground(owner, TrapKind.BEAR, center.add(4, 0, 0));
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(
                    owner, net.minecraft.world.level.Level.OVERWORLD, net.minecraft.world.level.Level.NETHER));
            check(dimension.isRemoved(), "Dimension event cleans deployment");
            var death = f.ground(owner, TrapKind.BEAR, center.add(4, 0, 0));
            owner.hurtServer(helper.getLevel(), owner.damageSources().genericKill(), Float.MAX_VALUE);
            check(death.isRemoved(), "Owner death cleans deployment");
            var logged = f.player(6); var logout = f.ground(logged, TrapKind.BEAR, center.add(4, 0, 0));
            helper.getLevel().getServer().getPlayerList().remove(logged);
            check(logout.isRemoved(), "Logout cleans deployment");
            helper.succeed();
        }
    }

    public static void targeting(GameTestHelper helper) {
        try (var f = new Fixture(helper, 125)) {
            var owner = f.player(0); var center = owner.position();
            var pet = EntityType.WOLF.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            pet.setPos(center); pet.setNoAi(true); pet.setNoGravity(true); pet.tame(owner);
            helper.getLevel().addFreshEntity(pet); f.entities.add(pet);
            var spectator = f.player(5); spectator.setGameMode(GameType.SPECTATOR); spectator.setPos(center);
            var flyer = f.target(center.add(0, 0.4, 0));
            var opponent = f.player(-5); opponent.setPos(center);
            var rules = helper.getLevel().getGameRules();
            boolean pvp = rules.get(net.minecraft.world.level.gamerules.GameRules.PVP);
            rules.set(net.minecraft.world.level.gamerules.GameRules.PVP, false, helper.getLevel().getServer());
            try {
                check(!Skills.canHit(owner, opponent), "Native PvP restriction excludes players");
                var noPvp = f.ground(owner, TrapKind.BEAR, center); ticks(noPvp, 10);
                check(!noPvp.isRemoved(), "PvP-disabled opponent cannot trigger trap"); noPvp.discard();
            } finally { rules.set(net.minecraft.world.level.gamerules.GameRules.PVP, pvp, helper.getLevel().getServer()); }
            opponent.setPos(center.add(-5, 0, 0));
            flyer.setPos(center.add(0, 5, 0));
            for (var kind : TrapKind.values()) {
                var extended = f.ground(owner, kind, center);
                var contact = f.target(center.add(1.1, 0, 0)); ticks(extended, 10);
                check(extended.phase() == TrapEntity.Phase.ARMED, kind + " excludes hitboxes beyond widened footprint");
                contact.setPos(center.add(0.95, 0, 0)); extended.tick();
                check(extended.isRemoved() || extended.phase() == TrapEntity.Phase.TRIGGERED,
                        kind + " triggers on contact outside the old footprint");
                extended.discard(); contact.discard();
            }
            flyer.setPos(center.add(0, 0.4, 0));
            var trap = f.ground(owner, TrapKind.BEAR, center); ticks(trap, 10);
            check(!trap.isRemoved() && trap.phase() == TrapEntity.Phase.ARMED, "Owner, pet, spectator, and overhead creature do not trigger");
            var passive = EntityType.SHEEP.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            passive.setPos(center.add(1.1, 0, 0)); passive.setNoAi(true); passive.setNoGravity(true);
            helper.getLevel().addFreshEntity(passive); f.entities.add(passive);
            trap.tick();
            check(trap.isRemoved() && Rooted.contains(passive), "Passive creature's hitbox touching footprint triggers even with center outside");
            near(flyer.getHealth(), 200, "Single-target Bear does not hit other nearby creatures");
            helper.succeed();
        }
    }

    public static void activation(GameTestHelper helper) {
        try (var f = new Fixture(helper, 135)) {
            var owner = f.player(0);
            var loadoutSave = TagValueOutput.createWithContext(new ProblemReporter.Collector(), helper.getLevel().registryAccess());
            int slot = 0;
            for (var kind : TrapKind.values()) { SkillLoadouts.bind(owner, slot++, kind.skillId); }
            owner.saveWithoutId(loadoutSave); var data = loadoutSave.buildResult(); data.remove("UUID");
            var returning = f.player(6); returning.load(TagValueInput.create(new ProblemReporter.Collector(), helper.getLevel().registryAccess(), data));
            returning.setPos(owner.position().add(6, 0, 0));
            slot = 0;
            for (var kind : TrapKind.values()) {
                var skill = SkillDefinition.find(kind.skillId).orElseThrow();
                ClassResources.set(owner, PlayerClass.STALKER, 100);
                reset(owner, skill);
                helper.getLevel().getServer().getCommands().performPrefixedCommand(owner.createCommandSourceStack(), "covenant skill use " + skill.id());
                near(ClassResources.current(owner, PlayerClass.STALKER), 100 - skill.cost(), "Command activates " + kind);
                ClassResources.set(returning, PlayerClass.STALKER, 100); reset(returning, skill);
                SkillLoadouts.activateSlot(returning, slot++);
                near(ClassResources.current(returning, PlayerClass.STALKER), 100 - skill.cost(), "Saved slot activates " + kind);
            }
            FixtureSkills.learn(owner, TrapKind.FLAME.skillId, false);
            var stale = helper.getLevel().getEntitiesOfClass(TrapEntity.class, owner.getBoundingBox().inflate(5),
                    trap -> trap.kind() == TrapKind.BLAST || trap.kind() == TrapKind.FLAME);
            check(stale.isEmpty(), "Unlearning Flame removes dependent Blast's pending deployment");
            helper.succeed();
        }
    }

    private static void reset(ServerPlayer owner, SkillDefinition skill) {
        try {
            var field = SkillRuntime.class.getDeclaredField("nextActivation"); field.setAccessible(true);
            field.setLong(owner.getData(RpgData.SKILL_RUNTIME), 0);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        owner.setData(RpgData.SKILLS, owner.getData(RpgData.SKILLS).withCooldown(skill.id(), 0));
    }
    private static void ticks(TrapEntity trap, int count) {
        for (int i = 0; i < count && !trap.isRemoved(); i++) { trap.tick(); }
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.001, message + ": expected " + expected + ", got " + actual);
    }
    private static void check(boolean value, String message) { if (!value) { throw new IllegalStateException(message); } }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final List<ServerPlayer> players = new ArrayList<>();
        final List<Entity> entities = new ArrayList<>();
        final List<BlockPos> blocks = new ArrayList<>();
        final List<Object> listeners = new ArrayList<>();
        final Vec3 origin;

        Fixture(GameTestHelper helper, int height) {
            this.helper = helper;
            var pos = helper.absolutePos(new BlockPos(2, height, 2));
            origin = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            for (int x = -8; x <= 8; x++) { for (int z = -8; z <= 8; z++) {
                block(BlockPos.containing(origin.add(x, -1, z)), Blocks.STONE.defaultBlockState());
            } }
        }

        ServerPlayer player(int x) {
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "trap-test-player"), false);
            var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            players.add(player); player.setPos(origin.add(x, 0, 0)); player.setNoGravity(true);
            player.setGameMode(GameType.SURVIVAL); player.connection.markClientLoaded();
            player.setYRot(0); player.setXRot(0);
            PlayerProfiles.selectClass(player, PlayerClass.STALKER);
            for (var kind : TrapKind.values()) { FixtureSkills.learn(player, kind.skillId, true); }
            ClassResources.set(player, PlayerClass.STALKER, 100);
            return player;
        }

        LivingEntity target(Vec3 pos) {
            var mob = EntityType.HUSK.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            mob.setPos(pos); mob.setNoAi(true); mob.setNoGravity(true);
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); mob.getAttribute(Attributes.ARMOR).setBaseValue(0);
            mob.setHealth(200); helper.getLevel().addFreshEntity(mob); entities.add(mob); return mob;
        }

        TrapEntity throwSkill(ServerPlayer owner, SkillDefinition skill) {
            var feedback = Skills.activate(owner, skill);
            TrapEntity trap;
            try {
                var field = SkillRuntime.class.getDeclaredField("traps"); field.setAccessible(true);
                trap = (TrapEntity) ((java.util.Map<?, ?>) field.get(owner.getData(RpgData.SKILL_RUNTIME))).get(TrapKind.forSkill(skill));
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            check(trap != null && !trap.isRemoved(), "Activation deploys trap: " + feedback.getString());
            entities.add(trap); return trap;
        }

        TrapEntity ground(ServerPlayer owner, TrapKind kind, Vec3 pos) {
            var trap = new TrapEntity(Skills.TRAP.get(), helper.getLevel());
            try {
                var initialize = TrapEntity.class.getDeclaredMethod("initialize", ServerPlayer.class, TrapKind.class, int.class);
                initialize.setAccessible(true); initialize.invoke(trap, owner, kind, kind.duration);
                helper.getLevel().addFreshEntity(trap);
                var accept = Traps.class.getDeclaredMethod("accept", ServerPlayer.class, TrapEntity.class);
                accept.setAccessible(true); accept.invoke(null, owner, trap);
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            trap.setPos(pos.add(0, 0.05, 0)); trap.setDeltaMovement(0, -0.1, 0); entities.add(trap);
            int falling = 0;
            while (trap.phase() == TrapEntity.Phase.FLIGHT && !trap.isRemoved() && falling++ < 50) { trap.tick(); }
            check(trap.phase() == TrapEntity.Phase.ARMING || trap.isRemoved(), "Ground fixture lands: " + trap.position()); return trap;
        }

        void block(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
            helper.getLevel().setBlockAndUpdate(pos, state); if (!blocks.contains(pos)) { blocks.add(pos.immutable()); }
        }

        void at(int tick, Runnable action) {
            helper.runAtTickTime(tick, () -> {
                try { action.run(); } catch (RuntimeException error) { close(); throw error; }
            });
        }

        @Override public void close() {
            listeners.forEach(net.neoforged.neoforge.common.NeoForge.EVENT_BUS::unregister); listeners.clear();
            entities.forEach(Entity::discard);
            players.forEach(player -> {
                Traps.clear(player, null);
                if (helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player) {
                    helper.getLevel().getServer().getPlayerList().remove(player);
                }
            });
            blocks.forEach(pos -> helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
        }
    }
}
