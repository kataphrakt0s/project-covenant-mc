package dev.covenant.rpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

/**
 * Permanent character state, independent of equipment. Experience is the current RPG experience
 * balance, not vanilla XP. Resource values are finite units in [0, maximumResource]. No level curve
 * or class definitions are imposed by this storage layer. Replace snapshots through PlayerProfiles.
 */
public record CharacterProfile(boolean created, int level, long experience,
        Optional<Identifier> selectedClass, Optional<Identifier> specialization,
        Optional<Identifier> resourceType, double currentResource, double maximumResource) {
    public static final CharacterProfile UNINITIALIZED = new CharacterProfile(false, 0, 0,
            Optional.empty(), Optional.empty(), Optional.empty(), 0, 0);

    public static final Codec<CharacterProfile> CODEC = ExtraCodecs.catchDecoderException(RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("created").forGetter(CharacterProfile::created),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("level").forGetter(CharacterProfile::level),
            ExtraCodecs.NON_NEGATIVE_LONG.fieldOf("experience").forGetter(CharacterProfile::experience),
            Identifier.CODEC.optionalFieldOf("class").forGetter(CharacterProfile::selectedClass),
            Identifier.CODEC.optionalFieldOf("specialization").forGetter(CharacterProfile::specialization),
            Identifier.CODEC.optionalFieldOf("resource_type").forGetter(CharacterProfile::resourceType),
            Codec.DOUBLE.fieldOf("current_resource").forGetter(CharacterProfile::currentResource),
            Codec.DOUBLE.fieldOf("maximum_resource").forGetter(CharacterProfile::maximumResource)
    ).apply(i, CharacterProfile::new)));
    public static final MapCodec<CharacterProfile> MAP_CODEC = CODEC.fieldOf("profile");
    public static final StreamCodec<ByteBuf, CharacterProfile> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public CharacterProfile {
        Objects.requireNonNull(selectedClass);
        Objects.requireNonNull(specialization);
        Objects.requireNonNull(resourceType);
        if (level < 0 || experience < 0 || !Double.isFinite(currentResource)
                || !Double.isFinite(maximumResource) || currentResource < 0
                || maximumResource < currentResource) {
            throw new IllegalArgumentException("Invalid character progression or resource balance");
        }
        if (created ? level < 1 || selectedClass.isEmpty()
                : level != 0 || experience != 0 || selectedClass.isPresent() || specialization.isPresent()
                    || resourceType.isPresent() || currentResource != 0 || maximumResource != 0) {
            throw new IllegalArgumentException("Inconsistent character creation state");
        }
        if (resourceType.isEmpty() && (currentResource != 0 || maximumResource != 0)) {
            throw new IllegalArgumentException("Resource values require a resource type");
        }
    }

    /** Initial level is 1 and XP is 0; a resource-less class can use an empty resource type and 0 units. */
    public static CharacterProfile create(Identifier selectedClass, Optional<Identifier> resourceType,
            double maximumResource) {
        return new CharacterProfile(true, 1, 0, Optional.of(selectedClass), Optional.empty(),
                resourceType, maximumResource, maximumResource);
    }
}
