package dev.covenant.rpg.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Intrinsic per-stack RPG data. The UUID identifies this page, never an owner, slot, or item type.
 * All nested values are immutable so Minecraft's component-sharing stack copies are safe.
 * Base modifiers and each affix's modifiers remain independent; none are stored on the player.
 */
public record AffixPage(UUID identity, EquipmentCategory category, List<RpgModifier> modifiers,
        List<Affix> affixes, PageProgression progression) {
    public static final int MAX_AFFIXES = 64;
    public static final Codec<AffixPage> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("identity").forGetter(AffixPage::identity),
            EquipmentCategory.CODEC.fieldOf("category").forGetter(AffixPage::category),
            RpgModifier.CODEC.listOf(0, Affix.MAX_MODIFIERS).fieldOf("modifiers").forGetter(AffixPage::modifiers),
            Affix.CODEC.listOf(0, MAX_AFFIXES).fieldOf("affixes").forGetter(AffixPage::affixes),
            PageProgression.CODEC.optionalFieldOf("progression", PageProgression.NONE).forGetter(AffixPage::progression)
    ).apply(i, AffixPage::new));
    public static final StreamCodec<ByteBuf, AffixPage> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public AffixPage {
        Objects.requireNonNull(identity);
        Objects.requireNonNull(category);
        Objects.requireNonNull(progression);
        modifiers = List.copyOf(modifiers);
        affixes = List.copyOf(affixes);
        if (modifiers.size() > Affix.MAX_MODIFIERS || affixes.size() > MAX_AFFIXES) {
            throw new IllegalArgumentException("Affix page exceeds serialized list limits");
        }
    }
}
