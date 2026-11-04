package dev.covenant.rpg.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/** A rolled affix snapshot. Tier starts at 1; rarity/definition IDs are open to future content. */
public record Affix(Identifier id, Identifier rarity, int tier, List<RpgModifier> modifiers) {
    public static final int MAX_MODIFIERS = 64;
    public static final Codec<Affix> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("id").forGetter(Affix::id),
            Identifier.CODEC.fieldOf("rarity").forGetter(Affix::rarity),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("tier").forGetter(Affix::tier),
            RpgModifier.CODEC.listOf(0, MAX_MODIFIERS).fieldOf("modifiers").forGetter(Affix::modifiers)
    ).apply(i, Affix::new));

    public Affix {
        Objects.requireNonNull(id);
        Objects.requireNonNull(rarity);
        modifiers = List.copyOf(modifiers);
        if (tier < 1 || modifiers.size() > MAX_MODIFIERS) {
            throw new IllegalArgumentException("Invalid affix tier or modifier count");
        }
    }
}
