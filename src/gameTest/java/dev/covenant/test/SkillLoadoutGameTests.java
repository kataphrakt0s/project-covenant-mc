package dev.covenant.test;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.ArcaneBolt;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLoadout;
import dev.covenant.rpg.skills.SkillLoadouts;
import dev.covenant.rpg.skills.SkillNetworking;
import dev.covenant.rpg.skills.Skills;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

/** Exercises the same authoritative operations used by the three client intent packets. */
final class SkillLoadoutGameTests {
    private SkillLoadoutGameTests() {}

    @SuppressWarnings("removal")
    static void run(GameTestHelper helper) {
        var players = new ArrayList<ServerPlayer>();
        var server = helper.getLevel().getServer();
        var player = helper.makeMockServerPlayerInLevel();
        players.add(player);
        boolean scheduled = false;
        try {
            var bolt = SkillDefinition.ARCANE_BOLT;
            var shot = SkillDefinition.PIERCING_SHOT;
            var unknown = Identifier.fromNamespaceAndPath("covenant", "unknown");
            check(player.getData(RpgData.SKILL_LOADOUT).equals(SkillLoadout.EMPTY), "Existing/new players default to no learned skills");
            check(key(SkillLoadouts.learn(player, bolt.id(), true), "commands.covenant.skill.class"), "Class required to learn");
            PlayerProfiles.selectClass(player, PlayerClass.WEAVER);
            var before = player.getData(RpgData.SKILL_LOADOUT);
            check(key(Skills.activate(player, bolt), "commands.covenant.skill.unlearned"), "Commands cannot bypass learning");
            check(key(SkillLoadouts.learn(player, unknown, true), "skills.covenant.unknown"), "Unknown learning rejected");
            check(key(SkillLoadouts.learn(player, shot.id(), true), "commands.covenant.skill.class"), "Wrong-class learning rejected");
            check(key(SkillLoadouts.bind(player, 0, bolt.id()), "commands.covenant.skill.unlearned"), "Unlearned assignment rejected");
            check(player.getData(RpgData.SKILL_LOADOUT).equals(before), "Rejected edits change nothing");
            SkillLoadouts.learn(player, bolt.id(), true);
            for (int slot = 0; slot < 8; slot++) { SkillLoadouts.bind(player, slot, bolt.id()); }
            check(player.getData(RpgData.SKILL_LOADOUT).bindings().size() == 8, "All eight slots assign independently");
            before = player.getData(RpgData.SKILL_LOADOUT);
            for (int slot : new int[]{-1, 8, Integer.MAX_VALUE}) {
                check(key(SkillLoadouts.bind(player, slot, bolt.id()), "skills.covenant.invalid_slot"), "Reject invalid assignment slot");
                check(key(SkillLoadouts.activateSlot(player, slot), "skills.covenant.invalid_slot"), "Reject invalid activation slot");
            }
            check(key(SkillLoadouts.bind(player, 0, unknown), "skills.covenant.unknown"), "Unknown binding rejected");
            check(player.getData(RpgData.SKILL_LOADOUT).equals(before), "Malformed requests leave loadout unchanged");
            SkillLoadouts.learn(player, bolt.id(), true);
            check(player.getData(RpgData.SKILL_LOADOUT).equals(before), "Repeated learn is idempotent");
            SkillLoadouts.bind(player, 7, null);
            check(!player.getData(RpgData.SKILL_LOADOUT).bindings().containsKey(7), "Right-click clearing removes just its slot");
            check(key(SkillLoadouts.activateSlot(player, 7), "skills.covenant.empty"), "Empty slot cannot activate");
            SkillLoadouts.bind(player, 7, bolt.id());
            PlayerProfiles.switchClass(player, PlayerClass.STALKER);
            check(player.getData(RpgData.SKILL_LOADOUT).equals(before), "Class switching preserves inactive learned skills/bindings");
            check(key(SkillLoadouts.activateSlot(player, 0), "commands.covenant.skill.class"), "Inactive class binding cannot activate");
            check(key(SkillLoadouts.bind(player, 0, bolt.id()), "commands.covenant.skill.class"), "Cannot assign inactive class skill");
            SkillLoadouts.learn(player, shot.id(), true);
            SkillLoadouts.bind(player, 0, shot.id());
            PlayerProfiles.switchClass(player, PlayerClass.WEAVER);
            var expected = player.getData(RpgData.SKILL_LOADOUT);
            var encoded = SkillLoadout.CODEC.encodeStart(NbtOps.INSTANCE, expected).getOrThrow();
            check(SkillLoadout.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow().equals(expected), "NBT codec retains all learning and integer slots");
            check(SkillLoadout.CODEC.parse(NbtOps.INSTANCE, new net.minecraft.nbt.CompoundTag()).getOrThrow().equals(SkillLoadout.EMPTY),
                    "Missing optional fields default empty");
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                SkillLoadout.STREAM_CODEC.encode(buffer, expected);
                check(SkillLoadout.STREAM_CODEC.decode(buffer).equals(expected), "Owner sync codec");
                var learn = new SkillNetworking.Learn(bolt.id(), false);
                SkillNetworking.Learn.STREAM_CODEC.encode(buffer, learn);
                check(SkillNetworking.Learn.STREAM_CODEC.decode(buffer).equals(learn), "Learn request codec");
                for (var bind : java.util.List.of(new SkillNetworking.Bind(7, Optional.of(bolt.id())),
                        new SkillNetworking.Bind(7, Optional.empty()))) {
                    SkillNetworking.Bind.STREAM_CODEC.encode(buffer, bind);
                    check(SkillNetworking.Bind.STREAM_CODEC.decode(buffer).equals(bind), "Assign/clear request codec");
                }
                var use = new SkillNetworking.Use(7);
                SkillNetworking.Use.STREAM_CODEC.encode(buffer, use);
                check(SkillNetworking.Use.STREAM_CODEC.decode(buffer).equals(use), "Slot activation codec");
            } finally { buffer.release(); }
            server.getPlayerList().saveAll();
            var disk = server.getPlayerList().loadPlayerData(player.nameAndId()).orElseThrow();
            disk.remove("UUID");
            var loaded = helper.makeMockServerPlayerInLevel();
            players.add(loaded);
            var reporter = new ProblemReporter.Collector();
            loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
            check(reporter.isEmpty() && loaded.getData(RpgData.SKILL_LOADOUT).equals(expected), "Actual player-file reload retains loadout");
            for (boolean endReturn : new boolean[]{false, true}) {
                var clone = helper.makeMockServerPlayerInLevel();
                players.add(clone);
                clone.restoreFrom(loaded, endReturn);
                check(clone.getData(RpgData.SKILL_LOADOUT).equals(expected), "Death/End return copy loadout");
            }
            player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(bolt.id(), ClassResources.now(player) + 100));
            SkillLoadouts.learn(player, bolt.id(), false);
            check(player.getData(RpgData.SKILL_LOADOUT).bindings().size() == 1
                    && player.getData(RpgData.SKILL_LOADOUT).bindings().get(0).equals(shot.id()),
                    "Unlearn clears every matching slot, preserving other skills");
            SkillLoadouts.learn(player, bolt.id(), true);
            check(key(Skills.activate(player, bolt), "commands.covenant.skill.cooldown"), "Unlearn/relearn cannot erase cooldown");
            player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(bolt.id(), 0));
            SkillLoadouts.bind(player, 7, bolt.id());
            check(key(SkillLoadouts.activateSlot(player, 7), "commands.covenant.skill.started"), "Bound slot begins existing cast");
            var resources = player.getData(RpgData.SKILLS);
            SkillLoadouts.learn(player, bolt.id(), false);
            check(player.getData(RpgData.SKILLS).equals(resources), "Unlearning does not alter resource or cooldown data");
            check(key(Skills.activate(player, bolt), "commands.covenant.skill.unlearned"), "Unlearned direct activation rejected");
            helper.onEachTick(player::doTick);
            helper.runAfterDelay(12, () -> {
                try {
                    check(helper.getLevel().getEntitiesOfClass(ArcaneBolt.class, player.getBoundingBox().inflate(20),
                            entity -> entity.getOwner() == player).isEmpty(), "Unlearning cancels pending cast without an effect");
                    check(Skills.cooldownRemaining(player, bolt) == 0, "Canceled cast starts no cooldown");
                    helper.succeed();
                } finally { players.forEach(server.getPlayerList()::remove); }
            });
            scheduled = true;
        } finally { if (!scheduled) { players.forEach(server.getPlayerList()::remove); } }
    }

    private static boolean key(Component value, String expected) {
        return value.getContents() instanceof TranslatableContents text && text.getKey().equals(expected);
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException(message); }
    }
}
