package dev.covenant.rpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import dev.covenant.rpg.RpgData;
import io.netty.buffer.ByteBuf;

/** Small owner-only checklist; raw discovery history stays on the logical server. */
public record ExplorationView(long revision, List<Entry> entries) {
    public record Entry(Identifier id, String title, String pool, int progress, int required, boolean claimed) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("id").forGetter(Entry::id),
                Codec.string(1, 120).fieldOf("title").forGetter(Entry::title),
                Codec.string(1, 48).fieldOf("pool").forGetter(Entry::pool),
                Codec.intRange(0, 8192).fieldOf("progress").forGetter(Entry::progress),
                Codec.intRange(1, 8192).fieldOf("required").forGetter(Entry::required),
                Codec.BOOL.fieldOf("claimed").forGetter(Entry::claimed)
        ).apply(i, Entry::new));
    }
    public static final ExplorationView EMPTY = new ExplorationView(0, List.of());
    public static final Codec<ExplorationView> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("revision").forGetter(ExplorationView::revision),
            Entry.CODEC.listOf().fieldOf("entries").forGetter(ExplorationView::entries)
    ).apply(i, ExplorationView::new));
    public static final StreamCodec<ByteBuf, ExplorationView> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    public ExplorationView {
        entries = List.copyOf(entries);
        if (revision < 0 || entries.size() > 128) { throw new IllegalArgumentException("Invalid exploration view"); }
    }
    public static void refresh(ServerPlayer player, ExplorationData.Snapshot snapshot) {
        var history = player.getData(RpgData.EXPLORATION);
        var entries = snapshot.goals().stream().map(goal -> new Entry(goal.id(), goal.title(), goal.pool(),
                goal.progress(history), goal.required(), history.claimed().contains(goal.id()))).toList();
        var next = new ExplorationView(snapshot.revision(), entries);
        if (!next.equals(player.getData(RpgData.EXPLORATION_VIEW))) { player.setData(RpgData.EXPLORATION_VIEW, next); }
    }
}
