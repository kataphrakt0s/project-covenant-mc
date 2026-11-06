package dev.covenant.rpg;

import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.SkillLifecycle;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Server mutation boundary for class/profile data; skill requests use their own validated boundary. */
public final class PlayerProfiles {
    private PlayerProfiles() {}

    /** Client UI may read its synchronized snapshot; reading does not initialize a character. */
    public static CharacterProfile get(Player player) {
        return player.getData(RpgData.PROFILE);
    }

    /**
     * Resolved class for gameplay or owner-side UI; empty for uninitialized or unknown saved IDs.
     * The raw selected ID remains available through get(player).selectedClass().
     */
    public static Optional<PlayerClass> getSelectedClass(Player player) {
        return get(player).selectedClass().flatMap(PlayerClass::find);
    }

    /**
     * One-time selection of a defined class. Creates the existing profile at level 1, XP 0,
     * without a specialization, and initializes the separate built-in resource balances.
     * Legacy profile resource fields stay empty. Rejects already-created characters.
     */
    public static void selectClass(ServerPlayer player, PlayerClass selectedClass) {
        initialize(player, selectedClass.id(), Optional.empty(), 0);
        initializeResources(player);
    }

    /** Trusted/operator testing switch; preserves progression and every class balance, cancels a pending cast. */
    public static void switchClass(ServerPlayer player, PlayerClass selectedClass) {
        requireServerThread(player);
        if (!get(player).created()) { selectClass(player, selectedClass); return; }
        initializeResources(player);
        update(player, p -> new CharacterProfile(true, p.level(), p.experience(), Optional.of(selectedClass.id()),
                p.specialization(), p.resourceType(), p.currentResource(), p.maximumResource()));
    }

    private static void initializeResources(ServerPlayer player) {
        for (var type : PlayerClass.all()) {
            if (!player.getData(RpgData.SKILLS).balances().containsKey(type.id())) {
                ClassResources.set(player, type, ClassResources.current(player, type));
            }
        }
    }

    /** Trusted storage API for server integrations, including IDs not in the built-in class catalog. */
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
            if (!previous.selectedClass().equals(next.selectedClass())) {
                SkillLifecycle.cancelPlayer(player, SkillLifecycle.Reason.CLASS_CHANGE);
                initializeResources(player);
            }
            player.setData(RpgData.PROFILE, next);
        }
    }

    private static void requireServerThread(ServerPlayer player) {
        if (!player.level().getServer().isSameThread()) {
            throw new IllegalStateException("Profile changes require the logical server thread");
        }
    }
}
