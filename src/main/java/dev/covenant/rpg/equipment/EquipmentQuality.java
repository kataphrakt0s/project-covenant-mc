package dev.covenant.rpg.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/** Intrinsic initialized rarity and remaining Potential; survives even removal of every affix. */
public record EquipmentQuality(Identifier rarity, int potential) {
    public static final Codec<EquipmentQuality> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("rarity").forGetter(EquipmentQuality::rarity),
            Codec.intRange(0, 1_000_000).fieldOf("potential").forGetter(EquipmentQuality::potential)
    ).apply(i, EquipmentQuality::new));
    public EquipmentQuality {
        java.util.Objects.requireNonNull(rarity);
        if (potential < 0 || potential > 1_000_000) { throw new IllegalArgumentException("Invalid Potential"); }
    }
}
