package dev.covenant.test;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import dev.covenant.rpg.*;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.progression.*;
import dev.covenant.rpg.skills.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Dedicated-server progression checks use native damage/death, online players, persistence and item use. */
final class ProgressionGameTests {
    static void engine(GameTestHelper helper) {
        var data = ProgressionData.get(helper.getLevel().getServer());
        check(data.required(ProgressionData.CHARACTER, 1) == 50 && data.required(ProgressionData.CHARACTER, 59) == 60000, "Grind curve anchors");
        check(data.gate(helper.getLevel(), ProgressionData.CHARACTER) == 20, "Overworld gate");
        var nether = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        check(data.gate(nether, ProgressionData.CHARACTER) == 40 && data.gate(end, ProgressionData.CHARACTER) == 60, "Vanilla dimension gates");
        check(Progression.advance(1, 49, 1, 60, n -> data.required(ProgressionData.CHARACTER, n)).equals(new Progression.Earned(2, 0, 1)), "Exact threshold");
        check(Progression.advance(19, 0, Long.MAX_VALUE, 20, n -> data.required(ProgressionData.CHARACTER, n)).equals(new Progression.Earned(20, 0, data.required(ProgressionData.CHARACTER, 19))), "Overflow-safe cap discards new surplus");
        check(Progression.advance(21, 1234, 999, 20, n -> 50).equals(new Progression.Earned(21, 1234, 0)), "Lower cap preserves earned state");
        check(Progression.advance(1, 150, 0, 60, n -> 50).equals(new Progression.Earned(1, 150, 0)), "Reload alone never normalizes XP");
        check(Progression.advance(1, 150, 1, 60, n -> 50).equals(new Progression.Earned(4, 1, 1)), "Real new award normalizes retained XP");
        check(Progression.advance(1, Long.MAX_VALUE - 1, 10, 3, n -> Long.MAX_VALUE).equals(new Progression.Earned(2, 9, 10)), "No addition overflow");
        try (var reader = helper.getLevel().getServer().getResourceManager().getResourceOrThrow(ProgressionData.FILE).openAsReader()) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            var invalid = json.deepCopy(); invalid.getAsJsonArray("character_requirements").set(0, new com.google.gson.JsonPrimitive(0));
            final var zero = invalid;
            rejects(() -> ProgressionData.parse(zero), "Zero threshold rejected");
            invalid = json.deepCopy(); invalid.addProperty("hostile_reward", 1.5); final var fraction = invalid;
            rejects(() -> ProgressionData.parse(fraction), "Fractional reward rejected");
            invalid = json.deepCopy(); invalid.getAsJsonObject("track_caps").addProperty("covenant:unknown", 3); final var unknown = invalid;
            rejects(() -> ProgressionData.parse(unknown), "Unknown track rejected");
            check(ProgressionData.get(helper.getLevel().getServer()) == data, "Parsing cannot mutate live balance");
            var rules = json.deepCopy();
            rules.getAsJsonArray("reward_tags").add(JsonParser.parseString("{\"tag\":\"covenant_test:reward_set\",\"priority\":1,\"xp\":7}"));
            rules.getAsJsonArray("reward_tags").add(JsonParser.parseString("{\"tag\":\"covenant_test:reward_set\",\"priority\":2,\"xp\":9}"));
            rules.getAsJsonArray("dimensions").add(JsonParser.parseString("{\"match\":\"#covenant_test:vanilla_dimensions\",\"priority\":1,\"caps\":{\"covenant:character\":30,\"covenant:mining\":5}}"));
            rules.getAsJsonArray("dimensions").add(JsonParser.parseString("{\"match\":\"#covenant_test:second_dimensions\",\"priority\":2,\"caps\":{\"covenant:character\":40,\"covenant:mining\":7}}"));
            var tagged = ProgressionData.parse(rules);
            check(tagged.gate(helper.getLevel(), ProgressionData.CHARACTER) == 20 && tagged.gate(helper.getLevel(), ProgressionData.PROFESSIONS.getFirst()) == 7, "Exact dimension wins for its track, tag priority decides independent profession gate");
            try (var f = new Fixture(helper)) {
                check(tagged.reward(f.mob(EntityType.HUSK)) == 9 && tagged.reward(f.mob(EntityType.ENDERMAN)) == 20, "Highest reward tag wins, exact entity override wins over tags");
                var player = f.player(PlayerClass.STALKER); PlayerProfiles.update(player, p -> p.withProgression(20, 7, false));
                Progression.awardCharacter(player, end, 10); check(xp(player) == 17, "Source dimension controls award while recipient remains in Overworld");
                PlayerProfiles.update(player, p -> p.withProgression(59, 59000, true)); Progression.awardCharacter(player, end, 1000);
                check(PlayerProfiles.get(player).level() == 60 && xp(player) == 0 && SkillLoadouts.availablePoints(player) == 60, "Global cap and earned point entitlement");
            }
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
        helper.succeed();
    }

    static void rewards(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var a = f.player(PlayerClass.STALKER); var b = f.player(PlayerClass.REAVER); var c = f.player(PlayerClass.WEAVER);
            var target = f.mob(EntityType.HUSK);
            f.damage(target, a, 1); f.damage(target, a, 1); f.damage(target, b, 1); f.kill(target, c);
            check(xp(a) == 4 && xp(b) == 4 && xp(c) == 4, "Equal rounded-up shares, once per player, includes killing blow");
            CombatProgression.died(target, a.damageSources().playerAttack(a)); check(xp(a) == 4, "Duplicate death boundary cannot repay");
            var classless = f.player(null); target = f.mob(EntityType.HUSK); f.kill(target, classless);
            check(!PlayerProfiles.get(classless).created(), "Classless kill grants and banks nothing");
            PlayerProfiles.selectClass(classless, PlayerClass.STALKER); check(xp(classless) == 0, "Selecting class has no retroactive payout");
            target = f.mob(EntityType.HUSK); f.damage(target, classless, 1);
            PlayerProfiles.update(a, p -> p.withProgression(20, 7, false)); f.kill(target, a);
            check(xp(classless) == 5 && xp(a) == 7 && PlayerProfiles.get(a).level() == 20, "Capped player counts but blocked share not redistributed");
            target = f.mob(EntityType.HUSK); f.damage(target, b, 1);
            helper.getLevel().getServer().getPlayerList().remove(b); f.kill(target, c);
            check(xp(c) == 14 && xp(b) == 4, "Offline contributor excluded, no queued award");
            var cow = f.mob(EntityType.COW); f.kill(cow, c); check(xp(c) == 14, "Livestock excluded");
            var enderman = f.mob(EntityType.ENDERMAN); f.kill(enderman, c); check(xp(c) == 34, "Enderman neutral opt-in gets 20 XP");
            var dead = f.player(PlayerClass.REAVER); target = f.mob(EntityType.HUSK); f.damage(target, dead, 1);
            dead.hurtServer(helper.getLevel(), dead.damageSources().fellOutOfWorld(), 10000);
            check(!dead.isAlive(), "Contributor died before payout"); f.kill(target, c);
            check(xp(dead) == 5, "Contributor death preserves enemy-owned participation");
            helper.succeed();
        }
    }

    static void support(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var fighter = f.player(PlayerClass.STALKER); var a = f.player(PlayerClass.REAVER); var b = f.player(PlayerClass.REAVER);
            f.team(fighter, a, b); f.equip(a, SkillDefinition.FORTITUDE_AURA); f.equip(b, SkillDefinition.FORTITUDE_AURA);
            Auras.refresh(a); Auras.refresh(b);
            var target = f.mob(EntityType.HUSK); f.kill(target, fighter);
            check(xp(fighter) == 4 && xp(a) == 4 && xp(b) == 4, "All overlapping passive suppliers qualify without attacking or activating");
            target = f.mob(EntityType.HUSK); f.damage(target, fighter, 1);
            f.equip(a, SkillDefinition.RAGING_AURA); // Unbinding Fortitude does not erase previously earned participation.
            f.kill(target, fighter); check(xp(a) == 8, "Recorded support remains after coverage ends");
            var control = f.player(PlayerClass.WEAVER); target = f.mob(EntityType.HUSK);
            CombatProgression.supportEnemy(control, target); f.kill(target, fighter);
            check(xp(control) > 0 && ClassResources.inCombat(control), "Support-only control qualifies and starts shared timer");
            helper.succeed();
        }
    }

    static void wither(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var a = f.player(PlayerClass.STALKER); var b = f.player(PlayerClass.WEAVER);
            var data = ProgressionData.get(helper.getLevel().getServer()); long required = data.required(ProgressionData.CHARACTER, 20);
            PlayerProfiles.update(a, p -> p.withProgression(20, required / 2, false));
            var boss = f.mob(EntityType.WITHER); CombatProgression.supportEnemy(b, boss); f.kill(boss, a);
            var profile = PlayerProfiles.get(a);
            check(profile.level() == 21 && profile.witherClaimed(), "First Wither bypasses dimension cap exactly once");
            check(profile.experience() == java.math.BigInteger.valueOf(required / 2).multiply(java.math.BigInteger.valueOf(data.required(ProgressionData.CHARACTER, 21)))
                    .divide(java.math.BigInteger.valueOf(required)).longValue(), "Preserves within-level fraction rounded down");
            check(PlayerProfiles.get(b).level() == 2 && PlayerProfiles.get(b).witherClaimed() && xp(b) == 0, "Support recipient gets milestone instead of ordinary share");
            long retained = xp(a); f.kill(f.mob(EntityType.WITHER), a);
            check(PlayerProfiles.get(a).level() == 21 && xp(a) == retained, "Repeat Wither obeys normal source cap");
            PlayerProfiles.update(b, p -> p.withProgression(60, 0, false)); f.kill(f.mob(EntityType.WITHER), b);
            check(!PlayerProfiles.get(b).witherClaimed(), "Global-cap kill cannot consume pending milestone");
            var server = helper.getLevel().getServer(); server.getPlayerList().saveAll();
            var disk = server.getPlayerList().loadPlayerData(a.nameAndId()).orElseThrow(); disk.remove("UUID");
            var loaded = f.player(null); var reporter = new ProblemReporter.Collector(); loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
            check(reporter.isEmpty() && PlayerProfiles.get(loaded).equals(profile), "Level XP and Wither claim survive actual player file");
            for (boolean endReturn : new boolean[]{false, true}) { var clone = f.player(null); clone.restoreFrom(loaded, endReturn); check(PlayerProfiles.get(clone).equals(profile), "Milestone respawn/End copy"); }
            var repeat = f.player(PlayerClass.REAVER); var first = f.player(PlayerClass.STALKER);
            PlayerProfiles.update(repeat, p -> p.withProgression(1, 0, true));
            boss = f.mob(EntityType.WITHER); CombatProgression.supportEnemy(first, boss); f.kill(boss, repeat);
            var ordinary = Progression.advance(1, 0, 1000, 20, level -> data.required(ProgressionData.CHARACTER, level));
            check(PlayerProfiles.get(repeat).level() == ordinary.level() && xp(repeat) == ordinary.experience()
                    && PlayerProfiles.get(first).level() == 2 && xp(first) == 0, "Milestone replaces one share, not the repeat player's denominator");
            helper.succeed();
        }
    }

    static void defenses(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var fighter = f.player(PlayerClass.STALKER); var support = f.player(PlayerClass.REAVER); f.team(fighter, support);
            f.equip(support, SkillDefinition.FORTITUDE_AURA); ClassResources.set(support, PlayerClass.REAVER, 100);
            Skills.activate(support, SkillDefinition.FORTITUDE_AURA);
            var target = f.mob(EntityType.HUSK); float health = fighter.getHealth();
            fighter.hurtServer(helper.getLevel(), target.damageSources().mobAttack(target), 2);
            check(fighter.getHealth() == health && Auras.shield(fighter) == 4, "Enemy hit fully absorbed by supplied Fortitude shield");
            f.kill(target, fighter); check(xp(support) == 5 && xp(fighter) == 5, "Fully absorbed support earns an equal share");
        }
        try (var f = new Fixture(helper)) {
            var fighter = f.player(PlayerClass.STALKER); var control = f.player(PlayerClass.WEAVER); var target = f.mob(EntityType.HUSK);
            f.equip(control, SkillDefinition.FREEZING_AURA); Skills.activate(control, SkillDefinition.FREEZING_AURA);
            check(Auras.frozen(target) && ClassResources.inCombat(control), "Native successful freeze records support and combat state");
            f.kill(target, fighter); check(xp(control) == 5, "Control-only participant receives normal share without damage");
        }
        try (var f = new Fixture(helper)) {
            var control = f.player(PlayerClass.WEAVER); var victim = f.player(PlayerClass.STALKER);
            f.equip(control, SkillDefinition.FREEZING_AURA); Skills.activate(control, SkillDefinition.FREEZING_AURA);
            check(Auras.frozen(victim) && ClassResources.inCombat(control) && xp(control) == 0,
                    "Qualifying PvP support refreshes generic combat state even though PvP awards no XP");
        }
        helper.succeed();
    }

    static void deathDecision(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var player = f.player(PlayerClass.REAVER); var target = f.mob(EntityType.HUSK);
            java.util.function.Consumer<LivingDeathEvent> cancel = event -> { if (event.getEntity() == target) { event.setCanceled(true); } };
            NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, cancel);
            try { f.kill(target, player); check(xp(player) == 0, "Canceled death gives no reward"); }
            finally { NeoForge.EVENT_BUS.unregister(cancel); }
            target.setHealth(1); f.kill(target, player); check(xp(player) == 10, "Accepted native death pays once after cancellation");
            helper.succeed();
        }
    }

    static void timer(GameTestHelper helper) {
        var f = new Fixture(helper); var player = f.player(PlayerClass.STALKER); var target = f.mob(EntityType.HUSK);
        f.damage(target, player, 1); check(ClassResources.inCombat(player), "Non-Reaver damage starts seven seconds");
        helper.runAfterDelay(141, () -> {
            check(!ClassResources.inCombat(player), "Generic timer expires after seven seconds");
            SkillLoadouts.apply(player, Map.of(), Map.of(SkillDefinition.PIERCING_SHOT.id(), 1));
            check(player.getData(RpgData.SKILL_LOADOUT).spent() == 1, "Apply permitted after grace");
        });
        helper.runAfterDelay(601, () -> {
            try { target.invulnerableTime = 0; target.hurtServer(helper.getLevel(), target.damageSources().generic(), 10000); check(xp(player) == 0, "Thirty-second credit expiry checked at death"); helper.succeed(); }
            finally { f.close(); }
        });
    }

    static void professions(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var player = f.player(null); var initial = player.getData(RpgData.PROFESSIONS);
            check(initial.tracks().size() == 7 && initial.tracks().values().stream().allMatch(t -> t.level() == 0 && t.experience() == 0), "Seven classless profession tracks initialized");
            var mining = ProgressionData.PROFESSIONS.getFirst();
            check(Progression.awardProfession(player, helper.getLevel(), mining, 20), "Provisional shared profession curve accepts trusted classless awards");
            check(player.getData(RpgData.PROFESSIONS).tracks().get(mining).experience() == 20, "Profession XP is independent of class creation");
            var state = initial.with(mining, new ProfessionState.Track(3, 17)); player.setData(RpgData.PROFESSIONS, state);
            PlayerProfiles.selectClass(player, PlayerClass.REAVER); check(player.getData(RpgData.PROFESSIONS).equals(state), "Class selection independent");
            var clone = f.player(null); clone.restoreFrom(player, false);
            check(clone.getData(RpgData.PROFESSIONS).equals(state) && state.tracks().get(mining).points() == 3, "Profession state and earned entitlement copy on death");
            helper.succeed();
        }
    }

    static void arrowRanks(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var baseline = f.player(PlayerClass.STALKER); var upgraded = f.player(PlayerClass.STALKER);
            PlayerProfiles.update(upgraded, p -> Progression.atLevel(p, 8));
            var id = SkillDefinition.PIERCING_SHOT.id();
            SkillLoadouts.apply(baseline, Map.of(), Map.of(id, 1)); SkillLoadouts.apply(upgraded, Map.of(), Map.of(id, 8));
            for (var player : List.of(baseline, upgraded)) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW)); player.getInventory().setItem(5, new ItemStack(Items.ARROW, 8));
                player.setYRot(0); player.setXRot(0); player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                Skills.activate(player, SkillDefinition.PIERCING_SHOT);
            }
            var arrows = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class, baseline.getBoundingBox().inflate(4));
            var first = arrows.stream().filter(arrow -> arrow.getOwner() == baseline).findFirst().orElseThrow();
            var last = arrows.stream().filter(arrow -> arrow.getOwner() == upgraded).findFirst().orElseThrow();
            f.mobs.addAll(arrows);
            check(Math.abs(first.getDeltaMovement().length() - 3) < .00001 && Math.abs(last.getDeltaMovement().length() - 3.42) < .00001, "Rank eight adds fourteen percent launch velocity");
            check(((dev.covenant.mixin.ArrowPiercingAccessor) first).covenant$getBaseDamage() == ((dev.covenant.mixin.ArrowPiercingAccessor) last).covenant$getBaseDamage(), "No flat per-rank damage coefficient");
            check(ClassResources.current(baseline, PlayerClass.STALKER) == ClassResources.current(upgraded, PlayerClass.STALKER), "Rank leaves cost unchanged");
            helper.succeed();
        }
    }

    static void respec(GameTestHelper helper) {
        var f = new Fixture(helper); var player = f.player(PlayerClass.STALKER);
        SkillLoadouts.apply(player, Map.of(), Map.of(SkillDefinition.PIERCING_SHOT.id(), 1)); SkillLoadouts.bind(player, 0, SkillDefinition.PIERCING_SHOT.id());
        var stack = new ItemStack(CombatRespecItem.ITEM.get(), 2); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        player.stopUsingItem(); check(stack.getCount() == 2 && player.getData(RpgData.SKILL_LOADOUT).spent() == 1, "Early release spends nothing");
        stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND); ClassResources.markCombat(player);
        stack.getItem().onUseTick(player.level(), player, stack, 30);
        check(!player.isUsingItem() && stack.getCount() == 2 && player.getData(RpgData.SKILL_LOADOUT).spent() == 1, "Combat interrupts without consuming");
        player.removeData(RpgData.SKILL_RUNTIME); stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.onEachTick(player::doTick);
        helper.runAfterDelay(42, () -> {
            try { check(stack.getCount() == 1 && player.getData(RpgData.SKILL_LOADOUT).ranks().isEmpty(), "Native two-second use consumes one and refunds all"); helper.succeed(); }
            finally { f.close(); }
        });
    }

    static final class Fixture implements AutoCloseable {
        final GameTestHelper helper; final List<ServerPlayer> players = new ArrayList<>(); final List<Entity> mobs = new ArrayList<>();
        final List<net.minecraft.world.scores.PlayerTeam> teams = new ArrayList<>();
        Fixture(GameTestHelper helper) { this.helper = helper; }
        ServerPlayer player(PlayerClass type) {
            return player(type, null);
        }
        ServerPlayer player(PlayerClass type, List<net.minecraft.network.protocol.Packet<?>> packets) {
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "xp" + UUID.randomUUID().toString().substring(0, 8)), false);
            var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            var connection = packets == null ? new Connection(PacketFlow.SERVERBOUND) : new Connection(PacketFlow.SERVERBOUND) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet, io.netty.channel.ChannelFutureListener listener, boolean flush) {
                    packets.add(packet); super.send(packet, listener, flush);
                }
                @Override public boolean isMemoryConnection() { return true; }
            };
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            if (packets != null) { net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection); }
            helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            player.setGameMode(GameType.SURVIVAL); player.connection.markClientLoaded();
            player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 100, 2))); player.setNoGravity(true);
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            if (type != null) { PlayerProfiles.selectClass(player, type); } players.add(player); return player;
        }
        <T extends net.minecraft.world.entity.Mob> T mob(EntityType<T> type) {
            var mob = type.create(helper.getLevel(), EntitySpawnReason.COMMAND); mob.setNoAi(true); mob.setNoGravity(true);
            mob.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 100, 4)));
            mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            if (mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss wither) { wither.setInvulnerableTicks(0); }
            helper.getLevel().addFreshEntity(mob); mobs.add(mob); return mob;
        }
        void damage(LivingEntity mob, ServerPlayer player, float amount) { mob.invulnerableTime = 0; mob.hurtServer(helper.getLevel(), mob.damageSources().playerAttack(player), amount); }
        void kill(LivingEntity mob, ServerPlayer player) { damage(mob, player, 10000); }
        void team(ServerPlayer... members) {
            var board = helper.getLevel().getScoreboard(); var team = board.addPlayerTeam("xp" + UUID.randomUUID().toString().substring(0, 8)); teams.add(team);
            for (var player : members) { board.addPlayerToTeam(player.getScoreboardName(), team); }
        }
        void equip(ServerPlayer player, SkillDefinition skill) {
            SkillTree.find(skill.id()).orElseThrow().prerequisite().ifPresent(id -> FixtureSkills.learn(player, id, true));
            FixtureSkills.learn(player, skill.id(), true); SkillLoadouts.bind(player, 0, skill.id()); Auras.reconcile(player);
        }
        @Override public void close() {
            for (var player : players) { Auras.clear(player); if (helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == player) { helper.getLevel().getServer().getPlayerList().remove(player); } }
            mobs.forEach(Entity::discard); teams.forEach(helper.getLevel().getScoreboard()::removePlayerTeam);
        }
    }
    private static long xp(ServerPlayer player) { return PlayerProfiles.get(player).experience(); }
    private static void rejects(Runnable action, String message) { try { action.run(); } catch (RuntimeException expected) { return; } throw new IllegalStateException(message); }
    private static void check(boolean value, String message) { if (!value) { throw new IllegalStateException(message); } }
}
