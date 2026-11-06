package dev.covenant.test;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

/** Owner codecs, actual player files, respawn copies, assignments and reset cancellation. */
final class SkillLoadoutGameTests {
    static void run(GameTestHelper helper) {
        try (var f = new ProgressionGameTests.Fixture(helper)) {
            var player = f.player(PlayerClass.WEAVER); var bolt = SkillDefinition.ARCANE_BOLT;
            SkillLoadouts.bind(player, 0, bolt.id()); check(player.getData(RpgData.SKILL_LOADOUT).bindings().isEmpty(), "Unpurchased binding rejected");
            SkillLoadouts.apply(player, Map.of(), Map.of(bolt.id(), 1));
            for (int slot = 0; slot < 8; slot++) { SkillLoadouts.bind(player, slot, bolt.id()); }
            var expected = player.getData(RpgData.SKILL_LOADOUT);
            check(expected.bindings().size() == 8, "Eight independent assignments");
            for (int slot : new int[]{-1, 8, Integer.MAX_VALUE}) { SkillLoadouts.bind(player, slot, bolt.id()); SkillLoadouts.activateSlot(player, slot); }
            check(player.getData(RpgData.SKILL_LOADOUT).equals(expected), "Malformed slots unchanged");
            check(SkillLoadout.CODEC.parse(NbtOps.INSTANCE, SkillLoadout.CODEC.encodeStart(NbtOps.INSTANCE, expected).getOrThrow()).getOrThrow().equals(expected), "Rank and binding NBT");
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                SkillLoadout.STREAM_CODEC.encode(buffer, expected); check(SkillLoadout.STREAM_CODEC.decode(buffer).equals(expected), "Owner sync");
                var apply = new SkillNetworking.Apply(Map.of(), expected.ranks());
                SkillNetworking.Apply.STREAM_CODEC.encode(buffer, apply); check(SkillNetworking.Apply.STREAM_CODEC.decode(buffer).equals(apply), "Apply snapshot codec");
                buffer.writeVarInt(63); boolean rejected = false;
                try { SkillNetworking.Apply.STREAM_CODEC.decode(buffer); } catch (io.netty.handler.codec.DecoderException exception) { rejected = true; }
                check(rejected, "Oversized request rejected before allocating entries");
            } finally { buffer.release(); }
            var server = helper.getLevel().getServer(); server.getPlayerList().saveAll();
            var disk = server.getPlayerList().loadPlayerData(player.nameAndId()).orElseThrow(); disk.remove("UUID");
            var loaded = f.player(null); var reporter = new ProblemReporter.Collector();
            loaded.load(TagValueInput.create(reporter, helper.getLevel().registryAccess(), disk));
            check(reporter.isEmpty() && loaded.getData(RpgData.SKILL_LOADOUT).equals(expected), "Actual player file ranks and bindings");
            for (boolean endReturn : new boolean[]{false, true}) {
                var clone = f.player(null); clone.restoreFrom(loaded, endReturn);
                check(clone.getData(RpgData.SKILL_LOADOUT).equals(expected), "Death and End copy");
            }
            SkillLoadouts.bind(player, 7, null); check(!player.getData(RpgData.SKILL_LOADOUT).bindings().containsKey(7), "Single binding clear");
            player.setData(RpgData.SKILLS, player.getData(RpgData.SKILLS).withCooldown(bolt.id(), ClassResources.now(player) + 100));
            var resources = player.getData(RpgData.SKILLS);
            check(SkillLoadouts.respec(player), "Reset succeeds");
            check(player.getData(RpgData.SKILL_LOADOUT).equals(SkillLoadout.EMPTY) && SkillLoadouts.availablePoints(player) == 1, "Full refund and binding clear");
            check(player.getData(RpgData.SKILLS).equals(resources), "Preserve resources and cooldowns");
            SkillLoadouts.apply(player, Map.of(), Map.of(bolt.id(), 1));
            check(Skills.cooldownRemaining(player, bolt) == 100, "Rebuy retains cooldown");
            player.setData(RpgData.SKILLS, resources.withCooldown(bolt.id(), 0));
            Skills.activate(player, bolt); check(Skills.isCasting(player), "Cast begins");
            check(SkillLoadouts.respec(player) && !Skills.isCasting(player), "Reset cancels cast");
            helper.succeed();
        }
    }
    private static void check(boolean value, String message) { if (!value) { throw new IllegalStateException(message); } }
}
