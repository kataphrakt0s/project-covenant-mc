package dev.covenant.test;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLoadouts;
import dev.covenant.rpg.skills.Skills;
import dev.covenant.rpg.stats.RpgStats;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Native collisions and authoritative cast cleanup for the first executable tree expansion. */
final class SkillFoundationGameTests {
    private SkillFoundationGameTests() {}

    @SuppressWarnings("removal")
    static void run(GameTestHelper helper) {
        var players = new ArrayList<ServerPlayer>();
        var targets = new ArrayList<LivingEntity>();
        // Long horizontal lanes extend beyond the empty structure; isolate from other skill fixtures.
        var origin = helper.absolutePos(new BlockPos(2, 20, 2));
        var piercing = player(helper, players, origin, 0, PlayerClass.STALKER);
        learnBow(piercing);
        var line = new ArrayList<Husk>();
        for (int i = 0; i < 4; i++) { line.add(target(helper, targets, piercing, 0, 3 + 2 * i)); }
        check(key(Skills.activate(piercing, SkillDefinition.PIERCING_SHOT), "used"), "Piercing Shot fires");
        check(arrows(piercing).getFirst().getPierceLevel() == 2, "Native piercing allows three targets total");

        var blocked = player(helper, players, origin, 10, PlayerClass.STALKER);
        learnBow(blocked);
        var behindWall = target(helper, targets, blocked, 0, 6);
        var wall = BlockPos.containing(blocked.getX(), blocked.getEyeY(), blocked.getZ() + 3);
        helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        check(key(Skills.activate(blocked, SkillDefinition.PIERCING_SHOT), "used"), "Blocked shot still fires");
        var wallArrow = arrows(blocked).getFirst();

        var reaver = player(helper, players, origin, 20, PlayerClass.REAVER);
        SkillLoadouts.learn(reaver, SkillDefinition.CLEAVE.id(), true);
        ClassResources.set(reaver, PlayerClass.REAVER, 100);
        reaver.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        reaver.doTick();
        var front = target(helper, targets, reaver, 0, 2);
        var insideEdge = target(helper, targets, reaver, 1.7, 1);
        var outsideEdge = target(helper, targets, reaver, 1.8, 1);
        var rear = target(helper, targets, reaver, 0, -2);
        var far = target(helper, targets, reaver, 0, 4);
        var hidden = target(helper, targets, reaver, -1, 2);
        helper.getLevel().setBlockAndUpdate(BlockPos.containing(reaver.getX() - 1, reaver.getEyeY(), reaver.getZ() + 1),
                Blocks.STONE.defaultBlockState());
        reaver.setXRot(-90);
        check(key(Skills.activate(reaver, SkillDefinition.CLEAVE), "used"), "Cleave ignores pitch");
        check(front.getHealth() < 20 && insideEdge.getHealth() < 20, "Cleave hits forward targets inside 60 degrees");
        check(outsideEdge.getHealth() == 20 && rear.getHealth() == 20 && far.getHealth() == 20 && hidden.getHealth() == 20,
                "Cleave rejects outside-arc, rear, distant, and occluded targets");
        front.discard(); insideEdge.discard(); outsideEdge.discard(); far.discard(); hidden.discard();
        reaver.setData(RpgData.SKILLS, reaver.getData(RpgData.SKILLS).withCooldown(SkillDefinition.CLEAVE.id(), 0));
        reaver.setData(RpgData.SKILL_RUNTIME, new dev.covenant.rpg.skills.SkillRuntime());
        var rage = ClassResources.current(reaver, PlayerClass.REAVER);
        check(key(Skills.activate(reaver, SkillDefinition.CLEAVE), "used"), "Only rear target produces a paid Cleave whiff");
        near(ClassResources.current(reaver, PlayerClass.REAVER), rage - 20, "Empty forward sweep spends Rage");

        var aimed = player(helper, players, origin, 35, PlayerClass.STALKER);
        learnBow(aimed);
        SkillLoadouts.learn(aimed, SkillDefinition.AIMED_SHOT.id(), true);
        aimed.getAttribute(RpgStats.FOCUS_REGENERATION).setBaseValue(0);
        double speed = aimed.getAttributeValue(Attributes.MOVEMENT_SPEED);
        var oldAim = target(helper, targets, aimed, 0, 4);
        var newAim = target(helper, targets, aimed, 5, 0);
        // This lane extends beyond the tiny GameTest structure's ticking chunks. Explicit fixture
        // tickets keep the real arrow ticking regardless of the randomly chosen test-grid alignment.
        var forced = new java.util.HashSet<net.minecraft.world.level.ChunkPos>();
        for (var entity : List.of(aimed, oldAim, newAim)) {
            var chunk = entity.chunkPosition();
            if (helper.getLevel().setChunkForced(chunk.x(), chunk.z(), true)) { forced.add(chunk); }
        }
        check(key(Skills.activate(aimed, SkillDefinition.AIMED_SHOT), "started"), "Aimed Shot begins");
        near(ClassResources.current(aimed, PlayerClass.STALKER), 100, "Wind-up does not spend");
        near(aimed.getAttributeValue(Attributes.MOVEMENT_SPEED), speed * 0.2, "Wind-up slows movement to 20 percent");
        check(arrows(aimed).isEmpty() && !aimed.isUsingItem(), "No early arrow or actual vanilla item use");
        helper.onEachTick(aimed::doTick);
        helper.runAtTickTime(9, () -> {
            check(line.subList(0, 3).stream().allMatch(t -> t.getHealth() < 20) && line.getLast().getHealth() == 20,
                    "Real arrow damages exactly three aligned enemies");
            check(behindWall.getHealth() == 20 && wallArrow.getPierceLevel() == 0, "Native block collision stops Piercing Shot");
            check(!wallArrow.hasData(RpgData.SKILL_SHOT), "Block impact ends the visual trail");
            check(arrows(aimed).isEmpty(), "Aimed Shot waits through the wind-up");
            // Moving is permitted and aim is read at completion. New aim points across the original line.
            aimed.setPos(aimed.getX() + 1, aimed.getY(), aimed.getZ());
            aimed.setYRot(-90);
            aimed.getAttribute(RpgStats.RESOURCE_COST).setBaseValue(0.5);
        });
        helper.runAtTickTime(27, () -> {
            check(oldAim.getHealth() == 20 && newAim.getHealth() <= 9, "One stronger arrow follows updated aim after moving: old="
                    + oldAim.getHealth() + ", new=" + newAim.getHealth() + ", player=" + aimed.position()
                    + ", target=" + newAim.position() + ", yaw=" + aimed.getYRot() + ", arrows="
                    + arrows(aimed).stream().map(a -> a.position() + " velocity=" + a.getDeltaMovement()).toList());
            near(ClassResources.current(aimed, PlayerClass.STALKER), 60, "Cast uses snapshotted cost despite a later stat edit");
            check(aimed.getInventory().getItem(5).getCount() == 7 && aimed.getMainHandItem().getDamageValue() == 1,
                    "Aimed Shot consumes one arrow and one durability");
            near(aimed.getAttributeValue(Attributes.MOVEMENT_SPEED), speed, "Successful cast restores speed");
            check(Skills.cooldownRemaining(aimed, SkillDefinition.AIMED_SHOT) > 0, "Cooldown begins at completion");
            prepare(aimed);
            check(key(Skills.activate(aimed, SkillDefinition.AIMED_SHOT), "started"), "Second wind-up begins");
            SkillLoadouts.learn(aimed, SkillDefinition.PIERCING_SHOT.id(), false);
            near(aimed.getAttributeValue(Attributes.MOVEMENT_SPEED), speed, "Removing prerequisite immediately restores speed");
            check(aimed.getData(RpgData.BOW_DRAW_UNTIL) == 0, "Unlearning clears the pose");
            learnBow(aimed);
            SkillLoadouts.learn(aimed, SkillDefinition.AIMED_SHOT.id(), true);
        });
        helper.runAtTickTime(29, () -> {
            check(key(Skills.activate(aimed, SkillDefinition.AIMED_SHOT), "started"), "Equipment interruption begins");
            aimed.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        });
        helper.runAtTickTime(32, () -> {
            near(aimed.getAttributeValue(Attributes.MOVEMENT_SPEED), speed, "Losing bow cancels and restores speed");
            near(ClassResources.current(aimed, PlayerClass.STALKER), 100, "Equipment interruption spends nothing");
            check(Skills.cooldownRemaining(aimed, SkillDefinition.AIMED_SHOT) == 0, "Equipment interruption has no cooldown");
            aimed.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            check(key(Skills.activate(aimed, SkillDefinition.AIMED_SHOT), "started"), "Resource revalidation begins");
            ClassResources.set(aimed, PlayerClass.STALKER, 0);
        });
        helper.runAtTickTime(56, () -> {
            near(aimed.getAttributeValue(Attributes.MOVEMENT_SPEED), speed, "Failed resource revalidation restores speed");
            check(Skills.cooldownRemaining(aimed, SkillDefinition.AIMED_SHOT) == 0, "Failed completion has no cooldown");
            prepare(aimed);
            check(key(Skills.activate(aimed, SkillDefinition.AIMED_SHOT), "started"), "Class interruption begins");
            PlayerProfiles.switchClass(aimed, PlayerClass.WEAVER);
            near(aimed.getAttributeValue(Attributes.MOVEMENT_SPEED), speed, "Class switch immediately restores speed");
            check(aimed.getData(RpgData.BOW_DRAW_UNTIL) == 0, "Class switch clears the pose");
        });
        helper.runAtTickTime(82, () -> {
            try {
                check(arrows(aimed).isEmpty(), "Canceled casts never fire later");
                near(ClassResources.current(aimed, PlayerClass.STALKER), 100, "Canceled casts preserve inactive Focus");
                helper.succeed();
            } finally {
                forced.forEach(chunk -> helper.getLevel().setChunkForced(chunk.x(), chunk.z(), false));
                targets.forEach(LivingEntity::discard);
                players.forEach(helper.getLevel().getServer().getPlayerList()::remove);
                helper.getLevel().setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
            }
        });
    }

    private static void prepare(ServerPlayer player) {
        ClassResources.set(player, PlayerClass.STALKER, 100);
        player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(SkillDefinition.AIMED_SHOT.id(), 0));
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, List<ServerPlayer> players, BlockPos origin, int x, PlayerClass type) {
        var player = helper.makeMockServerPlayerInLevel();
        players.add(player);
        player.setPos(origin.getX() + x + 0.5, origin.getY(), origin.getZ() + 0.5);
        player.setNoGravity(true);
        player.setYRot(0); player.setXRot(0);
        player.getAbilities().instabuild = false;
        PlayerProfiles.selectClass(player, type);
        return player;
    }

    private static void learnBow(ServerPlayer player) {
        SkillLoadouts.learn(player, SkillDefinition.PIERCING_SHOT.id(), true);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        player.getInventory().setItem(5, new ItemStack(Items.ARROW, 8));
    }

    private static Husk target(GameTestHelper helper, List<LivingEntity> targets, ServerPlayer player, double x, double z) {
        var target = EntityType.HUSK.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        target.setPos(player.getX() + x, player.getY(), player.getZ() + z);
        target.setNoAi(true); target.setNoGravity(true);
        helper.getLevel().addFreshEntity(target);
        targets.add(target);
        return target;
    }

    private static List<AbstractArrow> arrows(ServerPlayer player) {
        return player.level().getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox().inflate(12), a -> a.getOwner() == player);
    }

    private static boolean key(Component c, String suffix) {
        return c.getContents() instanceof TranslatableContents text && text.getKey().equals("commands.covenant.skill." + suffix);
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.00001, message + ": expected " + expected + ", got " + actual);
    }
    private static void check(boolean value, String message) { if (!value) { throw new IllegalStateException(message); } }
}
