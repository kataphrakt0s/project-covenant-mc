package dev.covenant.rpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.resources.Identifier;

/** Server-only discovery history. Claims survive definition removal and never pay twice under the same ID. */
public record ExplorationState(Map<Identifier, Discoveries> dimensions, Set<Identifier> claimed) {
    private static final Codec<Set<Identifier>> IDS = Identifier.CODEC.listOf().xmap(Set::copyOf, List::copyOf);
    public record Discoveries(Set<Identifier> biomes, Set<Identifier> structures) {
        public static final Discoveries EMPTY = new Discoveries(Set.of(), Set.of());
        public static final Codec<Discoveries> CODEC = RecordCodecBuilder.create(i -> i.group(
                IDS.fieldOf("biomes").forGetter(Discoveries::biomes),
                IDS.fieldOf("structures").forGetter(Discoveries::structures)
        ).apply(i, Discoveries::new));
        public Discoveries {
            biomes = Set.copyOf(biomes); structures = Set.copyOf(structures);
            if (biomes.size() > 8192 || structures.size() > 8192) { throw new IllegalArgumentException("Too many discoveries"); }
        }
    }
    public static final ExplorationState EMPTY = new ExplorationState(Map.of(), Set.of());
    public static final Codec<ExplorationState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Identifier.CODEC, Discoveries.CODEC).fieldOf("dimensions").forGetter(ExplorationState::dimensions),
            IDS.fieldOf("claimed").forGetter(ExplorationState::claimed)
    ).apply(i, ExplorationState::new));
    public ExplorationState {
        dimensions = Map.copyOf(dimensions); claimed = Set.copyOf(claimed);
        if (dimensions.size() > 256 || claimed.size() > 8192) { throw new IllegalArgumentException("Too much exploration history"); }
    }
    public Discoveries in(Identifier dimension) { return dimensions.getOrDefault(dimension, Discoveries.EMPTY); }

    /** Returns the same immutable snapshot on a repeat visit; records all observed types for future datapacks. */
    public ExplorationState discover(Identifier dimension, Identifier biome, Set<Identifier> structures) {
        var old = in(dimension);
        if (dimensions.containsKey(dimension) && old.biomes.contains(biome) && old.structures.containsAll(structures)) { return this; }
        var nextBiomes = new HashSet<>(old.biomes); nextBiomes.add(biome);
        var nextStructures = new HashSet<>(old.structures); nextStructures.addAll(structures);
        var nextDimensions = new HashMap<>(dimensions);
        nextDimensions.put(dimension, new Discoveries(nextBiomes, nextStructures));
        return new ExplorationState(nextDimensions, claimed);
    }
}
