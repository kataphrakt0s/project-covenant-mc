package dev.covenant.rpg;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Server mutation boundary. No client-to-server progression packets are registered. */
public final class PlayerProfiles {
    private PlayerProfiles() {}

    /** Client UI may read its synchronized snapshot; reading does not initialize a character. */
    public static CharacterProfile get(Player player) {
        return player.getData(RpgData.PROFILE);
    }

    /** Called by a future, server-validated character creation flow. Rejects repeat initialization. */
    public static void initialize(ServerPlayer player, Identifier selectedClass,
            Optional<Identifier> resourceType, double maximumResource) {
        requireServerThread(player);
        if (get(player).created()) {
            throw new IllegalStateException("Character is already created");
        }
        player.setData(RpgData.PROFILE, CharacterProfile.create(selectedClass, resourceType, maximumResource));
    }

    /** Trusted server systems replace permanent state; equipment bonuses must never be written here. */
    public static void update(ServerPlayer player, UnaryOperator<CharacterProfile> change) {
        requireServerThread(player);
        CharacterProfile previous = get(player);
        if (!previous.created()) {
            throw new IllegalStateException("Character has not been created");
        }
        CharacterProfile next = Objects.requireNonNull(change.apply(previous));
        if (!next.created()) {
            throw new IllegalArgumentException("Progression updates cannot erase character creation");
        }
        if (!previous.equals(next)) {
            player.setData(RpgData.PROFILE, next);
        }
    }

    private static void requireServerThread(ServerPlayer player) {
        if (!player.level().getServer().isSameThread()) {
            throw new IllegalStateException("Profile changes require the logical server thread");
        }
    }
}
