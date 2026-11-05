package dev.covenant.rpg.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

/** Saved balances by class and cooldown deadlines on the overworld game clock. No effective maxima. */
public record SkillState(Map<Identifier, Double> balances, Map<Identifier, Long> cooldowns) {
    public static final SkillState EMPTY = new SkillState(Map.of(), Map.of());
    private static final Codec<Double> BALANCE = Codec.DOUBLE.validate(v ->
            Double.isFinite(v) && v >= 0 ? com.mojang.serialization.DataResult.success(v)
                    : com.mojang.serialization.DataResult.error(() -> "Invalid resource balance"));
    public static final Codec<SkillState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Identifier.CODEC, BALANCE).fieldOf("balances").forGetter(SkillState::balances),
            Codec.unboundedMap(Identifier.CODEC, ExtraCodecs.NON_NEGATIVE_LONG).fieldOf("cooldowns").forGetter(SkillState::cooldowns)
    ).apply(i, SkillState::new));
    public static final StreamCodec<ByteBuf, SkillState> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public SkillState {
        balances = Map.copyOf(balances);
        cooldowns = Map.copyOf(cooldowns);
        if (balances.values().stream().anyMatch(v -> !Double.isFinite(v) || v < 0)
                || cooldowns.values().stream().anyMatch(v -> v < 0)) {
            throw new IllegalArgumentException("Invalid skill state");
        }
    }

    public SkillState withBalance(Identifier classId, double value) {
        var next = new HashMap<>(balances);
        next.put(classId, value);
        return new SkillState(next, cooldowns);
    }

    public SkillState withCooldown(Identifier skillId, long deadline) {
        var next = new HashMap<>(cooldowns);
        next.put(skillId, deadline);
        return new SkillState(balances, next);
    }
}
