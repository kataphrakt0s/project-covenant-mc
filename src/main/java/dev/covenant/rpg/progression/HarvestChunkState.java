package dev.covenant.rpg.progression;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import net.minecraft.world.level.block.state.BlockState;

/** Chunk-owned ore provenance and separate, disposable tree evidence; neither is sent to clients. */
public final class HarvestChunkState {
    private HarvestChunkState() {}

    public static final class PlacedOres {
        public static final Codec<PlacedOres> CODEC = Codec.LONG.listOf().xmap(values -> {
            var result = new PlacedOres(); values.forEach(value -> result.positions.add(value.longValue())); return result;
        }, state -> {
            var result = new ArrayList<Long>(state.positions.size()); state.positions.forEach((long value) -> result.add(value)); return result;
        });
        final LongOpenHashSet positions = new LongOpenHashSet();
    }

    /** Evidence survives sequential chopping, but not chunk unload/reload or catalogue changes. */
    public static final class Trees {
        final Long2ObjectOpenHashMap<BlockState> logs = new Long2ObjectOpenHashMap<>();
        long revision;
    }
}
