package dev.covenant.rpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

/** Independent saved tracks and paid allocations; effective equipment/combat stats are never stored here. */
public record ProfessionState(Map<Identifier, Track> tracks) {
    /** Paid points are historical costs, retained when catalogue tuning changes. */
    public record Allocation(int rank, int paidPoints) {
        public static final Codec<Allocation> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 64).fieldOf("rank").forGetter(Allocation::rank),
                Codec.intRange(1, 1000).fieldOf("paid_points").forGetter(Allocation::paidPoints)
        ).apply(i, Allocation::new));
        public Allocation {
            if (rank < 1 || rank > 64 || paidPoints < rank || paidPoints > 1000) {
                throw new IllegalArgumentException("Invalid paid profession allocation");
            }
        }
    }
    public record Track(int level, long experience, Map<Identifier, Allocation> allocations) {
        public static final Codec<Track> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 1000).fieldOf("level").forGetter(Track::level),
                ExtraCodecs.NON_NEGATIVE_LONG.fieldOf("experience").forGetter(Track::experience),
                Codec.unboundedMap(Identifier.CODEC, Allocation.CODEC)
                        .optionalFieldOf("allocations", Map.of()).forGetter(Track::allocations)
        ).apply(i, Track::new));
        public Track {
            allocations = Map.copyOf(allocations);
            if (level < 0 || level > 1000 || experience < 0 || allocations.size() > 128
                    || allocations.values().stream().mapToLong(Allocation::paidPoints).sum() > level) {
                throw new IllegalArgumentException("Invalid profession progress/point reservation");
            }
        }
        public Track(int level, long experience) { this(level, experience, Map.of()); }
        public int spentPoints() { return allocations.values().stream().mapToInt(Allocation::paidPoints).sum(); }
        public int points() { return level - spentPoints(); }
        public int rank(Identifier id) { var allocation = allocations.get(id); return allocation == null ? 0 : allocation.rank(); }
        public Track withProgression(int nextLevel, long xp) { return new Track(nextLevel, xp, allocations); }
        public Map<Identifier, Integer> ranks() {
            var result = new HashMap<Identifier, Integer>();
            allocations.forEach((id, allocation) -> result.put(id, allocation.rank()));
            return Map.copyOf(result);
        }
    }
    public static final ProfessionState INITIAL = initial();
    public static final Codec<ProfessionState> CODEC = Codec.unboundedMap(Identifier.CODEC, Track.CODEC)
            .xmap(ProfessionState::new, ProfessionState::tracks);
    public static final StreamCodec<ByteBuf, ProfessionState> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    public ProfessionState {
        tracks = Map.copyOf(tracks);
        if (!tracks.keySet().containsAll(ProgressionData.PROFESSIONS) || tracks.size() != 4) {
            throw new IllegalArgumentException("Expected four profession tracks");
        }
    }
    private static ProfessionState initial() {
        var tracks = new HashMap<Identifier, Track>();
        ProgressionData.PROFESSIONS.forEach(id -> tracks.put(id, new Track(0, 0)));
        return new ProfessionState(tracks);
    }
    public ProfessionState with(Identifier id, Track track) {
        if (!tracks.containsKey(id)) { throw new IllegalArgumentException("Unknown profession: " + id); }
        var next = new HashMap<>(tracks); next.put(id, track);
        return new ProfessionState(next);
    }
}
