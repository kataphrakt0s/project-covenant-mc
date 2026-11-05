package dev.covenant.rpg.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Immutable owner-synced progression and eight slot assignments, independent of keyboard layout. */
public record SkillLoadout(Set<Identifier> learned, Map<Integer, Identifier> bindings) {
    public static final int SLOT_COUNT = 8;
    public static final SkillLoadout EMPTY = new SkillLoadout(Set.of(), Map.of());
    // NBT/JSON compound keys are strings, even though logical slot indices are integers.
    private static final Codec<Integer> SLOT_KEY = Codec.STRING.comapFlatMap(key -> {
        try {
            int slot = Integer.parseInt(key);
            if (validSlot(slot)) { return com.mojang.serialization.DataResult.success(slot); }
        } catch (NumberFormatException ignored) { }
        return com.mojang.serialization.DataResult.error(() -> "Invalid skill slot: " + key);
    }, Object::toString);
    public static final Codec<SkillLoadout> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.listOf().xmap(Set::copyOf, java.util.List::copyOf)
                    .optionalFieldOf("learned", Set.of()).forGetter(SkillLoadout::learned),
            Codec.unboundedMap(SLOT_KEY, Identifier.CODEC)
                    .optionalFieldOf("bindings", Map.of()).forGetter(SkillLoadout::bindings)
    ).apply(i, SkillLoadout::new));
    public static final StreamCodec<ByteBuf, SkillLoadout> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public SkillLoadout {
        learned = Set.copyOf(learned);
        bindings = Map.copyOf(bindings);
        if (bindings.keySet().stream().anyMatch(slot -> !validSlot(slot))
                || !learned.containsAll(bindings.values())) {
            throw new IllegalArgumentException("Bindings require a valid slot and learned skill");
        }
    }

    public static boolean validSlot(int slot) { return slot >= 0 && slot < SLOT_COUNT; }

    public SkillLoadout withLearned(Identifier skill, boolean learn) {
        var nextLearned = new HashSet<>(learned);
        var nextBindings = new HashMap<>(bindings);
        if (learn) { nextLearned.add(skill); }
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
        return new SkillLoadout(learned, next);
    }
}
