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
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Exercises the registered payload handlers, including rejected traffic before gameplay dispatch. */
final class SkillNetworkingGameTests {
    private SkillNetworkingGameTests() {}

    /** Inspect real registered channels, without changing the registry or production visibility. */
    private static final class RegistryView extends NetworkRegistry {
        static java.util.Map<net.minecraft.network.ConnectionProtocol,
                java.util.Map<net.minecraft.resources.Identifier,
                net.neoforged.neoforge.network.registration.PayloadRegistration<?>>> registrations() {
            return PAYLOAD_REGISTRATIONS;
        }
    }

    static void connectionVersions(GameTestHelper helper) {
        var registrations = RegistryView.registrations();
        var local = registrations.get(net.minecraft.network.ConnectionProtocol.PLAY).values().stream()
                .filter(r -> r.id().getNamespace().equals("covenant"))
                .map(net.neoforged.neoforge.network.negotiation.NegotiableNetworkComponent::new).toList();
        var modVersion = net.neoforged.fml.ModList.get().getModContainerById("covenant").orElseThrow().getModInfo().getVersion().toString();
        check(!local.isEmpty() && local.stream().allMatch(r -> !r.optional() && r.version().startsWith(modVersion + " (")),
                "Every Covenant channel requires the installed release version");
        check(net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator.negotiate(local, local).success(),
                "Identical releases and wire formats negotiate successfully");
        var remote = local.stream().map(r -> new net.neoforged.neoforge.network.negotiation.NegotiableNetworkComponent(
                r.id(), "old-release" + r.version().substring(r.version().indexOf(" (")), r.flow(), r.optional())).toList();
        var mismatch = net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator.negotiate(local, remote);
        check(!mismatch.success() && mismatch.failureReasons().size() == local.size(), "Different mod releases reject all required channels");
        for (var reason : mismatch.failureReasons().values()) {
            check(reason.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents wrapper
                    && wrapper.getKey().equals("neoforge.network.negotiation.failure.mod"), "Native disconnect identifies Project Covenant");
            var detail = (Component) ((net.minecraft.network.chat.contents.TranslatableContents) reason.getContents()).getArgs()[1];
            check(detail.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && t.getKey().equals("neoforge.network.negotiation.failure.version.mismatch")
                    && java.util.Arrays.stream(t.getArgs()).anyMatch(a -> a.toString().startsWith(modVersion))
                    && java.util.Arrays.stream(t.getArgs()).anyMatch(a -> a.toString().startsWith("old-release")),
                    "Native mismatch reason carries both client and server versions for the disconnect screen");
        }
        check(!net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator.negotiate(local, java.util.List.of()).success(),
                "A missing Covenant installation is rejected");

        // Exercise the server's actual rejection path before any play listener/player is created.
        var queries = new java.util.HashMap<net.minecraft.network.ConnectionProtocol,
                java.util.Set<net.neoforged.neoforge.network.payload.ModdedNetworkQueryComponent>>();
        registrations.forEach((protocol, entries) -> queries.put(protocol, entries.values().stream().map(r ->
                new net.neoforged.neoforge.network.payload.ModdedNetworkQueryComponent(r.id(),
                        r.id().getNamespace().equals("covenant") ? "old-release" : r.version(), r.flow(), r.optional()))
                .collect(java.util.stream.Collectors.toSet())));
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        var sent = new java.util.ArrayList<Object>(); var disconnected = new java.util.ArrayList<Object>();
        var listener = (net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener) java.lang.reflect.Proxy.newProxyInstance(
                SkillNetworkingGameTests.class.getClassLoader(),
                new Class<?>[] {net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getConnection" -> connection;
                    case "getConnectionType" -> net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE;
                    case "send" -> { sent.add(args[0]); yield null; }
                    case "disconnect" -> { disconnected.add(args[0]); yield null; }
                    default -> throw new AssertionError("Unexpected configuration call: " + method);
                });
        try {
            NetworkRegistry.initializeNeoForgeConnection(listener, queries);
            check(disconnected.size() == 1, "Server cleanly disconnects the mismatched configuration connection");
            check(sent.size() == 1 && sent.getFirst() instanceof net.neoforged.neoforge.network.payload.ModdedNetworkSetupFailedPayload failure
                    && !failure.failureReasons().isEmpty(), "Server sends version failure details before disconnect, without entering play");
        } finally { channel.finishAndReleaseAll(); }
        helper.succeed();
    }

    @SuppressWarnings("removal")
    static void ingress(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var other = helper.makeMockServerPlayerInLevel();
        PlayerProfiles.selectClass(player, PlayerClass.REAVER);
        PlayerProfiles.selectClass(other, PlayerClass.REAVER);
        PlayerProfiles.update(player, p -> dev.covenant.rpg.progression.Progression.atLevel(p, 3));
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
