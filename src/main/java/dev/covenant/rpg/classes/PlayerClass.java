package dev.covenant.rpg.classes;

import dev.covenant.Covenant;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Built-in class identities. Only the explicit namespaced ID is saved, never an enum ordinal.
 * Names and identity/equipment descriptions are language resources, not equipment rules.
 * Adding a definition also exposes it to selection and command suggestions.
 */
public enum PlayerClass {
    STALKER("stalker"),
    REAVER("reaver"),
    WEAVER("weaver");

    private static final List<PlayerClass> ALL = List.of(values());
    private final Identifier id;
    private final String translationKey;

    PlayerClass(String path) {
        id = Identifier.fromNamespaceAndPath(Covenant.MOD_ID, path);
        translationKey = "class." + Covenant.MOD_ID + "." + path;
    }

    /** Persistent identity; renaming an ID requires an explicit saved-profile migration. */
    public Identifier id() {
        return id;
    }

    public Component displayName() {
        return Component.translatable(translationKey);
    }

    public Component description() {
        return Component.translatable(translationKey + ".description");
    }

    /** Immutable definition order for UI and command discovery. */
    public static List<PlayerClass> all() {
        return ALL;
    }

    /** Unknown saved IDs stay in the profile; they do not silently become another class. */
    public static Optional<PlayerClass> find(Identifier id) {
        for (PlayerClass definition : ALL) {
            if (definition.id.equals(id)) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }
}
