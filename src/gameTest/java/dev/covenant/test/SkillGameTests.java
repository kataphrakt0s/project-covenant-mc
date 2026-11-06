package dev.covenant.test;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.covenant.rpg.CharacterProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.equipment.AffixPages;
import dev.covenant.rpg.equipment.EquipmentCategory;
import dev.covenant.rpg.equipment.RpgModifier;
import dev.covenant.rpg.skills.ArcaneBolt;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillState;
import dev.covenant.rpg.skills.SkillLoadouts;
import dev.covenant.rpg.skills.Skills;
import dev.covenant.rpg.stats.RpgStats;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.TagValueInput;

/** Dedicated-server tests of actual resource, combat, skill, and native persistence lifecycles. */
final class SkillGameTests {
    private SkillGameTests() {}

    @SuppressWarnings("removal")
    static void ignoreCooldowns(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var players = new ArrayList<ServerPlayer>();
        var player = helper.makeMockServerPlayerInLevel(); players.add(player);
        var combo = helper.makeMockServerPlayerInLevel(); players.add(combo);
        var position = helper.absolutePos(new BlockPos(2, 120, 2));
        player.setPos(position.getX() + .5, position.getY(), position.getZ() + .5); player.setNoGravity(true);
        combo.setPos(player.position().add(12, 0, 0)); combo.setNoGravity(true);
        var dispatcher = server.getCommands().getDispatcher();
        var ordinary = player.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
        var operator = ordinary.withPermission(PermissionSet.ALL_PERMISSIONS);
        try {
            reject(() -> dispatcher.execute("covenant skill ignorecooldowns", ordinary));
            reject(() -> dispatcher.execute("covenant skill ignorecooldowns true", ordinary));
            reject(() -> dispatcher.execute("covenant skill ignorecooldowns", server.createCommandSourceStack()));
            check(!Skills.ignoresCooldowns(player), "Rejected commands preserve default mode");
            dispatcher.execute("covenant skill ignorecooldowns", operator);
            check(Skills.ignoresCooldowns(player) && !Skills.ignoresCooldowns(combo), "Toggle is self-only");
            check(key(Skills.activate(player, SkillDefinition.CLEAVE), "class"), "Cooldown bypass retains class checks");
            PlayerProfiles.selectClass(player, PlayerClass.REAVER);
            check(key(Skills.activate(player, SkillDefinition.CLEAVE), "unlearned"), "Cooldown bypass retains learning checks");
            SkillLoadouts.learn(player, SkillDefinition.CLEAVE.id(), true);
            check(key(Skills.activate(player, SkillDefinition.CLEAVE), "resource"), "Cooldown bypass retains resource checks");
            ClassResources.set(player, PlayerClass.REAVER, 100);
            player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(SkillDefinition.CLEAVE.id(), ClassResources.now(player) + 200));
            check(key(Skills.activate(player, SkillDefinition.CLEAVE), "used"), "Instant skill bypasses preexisting cooldown");
            near(ClassResources.current(player, PlayerClass.REAVER), 80, "Instant skill still spends Rage");
            check(Skills.cooldownRemaining(player, SkillDefinition.CLEAVE) == 60, "Normal timer commits while bypassed");
            check(key(Skills.activate(player, SkillDefinition.CLEAVE), "busy"), "Same-tick throttle remains enforced");
            var before = player.getData(RpgData.SKILLS);
            dispatcher.execute("covenant skill ignorecooldowns false", operator);
            dispatcher.execute("covenant skill ignorecooldowns false", operator);
            check(!Skills.ignoresCooldowns(player) && player.getData(RpgData.SKILLS).equals(before), "Explicit false is idempotent and preserves timers");
            check(key(Skills.activate(player, SkillDefinition.CLEAVE), "cooldown"), "Disabling restores remaining cooldown");
            dispatcher.execute("covenant skill ignorecooldowns true", operator);
            dispatcher.execute("covenant skill ignorecooldowns true", operator);
            check(Skills.ignoresCooldowns(player), "Explicit true is idempotent");
            dispatcher.execute("covenant skill ignorecooldowns", operator);
            check(!Skills.ignoresCooldowns(player), "No-argument command toggles off");
            dispatcher.execute("covenant skill ignorecooldowns", operator);
            var copy = helper.makeMockServerPlayerInLevel(); players.add(copy); copy.restoreFrom(player, false);
            check(!Skills.ignoresCooldowns(copy), "Cooldown flag is not death-copied");
            var reporter = new ProblemReporter.Collector();
            var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, player.registryAccess());
            player.saveWithoutId(output);
            var loaded = helper.makeMockServerPlayerInLevel(); players.add(loaded);
            var data = output.buildResult(); data.remove("UUID");
            loaded.load(TagValueInput.create(reporter, loaded.registryAccess(), data));
            check(reporter.isEmpty() && !Skills.ignoresCooldowns(loaded), "Native save/load omits debug flag");
            PlayerProfiles.switchClass(player, PlayerClass.WEAVER);
            check(Skills.ignoresCooldowns(player), "Class switching preserves debug mode on same entity");
            SkillLoadouts.learn(player, SkillDefinition.ARCANE_BOLT.id(), true);
            player.getAttribute(RpgStats.MANA_REGENERATION).setBaseValue(0);
            ClassResources.set(player, PlayerClass.WEAVER, 100);
            player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(SkillDefinition.ARCANE_BOLT.id(), ClassResources.now(player) + 200));
            PlayerProfiles.selectClass(combo, PlayerClass.REAVER);
            SkillLoadouts.learn(combo, SkillDefinition.LUNGING_COMBO.id(), true);
            ClassResources.set(combo, PlayerClass.REAVER, 100);
            Skills.setIgnoreCooldowns(combo, true);
            combo.setData(RpgData.SKILLS, combo.getData(RpgData.SKILLS).withCooldown(SkillDefinition.LUNGING_COMBO.id(), ClassResources.now(combo) + 200));
            check(key(Skills.activate(combo, SkillDefinition.LUNGING_COMBO), "used"), "Combo opens through existing cooldown");
        } catch (RuntimeException | CommandSyntaxException error) { players.forEach(server.getPlayerList()::remove); throw new IllegalStateException(error); }
        helper.onEachTick(player::doTick);
        helper.onEachTick(combo::doTick);
        helper.runAtTickTime(2, () -> {
            check(key(Skills.activate(player, SkillDefinition.ARCANE_BOLT), "started"), "Cast begins through existing cooldown");
            check(Skills.isCasting(player), "Cooldown bypass retains wind-up");
            near(ClassResources.current(player, PlayerClass.WEAVER), 100, "Cast has not spent early");
            check(key(Skills.activate(player, SkillDefinition.ARCANE_BOLT), "casting"), "Overlapping cast remains rejected");
        });
        helper.runAtTickTime(5, () -> check(key(Skills.activate(combo, SkillDefinition.LUNGING_COMBO), "used"), "Combo strike advances normally"));
        helper.runAtTickTime(7, () -> {
            check(key(Skills.activate(combo, SkillDefinition.LUNGING_COMBO), "used"), "Combo finisher executes");
            check(Skills.cooldownRemaining(combo, SkillDefinition.LUNGING_COMBO) > 0, "Combo still commits end cooldown");
        });
        helper.runAtTickTime(9, () -> check(key(Skills.activate(combo, SkillDefinition.LUNGING_COMBO), "used"), "Completed combo can reopen during cooldown"));
        helper.runAtTickTime(14, () -> {
            check(!Skills.isCasting(player), "Cast completes while bypass enabled");
            near(ClassResources.current(player, PlayerClass.WEAVER), 80, "Completed cast spends normal Mana");
            check(Skills.cooldownRemaining(player, SkillDefinition.ARCANE_BOLT) > 0, "Completed cast commits normal cooldown");
            check(key(Skills.activate(player, SkillDefinition.ARCANE_BOLT), "started"), "Cast can restart while cooldown remains");
            Skills.setIgnoreCooldowns(player, false);
        });
        helper.runAtTickTime(28, () -> {
            try {
                check(!Skills.isCasting(player), "Disabling bypass revalidates cooldown at cast completion");
                near(ClassResources.current(player, PlayerClass.WEAVER), 80, "Rejected cast completion does not spend Mana");
                check(key(Skills.activate(player, SkillDefinition.ARCANE_BOLT), "cooldown"), "Cooldown remains enforced after disabling");
                helper.succeed();
            } finally { players.forEach(server.getPlayerList()::remove); }
        });
    }

    @SuppressWarnings("removal")
    static void infiniteResource(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var players = new ArrayList<ServerPlayer>();
        var player = helper.makeMockServerPlayerInLevel(); players.add(player);
        var comboPlayer = helper.makeMockServerPlayerInLevel(); players.add(comboPlayer);
        var position = helper.absolutePos(new BlockPos(2, 95, 2));
        player.setPos(position.getX() + .5, position.getY(), position.getZ() + .5); player.setNoGravity(true);
        var dispatcher = server.getCommands().getDispatcher();
        var ordinary = player.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
        var operator = ordinary.withPermission(PermissionSet.ALL_PERMISSIONS);
        try {
            reject(() -> dispatcher.execute("covenant resource infinite", ordinary));
            reject(() -> dispatcher.execute("covenant resource infinite true", ordinary));
            reject(() -> dispatcher.execute("covenant resource infinite", server.createCommandSourceStack()));
            check(!ClassResources.infinite(player), "Rejected commands cannot enable infinite resource");
            dispatcher.execute("covenant resource infinite", operator);
            check(ClassResources.infinite(player), "No-argument command toggles on");
            check(!ClassResources.canAfford(player, 1), "Infinite resource cannot bypass missing class");
            PlayerProfiles.selectClass(player, PlayerClass.REAVER);
            for (var type : PlayerClass.all()) {
                PlayerProfiles.switchClass(player, type);
                ClassResources.set(player, type, 0);
                var before = player.getData(RpgData.SKILLS);
                check(ClassResources.canAfford(player, 10000) && ClassResources.consume(player, 10000), "Infinite resource works above capacity for " + type);
                check(player.getData(RpgData.SKILLS).equals(before), "Infinite spending preserves saved balances and cooldowns");
            }
            PlayerProfiles.switchClass(player, PlayerClass.REAVER);
            SkillLoadouts.learn(player, SkillDefinition.CLEAVE.id(), true);
            check(key(Skills.activate(player, SkillDefinition.CLEAVE), "used"), "Zero-Rage instant skill activates");
            check(Skills.cooldownRemaining(player, SkillDefinition.CLEAVE) == 60, "Infinite resource retains cooldown");
            check(key(Skills.activate(player, SkillDefinition.CLEAVE), "cooldown"), "Infinite resource cannot bypass cooldown");
            var other = comboPlayer;
            PlayerProfiles.selectClass(other, PlayerClass.REAVER);
            check(!ClassResources.infinite(other) && !ClassResources.canAfford(other, 1), "Toggle is per player");
            other.setPos(player.position().add(12, 0, 0)); other.setNoGravity(true);
            SkillLoadouts.learn(other, SkillDefinition.LUNGING_COMBO.id(), true);
            dispatcher.execute("covenant resource infinite true", other.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS));
            check(key(Skills.activate(other, SkillDefinition.LUNGING_COMBO), "used"), "Zero-Rage combo opens");
            var copy = helper.makeMockServerPlayerInLevel(); players.add(copy); copy.restoreFrom(player, false);
            check(!ClassResources.infinite(copy), "Infinite debug flag is not death-copied");
            var reporter = new ProblemReporter.Collector();
            var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, player.registryAccess());
            player.saveWithoutId(output);
            var loaded = helper.makeMockServerPlayerInLevel(); players.add(loaded);
            var data = output.buildResult(); data.remove("UUID");
            loaded.load(TagValueInput.create(reporter, loaded.registryAccess(), data));
            check(reporter.isEmpty() && !ClassResources.infinite(loaded), "Native save/load does not retain debug flag");
            dispatcher.execute("covenant resource infinite false", operator);
            dispatcher.execute("covenant resource infinite false", operator);
            check(!ClassResources.infinite(player) && !ClassResources.consume(player, 1), "Explicit false is idempotent and restores resource checks");
            dispatcher.execute("covenant resource infinite true", operator);
            dispatcher.execute("covenant resource infinite true", operator);
            check(ClassResources.infinite(player), "Explicit true is idempotent");
            dispatcher.execute("covenant resource infinite", operator);
            check(!ClassResources.infinite(player), "No-argument command toggles off");
            dispatcher.execute("covenant resource infinite true", operator);
            PlayerProfiles.switchClass(player, PlayerClass.WEAVER);
            ClassResources.set(player, PlayerClass.WEAVER, 0);
            player.getAttribute(RpgStats.MANA_REGENERATION).setBaseValue(0);
            SkillLoadouts.learn(player, SkillDefinition.ARCANE_BOLT.id(), true);
        } catch (RuntimeException | CommandSyntaxException error) { players.forEach(server.getPlayerList()::remove); throw new IllegalStateException(error); }
        helper.onEachTick(player::doTick);
        helper.onEachTick(comboPlayer::doTick);
        helper.runAtTickTime(2, () -> {
            try {
                var result = Skills.activate(player, SkillDefinition.ARCANE_BOLT);
                check(key(result, "started"), "Zero-Mana cast passes affordability check: " + result.getString());
                check(Skills.isCasting(player), "Infinite resource retains cast time");
            } catch (RuntimeException error) { players.forEach(server.getPlayerList()::remove); throw error; }
        });
        helper.runAtTickTime(14, () -> {
            try {
                check(!Skills.isCasting(player) && Skills.cooldownRemaining(player, SkillDefinition.ARCANE_BOLT) > 0, "Zero-Mana cast completes with normal cooldown");
                near(ClassResources.current(player, PlayerClass.WEAVER), 0, "Cast does not spend or fill stored Mana");
                check(key(Skills.activate(comboPlayer, SkillDefinition.LUNGING_COMBO), "used"), "Zero-Rage combo follow-up executes");
                near(ClassResources.current(comboPlayer, PlayerClass.REAVER), 0, "Combo follow-up leaves Rage unchanged");
                player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(SkillDefinition.ARCANE_BOLT.id(), 0));
                check(key(Skills.activate(player, SkillDefinition.ARCANE_BOLT), "started"), "Second zero-Mana cast starts");
                dispatcher.execute("covenant resource infinite false", operator);
            } catch (RuntimeException | CommandSyntaxException error) { players.forEach(server.getPlayerList()::remove); throw new IllegalStateException(error); }
        });
        helper.runAtTickTime(28, () -> {
            try {
                check(!Skills.isCasting(player) && Skills.cooldownRemaining(player, SkillDefinition.ARCANE_BOLT) == 0, "Disabling during cast restores resource check at completion");
                near(ClassResources.current(player, PlayerClass.WEAVER), 0, "Rejected completion preserves balance");
                check(key(Skills.activate(comboPlayer, SkillDefinition.LUNGING_COMBO), "used"), "Zero-Rage combo finisher executes");
                check(Skills.cooldownRemaining(comboPlayer, SkillDefinition.LUNGING_COMBO) == 240, "Infinite combo retains end cooldown");
                helper.succeed();
            } finally { players.forEach(server.getPlayerList()::remove); }
        });
    }

    @SuppressWarnings("removal")
    static void lifecycle(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var players = new ArrayList<ServerPlayer>();
        try {
            var player = helper.makeMockServerPlayerInLevel();
            players.add(player);
            PlayerProfiles.selectClass(player, PlayerClass.STALKER);
            near(ClassResources.current(player, PlayerClass.STALKER), 100, "Focus starts full");
            near(ClassResources.current(player, PlayerClass.REAVER), 0, "Rage starts empty");
            ClassResources.set(player, PlayerClass.STALKER, 10000);
            near(ClassResources.current(player, PlayerClass.STALKER), 100, "Focus hard cap");
            check(ClassResources.consume(player, 63), "Spend active Focus");
            check(!ClassResources.consume(player, 38), "Overspending rejected");
            var profile = PlayerProfiles.get(player);
            PlayerProfiles.switchClass(player, PlayerClass.WEAVER);
            ClassResources.set(player, PlayerClass.WEAVER, 12);
            PlayerProfiles.switchClass(player, PlayerClass.REAVER);
            ClassResources.set(player, PlayerClass.REAVER, 19);
            PlayerProfiles.switchClass(player, PlayerClass.STALKER);
            near(ClassResources.current(player, PlayerClass.STALKER), 37, "Switch restores Focus");
            near(ClassResources.current(player, PlayerClass.WEAVER), 12, "Inactive Mana preserved");
            near(ClassResources.current(player, PlayerClass.REAVER), 19, "Inactive Rage preserved");
            check(PlayerProfiles.get(player).equals(profile), "Switch preserves existing progression fields");

            // Resource stats use the existing native Affix Page pipeline. Focus cannot gain capacity.
            var armor = new ItemStack(Items.IRON_CHESTPLATE);
            AffixPages.attach(helper.getLevel(), armor, EquipmentCategory.BODY_ARMOR,
                    List.of(new RpgModifier(id("mana_capacity"), RpgModifier.Operation.ADD, 1),
                            new RpgModifier(id("mana_regeneration"), RpgModifier.Operation.ADD, 1),
                            new RpgModifier(id("focus_regeneration"), RpgModifier.Operation.ADD, 10)), List.of());
            player.setItemSlot(EquipmentSlot.CHEST, armor);
            player.doTick();
            near(ClassResources.maximum(player, PlayerClass.STALKER), 100, "Equipment cannot raise Focus cap");
            near(ClassResources.regeneration(player, PlayerClass.STALKER), 31.25, "Focus regeneration bonus capped");
            near(ClassResources.maximum(player, PlayerClass.WEAVER), 200, "Mana grows through equipment");
            near(ClassResources.regeneration(player, PlayerClass.WEAVER), 10, "Mana recovery grows through equipment");
            PlayerProfiles.switchClass(player, PlayerClass.WEAVER);
            ClassResources.set(player, PlayerClass.WEAVER, 180);
            var expected = player.getData(RpgData.SKILLS).withCooldown(SkillDefinition.ARCANE_BOLT.id(), ClassResources.now(player) + 80);
            player.setData(RpgData.SKILLS, expected);
            var buffer = Unpooled.buffer();
            try {
                SkillState.STREAM_CODEC.encode(buffer, expected);
                check(SkillState.STREAM_CODEC.decode(buffer).equals(expected), "Owner snapshot network codec");
            } finally { buffer.release(); }
            server.getPlayerList().saveAll();
            var disk = server.getPlayerList().loadPlayerData(player.nameAndId()).orElseThrow();
            disk.remove("UUID");
            var loaded = helper.makeMockServerPlayerInLevel();
            players.add(loaded);
            var reporter = new ProblemReporter.Collector();
            loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
            check(reporter.isEmpty() && loaded.getData(RpgData.SKILLS).equals(expected), "Actual player-file retains all balances/cooldowns");
            loaded.doTick();
            check(ClassResources.current(loaded, PlayerClass.WEAVER) >= 180, "First equipment update does not truncate saved Mana");
            var death = helper.makeMockServerPlayerInLevel();
            players.add(death);
            death.restoreFrom(loaded, false);
            check(death.getData(RpgData.SKILLS).equals(loaded.getData(RpgData.SKILLS)), "Death copies balances/cooldowns");
            var end = helper.makeMockServerPlayerInLevel();
            players.add(end);
            end.restoreFrom(loaded, true);
            check(end.getData(RpgData.SKILLS).equals(loaded.getData(RpgData.SKILLS)), "End return copies balances/cooldowns");

            var dispatcher = server.getCommands().getDispatcher();
            var ordinary = player.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
            var operator = ordinary.withPermission(PermissionSet.ALL_PERMISSIONS);
            dispatcher.execute("covenant resource", ordinary);
            dispatcher.execute("covenant skill", ordinary);
            reject(() -> dispatcher.execute("covenant resource set 99", ordinary));
            reject(() -> dispatcher.execute("covenant class switch covenant:reaver", ordinary));
            reject(() -> dispatcher.execute("covenant skill use covenant:unknown", ordinary));
            reject(() -> dispatcher.execute("covenant skill use covenant:arcane_bolt", server.createCommandSourceStack()));
            reject(() -> dispatcher.execute("covenant resource set NaN", operator));
            dispatcher.execute("covenant class switch covenant:stalker", operator);
            dispatcher.execute("covenant resource set 999", operator);
            near(ClassResources.current(player, PlayerClass.STALKER), 100, "Debug edit is clamped");
            check(dispatcher.getCompletionSuggestions(dispatcher.parse("covenant skill use ", ordinary)).join().getList().size() == SkillDefinition.ALL.size(),
                    "All executable skill suggestions");
            // Legacy active-resource values are imported only until the new balance exists.
            var legacy = helper.makeMockServerPlayerInLevel();
            players.add(legacy);
            PlayerProfiles.initialize(legacy, PlayerClass.WEAVER.id(), Optional.of(id("mana")), 100);
            PlayerProfiles.update(legacy, p -> new CharacterProfile(true, 1, 0, p.selectedClass(), Optional.empty(), p.resourceType(), 42, 100));
            near(ClassResources.current(legacy, PlayerClass.WEAVER), 42, "Legacy balance import");
            ClassResources.set(legacy, PlayerClass.WEAVER, 7);
            near(ClassResources.current(legacy, PlayerClass.WEAVER), 7, "New balance supersedes legacy field");
            helper.succeed();
        } catch (CommandSyntaxException e) { throw new IllegalStateException(e); }
        finally { players.forEach(server.getPlayerList()::remove); }
    }

    @SuppressWarnings("removal")
    static void recovery(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        PlayerProfiles.selectClass(player, PlayerClass.REAVER);
        ClassResources.set(player, PlayerClass.STALKER, 11);
        ClassResources.set(player, PlayerClass.WEAVER, 13);
        var dummy = helper.spawn(EntityType.HUSK, 2, 2, 2);
        dummy.setNoAi(true);
        dummy.setInvulnerable(false);
        dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        player.setPos(dummy.getX() + 1, dummy.getY(), dummy.getZ());
        check(dummy.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 1), "Real damage starts combat");
        helper.onEachTick(player::doTick);
        helper.runAtTickTime(40, () -> {
            near(ClassResources.current(player, PlayerClass.STALKER), 11, "Inactive Focus does not regenerate");
            near(ClassResources.current(player, PlayerClass.WEAVER), 13, "Inactive Mana does not regenerate");
            near(ClassResources.current(player, PlayerClass.REAVER), 1, "One damage grants one Rage with no passive growth");
            ClassResources.set(player, PlayerClass.REAVER, 60);
        });
        helper.runAtTickTime(100, () -> {
            check(dummy.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 1), "A later hit refreshes grace");
            ClassResources.set(player, PlayerClass.REAVER, 20);
        });
        helper.runAtTickTime(200, () -> {
            check(ClassResources.inCombat(player), "Still in refreshed combat grace");
            near(ClassResources.current(player, PlayerClass.REAVER), 20, "Combat grace preserves Rage without generating it");
            dummy.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 0);
        });
        helper.runAtTickTime(241, () -> {
            check(!ClassResources.inCombat(player), "Seven seconds expire; zero damage cannot refresh grace");
            ClassResources.set(player, PlayerClass.REAVER, 60);
        });
        helper.runAtTickTime(281, () -> {
            try {
                nearPulse(ClassResources.current(player, PlayerClass.REAVER), 40, 2.5, "Predictable out-of-combat depletion");
                PlayerProfiles.switchClass(player, PlayerClass.STALKER);
                ClassResources.set(player, PlayerClass.STALKER, 0);
            } catch (RuntimeException e) { helper.getLevel().getServer().getPlayerList().remove(player); throw e; }
        });
        helper.runAtTickTime(321, () -> {
            nearPulse(ClassResources.current(player, PlayerClass.STALKER), 50, 6.25, "Fast Focus recovery");
            PlayerProfiles.switchClass(player, PlayerClass.WEAVER);
            ClassResources.set(player, PlayerClass.WEAVER, 0);
        });
        helper.runAtTickTime(361, () -> {
            try {
                nearPulse(ClassResources.current(player, PlayerClass.WEAVER), 10, 1.25, "Predictable Mana recovery");
                helper.succeed();
            } finally { helper.getLevel().getServer().getPlayerList().remove(player); dummy.discard(); }
        });
    }

    @SuppressWarnings("removal")
    static void effects(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var base = helper.absolutePos(new BlockPos(2, 8, 1));
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        player.setNoGravity(true);
        player.setYRot(0);
        player.setXRot(0);
        PlayerProfiles.selectClass(player, PlayerClass.STALKER);
        SkillLoadouts.learn(player, SkillDefinition.PIERCING_SHOT.id(), true);
        var bowTarget = target(helper, player, 0, 3);
        check(key(Skills.activate(player, SkillDefinition.PIERCING_SHOT), "bow"), "Bow prerequisite");
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        player.getAbilities().instabuild = false;
        check(key(Skills.activate(player, SkillDefinition.PIERCING_SHOT), "arrow"), "Ammunition prerequisite");
        player.getInventory().setItem(5, new ItemStack(Items.ARROW, 8));
        var costStat = player.getAttribute(RpgStats.RESOURCE_COST);
        costStat.addTransientModifier(new AttributeModifier(id("test_cost"), -0.5, AttributeModifier.Operation.ADD_VALUE));
        check(key(Skills.activate(player, SkillDefinition.CLEAVE), "class"), "Class restriction");
        ClassResources.set(player, PlayerClass.STALKER, 0);
        check(key(Skills.activate(player, SkillDefinition.PIERCING_SHOT), "resource"), "Insufficient resource rejection");
        check(!player.swinging, "Rejected skill requests do not play a hand animation");
        check(!player.hasData(RpgData.BOW_DRAW_UNTIL), "Rejected shots do not start a cosmetic bow charge");
        ClassResources.set(player, PlayerClass.STALKER, 100);
        check(key(Skills.activate(player, SkillDefinition.PIERCING_SHOT), "used"), "Bow skill activation");
        check(!player.swinging, "Piercing Shot does not play a melee swing");
        check(player.getData(RpgData.BOW_DRAW_UNTIL) == player.level().getGameTime() + 6,
                "Successful shot synchronizes the brief bow-charge pose");
        check(!player.isUsingItem(), "Cosmetic bow charge does not start actual item use");
        near(ClassResources.current(player, PlayerClass.STALKER), 85, "Cost multiplier applies");
        check(player.getInventory().getItem(5).getCount() == 7 && player.getMainHandItem().getDamageValue() == 1,
                "Survival shot consumes one arrow and wears the bow");
        var before = player.getData(RpgData.SKILLS);
        long drawUntil = player.getData(RpgData.BOW_DRAW_UNTIL);
        check(key(Skills.activate(player, SkillDefinition.PIERCING_SHOT), "cooldown"), "Duplicate shot rejected");
        check(player.getData(RpgData.SKILLS).equals(before), "Duplicate activation does not spend");
        check(player.getData(RpgData.BOW_DRAW_UNTIL) == drawUntil, "Rejected duplicate does not restart the bow charge");
        var shots = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox().inflate(2), a -> a.getOwner() == player);
        check(shots.size() == 1, "One native arrow spawned");
        var shot = shots.getFirst();
        check(shot.hasData(RpgData.SKILL_SHOT) && shot.getData(RpgData.SKILL_SHOT),
                "Skill arrow carries the tracking-synced trail marker");
        var ordinaryArrow = EntityType.ARROW.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        check(!ordinaryArrow.hasData(RpgData.SKILL_SHOT), "Ordinary arrows have no skill trail marker");
        helper.onEachTick(player::doTick);
        helper.runAtTickTime(8, () -> {
            check(bowTarget.getHealth() <= 14, "Enhanced bow projectile damages a single target");
            check(shot.getData(RpgData.SKILL_SHOT), "Piercing arrow retains its trail after an entity impact");
            bowTarget.discard();
            PlayerProfiles.switchClass(player, PlayerClass.REAVER);
            SkillLoadouts.learn(player, SkillDefinition.CLEAVE.id(), true);
            ClassResources.set(player, PlayerClass.REAVER, 50);
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            player.doTick();
            var first = target(helper, player, 1, 1);
            var second = target(helper, player, -1, 1);
            check(key(Skills.activate(player, SkillDefinition.CLEAVE), "used"), "Cleave activation");
            check(first.getHealth() < 20 && second.getHealth() < 20, "Cleave hits multiple nearby enemies");
            first.discard(); second.discard();
            PlayerProfiles.switchClass(player, PlayerClass.WEAVER);
            SkillLoadouts.learn(player, SkillDefinition.ARCANE_BOLT.id(), true);
            ClassResources.set(player, PlayerClass.WEAVER, 100);
        });
        final Husk[] boltTarget = new Husk[1];
        helper.runAtTickTime(10, () -> {
            boltTarget[0] = target(helper, player, 0, 4);
            check(key(Skills.activate(player, SkillDefinition.ARCANE_BOLT), "started"), "Cast begins");
            near(ClassResources.current(player, PlayerClass.WEAVER), 100, "No spend before cast completion");
            check(key(Skills.activate(player, SkillDefinition.ARCANE_BOLT), "casting"), "Overlapping cast rejected");
        });
        helper.runAtTickTime(19, () -> {
            near(ClassResources.current(player, PlayerClass.WEAVER), 100, "Cast waits for deadline");
        });
        helper.runAtTickTime(27, () -> {
            check(boltTarget[0].getHealth() == 14, "Bolt deals six magical damage");
            check(ClassResources.current(player, PlayerClass.WEAVER) < 95, "Cast consumes scaled Mana");
            check(Skills.cooldownRemaining(player, SkillDefinition.ARCANE_BOLT) > 0, "Cooldown starts on completion");
            // Cancel another pending cast via the existing trusted profile API.
            player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(SkillDefinition.ARCANE_BOLT.id(), 0));
            check(key(Skills.activate(player, SkillDefinition.ARCANE_BOLT), "started"), "Second cast begins after cooldown cleared by fixture");
            PlayerProfiles.switchClass(player, PlayerClass.STALKER);
        });
        helper.runAtTickTime(40, () -> {
            check(helper.getLevel().getEntitiesOfClass(ArcaneBolt.class, player.getBoundingBox().inflate(20), b -> b.getOwner() == player).isEmpty(),
                    "Class switch cancels cast without spawning another projectile");
            var bow = new ItemStack(Items.BOW);
            bow.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY), 1);
            player.setItemSlot(EquipmentSlot.MAINHAND, bow);
            int arrows = player.getInventory().getItem(5).getCount();
            check(key(Skills.activate(player, SkillDefinition.PIERCING_SHOT), "used"), "Infinity bow shot");
            check(player.getInventory().getItem(5).getCount() == arrows, "Native Infinity ammunition semantics");
            check(helper.getLevel().getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox().inflate(2), a -> a.getOwner() == player)
                    .stream().allMatch(a -> a.pickup == AbstractArrow.Pickup.CREATIVE_ONLY), "Infinite arrows cannot duplicate survival ammo");
            PlayerProfiles.switchClass(player, PlayerClass.WEAVER);
        });
        helper.runAtTickTime(42, () -> {
            check(key(Skills.activate(player, SkillDefinition.ARCANE_BOLT), "started"), "Cast begins for resource revalidation");
            ClassResources.set(player, PlayerClass.WEAVER, 0);
        });
        helper.runAtTickTime(60, () -> {
            try {
                check(helper.getLevel().getEntitiesOfClass(ArcaneBolt.class, player.getBoundingBox().inflate(20), b -> b.getOwner() == player).isEmpty(),
                        "Insufficient resources at completion do not execute");
                check(Skills.cooldownRemaining(player, SkillDefinition.ARCANE_BOLT) == 0, "Rejected completion starts no cooldown");
                check(ClassResources.current(player, PlayerClass.WEAVER) < 10, "Rejected completion cannot overspend Mana");
                helper.succeed();
            } finally {
                boltTarget[0].discard();
                helper.getLevel().getServer().getPlayerList().remove(player);
            }
        });
    }

    private static Husk target(GameTestHelper helper, ServerPlayer player, double dx, double dz) {
        var target = EntityType.HUSK.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        target.setPos(player.getX() + dx, player.getY(), player.getZ() + dz);
        target.setNoAi(true); target.setNoGravity(true);
        helper.getLevel().addFreshEntity(target);
        return target;
    }

    private static boolean key(Component c, String suffix) {
        return c.getContents() instanceof TranslatableContents text && text.getKey().equals("commands.covenant.skill." + suffix);
    }
    private static void reject(CommandCall call) {
        try { call.run(); } catch (CommandSyntaxException expected) { return; }
        throw new IllegalStateException("Expected command rejection");
    }
    @FunctionalInterface private interface CommandCall { void run() throws CommandSyntaxException; }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("covenant", path); }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.00001, message + ": expected " + expected + ", got " + actual);
    }
    private static void nearPulse(double actual, double expected, double pulse, String message) {
        // Helper callbacks can bracket either side of a resource pulse on their boundary tick.
        check(Math.abs(actual - expected) <= pulse + 0.00001, message + ": expected " + expected + " within one pulse, got " + actual);
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new IllegalStateException(message); } }
}
