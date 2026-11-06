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

/** Independent saved profession tracks. Ability content and automatic activity sources are a later integration. */
public record ProfessionState(Map<Identifier, Track> tracks) {
    public record Track(int level, long experience) {
        public static final Codec<Track> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 1000).fieldOf("level").forGetter(Track::level),
                ExtraCodecs.NON_NEGATIVE_LONG.fieldOf("experience").forGetter(Track::experience)
        ).apply(i, Track::new));
        public Track { if (level < 0 || level > 1000 || experience < 0) { throw new IllegalArgumentException("Invalid profession progress"); } }
        public int points() { return level; }
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
