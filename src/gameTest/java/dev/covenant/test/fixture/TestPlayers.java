package dev.covenant.test.fixture;

import com.mojang.authlib.GameProfile;
import dev.covenant.rpg.skills.SkillLifecycle;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Real Survival players with negotiated in-memory connections; class/resources/position remain explicit. */
public final class TestPlayers {
    private TestPlayers() {}

    public static ServerPlayer create(GameTestHelper helper) {
        return create(helper, packet -> {});
    }

    /** The observer sees outbound packets, including setup; clear captured packets before the action under test. */
    public static ServerPlayer create(GameTestHelper helper, Consumer<Packet<?>> observer) {
        var id = UUID.randomUUID();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(id, "test" + id.toString().substring(0, 8)), false);
        var level = helper.getLevel();
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override public boolean isMemoryConnection() { return true; }
            @Override public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {
                observer.accept(packet);
                super.send(packet, listener, flush);
            }
        };
        var channel = new EmbeddedChannel(connection);
        Runnable cleanup = () -> {
            try {
                var players = level.getServer().getPlayerList();
                if (players.getPlayer(player.getUUID()) == player) {
                    SkillLifecycle.cancelPlayer(player, SkillLifecycle.Reason.LOGOUT);
                    players.remove(player);
                }
            } finally {
                channel.finishAndReleaseAll();
            }
        };
        TestCleanup.onFinish(helper, cleanup);
        try {
            NetworkRegistry.configureMockConnection(connection);
            level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
            player.setGameMode(GameType.SURVIVAL);
            player.connection.markClientLoaded();
            return player;
        } catch (RuntimeException | Error error) {
            cleanup.run();
            throw error;
        }
    }
}
