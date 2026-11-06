package dev.covenant.test;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.LightningVisual;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillNetworking;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Exercises the registered payload handlers, including rejected traffic before gameplay dispatch. */
final class SkillNetworkingGameTests {
    private SkillNetworkingGameTests() {}

    @SuppressWarnings("removal")
    static void ingress(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var other = helper.makeMockServerPlayerInLevel();
        PlayerProfiles.selectClass(player, PlayerClass.REAVER);
        PlayerProfiles.selectClass(other, PlayerClass.REAVER);
        ClassResources.set(player, PlayerClass.REAVER, 100);
        NetworkRegistry.configureMockConnection(player.connection.getConnection());
        NetworkRegistry.configureMockConnection(other.connection.getConnection());
        var cleave = SkillDefinition.CLEAVE.id();
        var rend = SkillDefinition.REND.id();
        send(player, new SkillNetworking.Learn(cleave, true));
        for (int i = 0; i < 100; i++) { send(player, new SkillNetworking.Learn(cleave, true)); }
        send(player, new SkillNetworking.Bind(0, Optional.of(cleave)));
        check(cleave.equals(player.getData(RpgData.SKILL_LOADOUT).bindings().get(0)),
                "Duplicate packets do not exhaust the budget before a distinct binding");
        send(player, new SkillNetworking.Use(0));
        for (int i = 0; i < 100; i++) { send(player, new SkillNetworking.Use(0)); }
        check(ClassResources.current(player, PlayerClass.REAVER) == 80, "Repeated use spends once");
        send(player, new SkillNetworking.Learn(SkillDefinition.FIRE_BOLT.id(), true));
        check(!player.getData(RpgData.SKILL_LOADOUT).learned().contains(SkillDefinition.FIRE_BOLT.id()),
                "Registered handler still enforces the selected class");
        for (int i = 0; i < 32; i++) { send(player, new SkillNetworking.Use(-100 - i)); }
        send(player, new SkillNetworking.Learn(rend, true));
        check(!player.getData(RpgData.SKILL_LOADOUT).learned().contains(rend), "Invalid requests also consume ingress budget");
        send(other, new SkillNetworking.Learn(cleave, true));
        check(other.getData(RpgData.SKILL_LOADOUT).learned().contains(cleave), "Budgets are isolated per connection player");
        helper.runAtTickTime(3, () -> {
            try {
                send(player, new SkillNetworking.Learn(rend, true));
                check(player.getData(RpgData.SKILL_LOADOUT).learned().contains(rend), "Budget recovers with server ticks");
                check(player.getData(RpgData.SKILL_LOADOUT).bindings().size() == 1, "Invalid slots never mutate assignments");
                helper.succeed();
            } finally {
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.getLevel().getServer().getPlayerList().remove(other);
            }
        });
    }

    static void lightningCodec(GameTestHelper helper) {
        var beam = new LightningVisual(helper.getLevel().dimension().identifier(), new Vec3(1, 20, 3), new Vec3(25, 20, 3));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            LightningVisual.STREAM_CODEC.encode(buffer, beam);
            check(beam.equals(LightningVisual.STREAM_CODEC.decode(buffer)), "Beam dimension and endpoints survive transport");
            check(buffer.readableBytes() == 0, "Beam is one bounded endpoint payload");
        } finally { buffer.release(); }
        boolean rejected = false;
        try { new LightningVisual(beam.dimension(), beam.start(), new Vec3(Double.NaN, 0, 0)); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Non-finite endpoints rejected");
        rejected = false;
        try { new LightningVisual(beam.dimension(), Vec3.ZERO, new Vec3(33, 0, 0)); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Excessive particle spans rejected");
        helper.succeed();
    }

    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        NetworkRegistry.handleModdedPayload(player.connection, new ServerboundCustomPayloadPacket(payload));
    }

    private static void check(boolean value, String message) { if (!value) { throw new IllegalStateException(message); } }
}
