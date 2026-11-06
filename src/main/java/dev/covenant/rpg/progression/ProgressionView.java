package dev.covenant.rpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/** Owner-only presentation snapshot; no player data or balance decisions originate on the client. */
public record ProgressionView(long requiredXp, int dimensionCap, int globalCap, boolean inCombat) {
    public static final ProgressionView EMPTY = new ProgressionView(0, 60, 60, false);
    public static final Codec<ProgressionView> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.NON_NEGATIVE_LONG.fieldOf("required").forGetter(ProgressionView::requiredXp),
            Codec.intRange(0, 1000).fieldOf("dimension_cap").forGetter(ProgressionView::dimensionCap),
            Codec.intRange(1, 1000).fieldOf("global_cap").forGetter(ProgressionView::globalCap),
            Codec.BOOL.fieldOf("combat").forGetter(ProgressionView::inCombat)
    ).apply(i, ProgressionView::new));
    public static final StreamCodec<ByteBuf, ProgressionView> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
