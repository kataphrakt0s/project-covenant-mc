package dev.covenant.rpg.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

/** Reserved item progression balances, default 0. No costs, caps, or upgrade rules exist yet. */
public record PageProgression(int upgrades, long experience) {
    public static final PageProgression NONE = new PageProgression(0, 0);
    public static final Codec<PageProgression> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("upgrades").forGetter(PageProgression::upgrades),
            ExtraCodecs.NON_NEGATIVE_LONG.fieldOf("experience").forGetter(PageProgression::experience)
    ).apply(i, PageProgression::new));

    public PageProgression {
        if (upgrades < 0 || experience < 0) {
            throw new IllegalArgumentException("Page progression cannot be negative");
        }
    }
}
