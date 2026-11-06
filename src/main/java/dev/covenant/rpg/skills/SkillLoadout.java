package dev.covenant.rpg.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Immutable owner-synced progression and eight slot assignments, independent of keyboard layout. */
public record SkillLoadout(Map<Identifier, Integer> ranks, Map<Integer, Identifier> bindings) {
    public static final int SLOT_COUNT = 8;
    public static final SkillLoadout EMPTY = new SkillLoadout(Map.of(), Map.of());
    // NBT/JSON compound keys are strings, even though logical slot indices are integers.
    private static final Codec<Integer> SLOT_KEY = Codec.STRING.comapFlatMap(key -> {
        try {
            int slot = Integer.parseInt(key);
            if (validSlot(slot)) { return com.mojang.serialization.DataResult.success(slot); }
        } catch (NumberFormatException ignored) { }
        return com.mojang.serialization.DataResult.error(() -> "Invalid skill slot: " + key);
    }, Object::toString);
    public static final Codec<SkillLoadout> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Identifier.CODEC, Codec.intRange(1, 12))
                    .fieldOf("ranks").forGetter(SkillLoadout::ranks),
            Codec.unboundedMap(SLOT_KEY, Identifier.CODEC)
                    .optionalFieldOf("bindings", Map.of()).forGetter(SkillLoadout::bindings)
    ).apply(i, SkillLoadout::new));
    public static final StreamCodec<ByteBuf, SkillLoadout> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public SkillLoadout {
        ranks = Map.copyOf(ranks);
        bindings = Map.copyOf(bindings);
        if (bindings.keySet().stream().anyMatch(slot -> !validSlot(slot))
                || ranks.size() > 62 || ranks.values().stream().anyMatch(rank -> rank < 1 || rank > 12)
                || !ranks.keySet().containsAll(bindings.values())) {
            throw new IllegalArgumentException("Bindings require a valid slot and learned skill");
        }
    }

    /** Trusted fixture/helper constructor; normal purchases pass through SkillLoadouts.apply. */
    public SkillLoadout(Set<Identifier> learned, Map<Integer, Identifier> bindings) {
        this(learned.stream().collect(java.util.stream.Collectors.toMap(id -> id, id -> 1)), bindings);
    }
    public Set<Identifier> learned() { return ranks.keySet(); }
    public int rank(Identifier id) { return ranks.getOrDefault(id, 0); }
    public int spent() { return ranks.values().stream().mapToInt(Integer::intValue).sum(); }

    public static boolean validSlot(int slot) { return slot >= 0 && slot < SLOT_COUNT; }

    public SkillLoadout withLearned(Identifier skill, boolean learn) {
        var nextLearned = new HashMap<>(ranks);
        var nextBindings = new HashMap<>(bindings);
        if (learn) { nextLearned.putIfAbsent(skill, 1); }
        else {
            nextLearned.remove(skill);
            nextBindings.values().removeIf(skill::equals);
        }
        return new SkillLoadout(nextLearned, nextBindings);
    }

    /** Null clears a slot. The same learned skill may occupy several slots for this prototype. */
    public SkillLoadout withBinding(int slot, Identifier skill) {
        if (!validSlot(slot)) { throw new IllegalArgumentException("Invalid skill slot"); }
        var next = new HashMap<>(bindings);
        if (skill == null) { next.remove(slot); }
        else { next.put(slot, skill); }
        return new SkillLoadout(ranks, next);
    }
}
