package dev.covenant.rpg.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

/**
 * One independent modifier of a namespaced RPG stat. Amounts are finite; multiplicative amounts
 * are fractions (0.10 means 10%). Stat IDs are Minecraft attribute registry IDs.
 * Evaluation uses vanilla order: (base + sum(ADD)) * (1 + sum(MULTIPLY_BASE)),
 * then each (1 + MULTIPLY_TOTAL), finally the attribute's range clamp.
 */
public record RpgModifier(Identifier stat, Operation operation, double amount) {
    public static final Codec<RpgModifier> CODEC = ExtraCodecs.catchDecoderException(RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("stat").forGetter(RpgModifier::stat),
            Operation.CODEC.fieldOf("operation").forGetter(RpgModifier::operation),
            Codec.DOUBLE.fieldOf("amount").forGetter(RpgModifier::amount)
    ).apply(i, RpgModifier::new)));

    public RpgModifier {
        Objects.requireNonNull(stat);
        Objects.requireNonNull(operation);
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("Modifier amount must be finite");
        }
    }

    public enum Operation implements StringRepresentable {
        ADD("add"), MULTIPLY_BASE("multiply_base"), MULTIPLY_TOTAL("multiply_total");

        public static final Codec<Operation> CODEC = StringRepresentable.fromEnum(Operation::values);
        private final String name;

        Operation(String name) { this.name = name; }

        @Override
        public String getSerializedName() { return name; }
    }
}
