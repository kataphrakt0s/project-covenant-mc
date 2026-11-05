package dev.covenant.rpg.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Target-owned wounds. Only active entity ticks advance these saved relative timers. */
public final class BleedState {
    /** Shared cap across all attackers, not a per-player cap. */
    public static final int MAX_STACKS = 3;
    /** One pulse per second at 20 TPS, independent of applications and normal hurt immunity. */
    public static final int PULSE_TICKS = 20;

    public static final class Stack {
        private static final Codec<Float> DAMAGE = Codec.FLOAT.validate(value ->
                Float.isFinite(value) && value > 0 ? DataResult.success(value)
                        : DataResult.error(() -> "Bleed damage must be finite and positive"));
        private static final Codec<Stack> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("attacker").forGetter(Stack::attacker),
                DAMAGE.fieldOf("damage").forGetter(Stack::damage),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("remaining_ticks").forGetter(Stack::remainingTicks)
        ).apply(i, Stack::new));
        private final UUID attacker;
        private final float damage;
        private int remainingTicks;

        private Stack(UUID attacker, float damage, int remainingTicks) {
            this.attacker = attacker;
            this.damage = damage;
            this.remainingTicks = remainingTicks;
        }

        public UUID attacker() { return attacker; }
        public float damage() { return damage; }
        public int remainingTicks() { return remainingTicks; }
    }

    private static final Codec<List<Stack>> STACKS = Stack.CODEC.listOf().validate(stacks ->
            stacks.size() <= MAX_STACKS ? DataResult.success(stacks)
                    : DataResult.error(() -> "Too many bleed stacks"));
    public static final Codec<BleedState> CODEC = RecordCodecBuilder.create(i -> i.group(
            STACKS.fieldOf("stacks").forGetter(BleedState::stacks),
            Codec.intRange(1, PULSE_TICKS).fieldOf("until_pulse").forGetter(BleedState::untilPulse)
    ).apply(i, BleedState::new));

    private final ArrayList<Stack> stacks;
    private final List<Stack> view;
    private int untilPulse;

    public BleedState() { this(List.of(), PULSE_TICKS); }

    private BleedState(List<Stack> stacks, int untilPulse) {
        this.stacks = new ArrayList<>(MAX_STACKS);
        for (var stack : stacks) {
            this.stacks.add(new Stack(stack.attacker, stack.damage, stack.remainingTicks));
        }
        view = Collections.unmodifiableList(this.stacks);
        this.untilPulse = untilPulse;
    }

    /** Oldest first; callers cannot change the list or mutate individual stacks. */
    public List<Stack> stacks() { return view; }
    public int untilPulse() { return untilPulse; }
    public int count() { return stacks.size(); }

    void apply(UUID attacker, float damage, int ticks) {
        if (attacker == null || !Float.isFinite(damage) || damage <= 0 || ticks < 1) {
            throw new IllegalArgumentException("Invalid bleed application");
        }
        for (var stack : stacks) { stack.remainingTicks = ticks; }
        if (stacks.size() == MAX_STACKS) { stacks.removeFirst(); }
        stacks.add(new Stack(attacker, damage, ticks));
    }

    boolean advancePulse() {
        if (--untilPulse > 0) { return false; }
        untilPulse = PULSE_TICKS;
        return true;
    }

    /** Run after the pulse so a five-second stack receives its fifth tick at expiry. */
    void age() {
        for (int i = stacks.size() - 1; i >= 0; i--) {
            if (--stacks.get(i).remainingTicks <= 0) { stacks.remove(i); }
        }
    }
}
