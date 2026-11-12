package dev.covenant.rpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.covenant.rpg.RpgData;
import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Owner-only transient presentation. Catalogue revisions invalidate stale purchase previews. */
public record ProfessionView(long revision, Map<Identifier, ProfessionData.Node> abilities, Map<Identifier, Track> tracks,
        List<CropTarget> crops) {
    /** Only target recognition is predicted. Tool wear, loot, inventory and terrain remain server-owned. */
    public record CropTarget(Identifier block, String property, int mature) {
        public static final Codec<CropTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("block").forGetter(CropTarget::block),
                Codec.STRING.fieldOf("property").forGetter(CropTarget::property),
                Codec.INT.fieldOf("mature").forGetter(CropTarget::mature)
        ).apply(i, CropTarget::new));
        public CropTarget(HarvestDefinitions.Crop crop) { this(crop.block(), crop.age().getName(), crop.mature()); }
        public boolean test(BlockState state) {
            return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(block)
                    && state.getBlock().getStateDefinition().getProperty(property) instanceof IntegerProperty age
                    && state.getValue(age) == mature;
        }
    }
    public record Track(long requiredXp, int dimensionCap, int maximum) {
        public static final Codec<Track> CODEC = RecordCodecBuilder.create(i -> i.group(
                ExtraCodecs.NON_NEGATIVE_LONG.fieldOf("required").forGetter(Track::requiredXp),
                Codec.intRange(0, 1000).fieldOf("dimension_cap").forGetter(Track::dimensionCap),
                Codec.intRange(0, 1000).fieldOf("maximum").forGetter(Track::maximum)
        ).apply(i, Track::new));
    }
    public static final ProfessionView EMPTY = new ProfessionView(0, Map.of(), Map.of(), List.of());
    public static final Codec<ProfessionView> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.NON_NEGATIVE_LONG.fieldOf("revision").forGetter(ProfessionView::revision),
            Codec.unboundedMap(Identifier.CODEC, ProfessionData.Node.CODEC).fieldOf("abilities").forGetter(ProfessionView::abilities),
            Codec.unboundedMap(Identifier.CODEC, Track.CODEC).fieldOf("tracks").forGetter(ProfessionView::tracks),
            CropTarget.CODEC.listOf().optionalFieldOf("crops", List.of()).forGetter(ProfessionView::crops)
    ).apply(i, ProfessionView::new));
    public static final StreamCodec<ByteBuf, ProfessionView> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    public ProfessionView {
        abilities = Map.copyOf(abilities); tracks = Map.copyOf(tracks); crops = List.copyOf(crops);
        if (revision < 0 || abilities.size() > 128 || !ProgressionData.PROFESSIONS.containsAll(tracks.keySet()) || crops.size() > 256) { throw new IllegalArgumentException("Invalid profession view"); }
    }
    public List<ProfessionData.Node> nodes(Identifier profession) {
        return abilities.values().stream().filter(n -> n.profession().equals(profession))
                .sorted(Comparator.comparingInt(ProfessionData.Node::order).thenComparing(n -> n.id().toString())).toList();
    }
    public static void refresh(ServerPlayer player) {
        var snapshot = ProfessionData.snapshot(player.level().getServer());
        if (snapshot == null) { return; }
        var balance = ProgressionData.get(player.level().getServer());
        var previous = player.getData(RpgData.PROFESSION_VIEW);
        var state = player.getData(RpgData.PROFESSIONS);
        boolean changed = previous.revision != snapshot.revision();
        for (var id : ProgressionData.PROFESSIONS) {
            var old = previous.tracks.get(id);
            var track = state.tracks().get(id);
            if (old == null || old.requiredXp != balance.required(id, track.level())
                    || old.dimensionCap != balance.gate(player.level(), id) || old.maximum != balance.maximum(id)) { changed = true; }
        }
        if (!changed) { return; }
        var tracks = new HashMap<Identifier, Track>();
        for (var id : ProgressionData.PROFESSIONS) {
            tracks.put(id, new Track(balance.required(id, state.tracks().get(id).level()), balance.gate(player.level(), id), balance.maximum(id)));
        }
        var crops = snapshot.catalogue().harvesting().crops().stream().filter(crop -> crop.plantingItem() != null).map(CropTarget::new).toList();
        player.setData(RpgData.PROFESSION_VIEW, new ProfessionView(snapshot.revision(), snapshot.catalogue().abilities(), tracks, crops));
    }
}
