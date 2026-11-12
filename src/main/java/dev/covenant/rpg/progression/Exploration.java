package dev.covenant.rpg.progression;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.skills.ClassResources;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.profiling.Profiler;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/** Position-change discovery checks against loaded native chunks. No chunk requests or recurring world scans. */
public final class Exploration {
    private Exploration() {}
    /** Disposable observation cursor, never saved, synced or copied to replacement players. */
    public static final class Cursor {
        long position = Long.MIN_VALUE, revision = -1;
        Identifier dimension;
    }
    public static void reload(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(player -> {
            var snapshot = ExplorationData.snapshot(player.level().getServer());
            if (snapshot != null) { evaluate(player, snapshot); }
        });
    }
    public static void tick(ServerPlayer player) {
        if (!ProfessionHarvesting.participant(player)) { return; }
        var snapshot = ExplorationData.snapshot(player.level().getServer());
        if (snapshot == null) { return; }
        var level = player.level(); var pos = player.blockPosition(); var dimension = level.dimension().identifier();
        var cursor = player.getData(RpgData.EXPLORATION_CURSOR);
        boolean reloaded = cursor.revision != snapshot.revision();
        // A stationary once-per-second retry observes structure starts that have just become available.
        if (!reloaded && cursor.position == pos.asLong() && dimension.equals(cursor.dimension) && player.tickCount % 20 != 0) { return; }
        try (var zone = Profiler.get().zone("covenant:exploration_discovery")) {
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null) { return; }
            var old = player.getData(RpgData.EXPLORATION);
            Set<Identifier> structures = Set.of();
            var known = old.in(dimension);
            var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            for (var entry : chunk.getAllReferences().entrySet()) {
                var id = registry.getKey(entry.getKey());
                if (id == null || known.structures().contains(id)) { continue; }
                var starts = entry.getValue().iterator();
                while (starts.hasNext()) {
                    long startPosition = starts.nextLong();
                    var startChunk = level.getChunkSource().getChunkNow(ChunkPos.getX(startPosition), ChunkPos.getZ(startPosition));
                    if (startChunk == null) { continue; }
                    var start = startChunk.getStartForStructure(entry.getKey());
                    if (start != null && start.isValid() && start.getBoundingBox().isInside(pos)) {
                        if (structures.isEmpty()) { structures = new HashSet<>(); }
                        structures.add(id); break;
                    }
                }
            }
            var biome = level.getBiome(pos).unwrapKey().orElseThrow().identifier();
            var next = old.discover(dimension, biome, structures);
            if (next != old) { player.setData(RpgData.EXPLORATION, next); }
            if (next != old || reloaded) { evaluate(player, snapshot); }
            cursor.position = pos.asLong(); cursor.dimension = dimension; cursor.revision = snapshot.revision();
        }
    }
    /** Re-evaluation recognizes saved observations; existing claims/levels are never reconstructed or revoked. */
    public static void evaluate(ServerPlayer player, ExplorationData.Snapshot snapshot) {
        ClassResources.requireServer(player);
        var history = player.getData(RpgData.EXPLORATION);
        if (ProfessionHarvesting.participant(player)) {
            var claims = new HashSet<>(history.claimed());
            for (var goal : snapshot.goals()) {
                if (!claims.contains(goal.id()) && goal.progress(history) >= goal.required()) { claims.add(goal.id()); }
            }
            int earned = claims.size() - history.claimed().size();
            if (earned > 0) {
                var state = player.getData(RpgData.PROFESSIONS); var old = state.tracks().get(ProgressionData.EXPLORATION);
                int maximum = ProgressionData.get(player.level().getServer()).maximum(ProgressionData.EXPLORATION);
                int level = old.level() + Math.min(earned, Math.max(0, maximum - old.level()));
                player.setData(RpgData.EXPLORATION, new ExplorationState(history.dimensions(), claims));
                if (level != old.level()) {
                    player.setData(RpgData.PROFESSIONS, state.with(ProgressionData.EXPLORATION, new ProfessionState.Track(level, 0, old.allocations())));
                    Progression.refreshView(player);
                    Progression.earned(player, ProgressionData.EXPLORATION, old.level(), level);
                }
            }
        }
        ExplorationView.refresh(player, snapshot);
    }
}
