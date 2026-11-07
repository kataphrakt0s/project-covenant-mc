package dev.covenant.rpg.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/** Stackable material identity: both family and rarity must match, independent of the source item. */
public record AffixMaterial(Identifier family, Identifier rarity) {
    public static final Codec<AffixMaterial> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("family").forGetter(AffixMaterial::family),
            Identifier.CODEC.fieldOf("rarity").forGetter(AffixMaterial::rarity)
    ).apply(i, AffixMaterial::new));
    public AffixMaterial { java.util.Objects.requireNonNull(family); java.util.Objects.requireNonNull(rarity); }
}
