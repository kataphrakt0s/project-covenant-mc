package dev.covenant.rpg.progression;

import dev.covenant.rpg.RpgData;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Event-driven, logical-server activity rewards. No ticking, terrain loading, or equipment/drop changes. */
public final class ProfessionHarvesting {
    private static final Identifier MINING = ProfessionData.id("mining");
    private static final Identifier LOGGING = ProfessionData.id("logging");
    private static final Identifier FARMING = ProfessionData.id("farming");
    private static final Identifier CRAFTING = ProfessionData.id("crafting");
    /** Conservative recognition bounds, in blocks/reads per request; unsupported shapes fall back to ordinary chopping. */
    public static final int MAX_TREE_LOGS = 128, MAX_TREE_HEIGHT = 32, MAX_TREE_RADIUS = 4, MAX_TREE_READS = 4096;
    private ProfessionHarvesting() {}

    public record Award(Identifier profession, Identifier activity) {}

    private static boolean participant(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative() && !(player instanceof FakePlayer);
    }

    /** Called before native removal, after final BreakEvent acceptance and harvest/tool validation. */
    public static Award beforeRemoval(ServerPlayer player, BlockPos pos, BlockState state, boolean canHarvest) {
        if (!canHarvest || !participant(player)) { return null; }
        try (var zone = Profiler.get().zone("covenant:harvest_reward_check")) {
            var snapshot = ProfessionData.snapshot(player.level().getServer());
            var definitions = snapshot.catalogue().harvesting();
            for (var ore : definitions.ores()) {
                if (ore.blocks().test(state)) {
                    return isPlaced(player.level(), pos) ? null : new Award(MINING, ore.activity());
                }
            }
            for (var crop : definitions.crops()) {
                if (crop.test(state)) { return new Award(FARMING, ProfessionData.id("farming/mature_crop")); }
            }
            for (var tree : definitions.trees()) {
                if (tree.logs().test(state)) {
                    return recognizedTree(player.level(), pos, state, snapshot, tree) != null
                            ? new Award(LOGGING, ProfessionData.id("logging/log")) : null;
                }
            }
            return null;
        }
    }

    /** Called only when the native removal returned true and the original block actually changed. */
    public static void removed(ServerPlayer player, Award award) {
        if (award != null) { award(player, player.level(), award.profession(), award.activity()); }
    }

    /** Vanilla calls its breeding criterion only after the final breeding decision, using its chosen breeder. */
    public static void bred(ServerPlayer player, ServerLevel source) {
        award(player, source, FARMING, ProfessionData.id("farming/breeding"));
    }

    /** Native enchanting completion passes 1/2/3 for the selected option, after paying its vanilla costs. */
    public static void enchanted(ServerPlayer player, int option) {
        if (option >= 1 && option <= 3) {
            award(player, player.level(), CRAFTING, ProfessionData.id("crafting/enchant_option_" + option));
        }
    }

    private static void award(ServerPlayer player, ServerLevel source, Identifier profession, Identifier activity) {
        if (!participant(player)) { return; }
        long amount = ProfessionData.snapshot(source.getServer()).catalogue().activityRewards().getOrDefault(activity, 0L);
        Progression.awardProfession(player, source, profession, amount);
    }

    /** Observe the final placement-hook result, including cancellations by the last event subscriber. */
    public static void placed(BlockSnapshot snapshot) {
        if (snapshot.getLevel() instanceof ServerLevel level && isOre(level, snapshot.getCurrentState())) {
            markPlaced(level, snapshot.getPos(), true);
        }
    }

    private static boolean isOre(ServerLevel level, BlockState state) {
        if (state.is(Tags.Blocks.ORES)) { return true; }
        for (var ore : ProfessionData.snapshot(level.getServer()).catalogue().harvesting().ores()) {
            if (ore.blocks().test(state)) { return true; }
        }
        return false;
    }

    private static LevelChunk loaded(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static boolean isPlaced(ServerLevel level, BlockPos pos) {
        var chunk = loaded(level, pos);
        return chunk != null && chunk.hasData(RpgData.PLACED_ORES) && chunk.getData(RpgData.PLACED_ORES).positions.contains(pos.asLong());
    }

    private static void markPlaced(ServerLevel level, BlockPos pos, boolean placed) {
        var chunk = loaded(level, pos);
        if (chunk == null || (!placed && !chunk.hasData(RpgData.PLACED_ORES))) { return; }
        var data = chunk.getData(RpgData.PLACED_ORES);
        boolean changed = placed ? data.positions.add(pos.asLong()) : data.positions.remove(pos.asLong());
        if (changed) { chunk.markUnsaved(); }
    }

    /** Block edits invalidate tree evidence. Ore exclusions persist through native moving-piston states. */
    public static void changed(LevelChunk chunk, BlockPos pos, BlockState state) {
        if (!(chunk.getLevel() instanceof ServerLevel level)) { return; }
        if (chunk.hasData(RpgData.HARVEST_TREES)) { chunk.getData(RpgData.HARVEST_TREES).logs.remove(pos.asLong()); }
        if (chunk.hasData(RpgData.PLACED_ORES) && chunk.getData(RpgData.PLACED_ORES).positions.contains(pos.asLong())
                && !state.is(Blocks.MOVING_PISTON) && !isOre(level, state)) { markPlaced(level, pos, false); }
    }

    /** At most twelve native pushed blocks. Capture provenance before any source/destination is overwritten. */
    public record PistonMove(List<BlockPos> sources, Set<BlockPos> placed, Direction direction) {
        public void complete(ServerLevel level) {
            for (var pos : sources) { markPlaced(level, pos, false); markPlaced(level, pos.relative(direction), false); }
            for (var pos : placed) { markPlaced(level, pos.relative(direction), true); }
        }
    }
    public static PistonMove capturePiston(Level level, List<BlockPos> pushed, Direction direction, boolean extending) {
        if (!(level instanceof ServerLevel server)) { return null; }
        var sources = List.copyOf(pushed);
        var placed = new HashSet<BlockPos>();
        for (var source : sources) { if (isPlaced(server, source)) { placed.add(source); } }
        return new PistonMove(sources, placed, extending ? direction : direction.getOpposite());
    }

    static HarvestChunkState.TreeEvidence recognizedTree(ServerLevel level, BlockPos pos, BlockState state,
            ProfessionData.Snapshot snapshot, HarvestDefinitions.Tree tree) {
        try (var zone = Profiler.get().zone("covenant:tree_recognition")) {
            var chunk = loaded(level, pos);
            if (chunk == null) { return null; }
            if (chunk.hasData(RpgData.HARVEST_TREES)) {
                var cache = chunk.getData(RpgData.HARVEST_TREES);
                var evidence = cache.logs.get(pos.asLong());
                if (cache.revision == snapshot.revision() && evidence != null && evidence.logs().get(pos) == state) { return evidence; }
            }
            var search = new TreeSearch(level);
            var base = pos;
            for (int i = 0; i < MAX_TREE_HEIGHT; i++) {
                var below = search.read(base.below());
                if (below == null) { return null; }
                if (!tree.logs().test(below)) { break; }
                base = base.below();
            }
            var ground = search.read(base.below());
            if (ground == null || !snapshot.catalogue().harvesting().ground().test(ground)) { return null; }
            var queue = new ArrayDeque<BlockPos>(); var seen = new HashSet<BlockPos>();
            var logs = new LinkedHashMap<BlockPos, BlockState>();
            queue.add(base); seen.add(base); boolean canopy = false;
            while (!queue.isEmpty()) {
                var current = queue.removeFirst();
                var log = search.read(current);
                if (log == null) { return null; }
                logs.put(current, log);
                if (logs.size() > MAX_TREE_LOGS) { return null; }
                for (var direction : Direction.values()) {
                    var leafPos = current.relative(direction);
                    var leaf = search.read(leafPos);
                    if (leaf == null) { return null; }
                    if (current.getY() >= base.getY() + 2 && tree.leaves().test(leaf)
                            && leaf.hasProperty(BlockStateProperties.PERSISTENT) && !leaf.getValue(BlockStateProperties.PERSISTENT)) { canopy = true; }
                }
                // Upward and same-height adjacency supports trunks/branches without descending into nearby builds.
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) { continue; }
                            var next = current.offset(dx, dy, dz);
                            if (!seen.add(next)) { continue; }
                            var nextState = search.read(next);
                            if (nextState == null) { return null; }
                            if (!tree.logs().test(nextState)) { continue; }
                            if (Math.abs(next.getX() - base.getX()) > MAX_TREE_RADIUS || Math.abs(next.getZ() - base.getZ()) > MAX_TREE_RADIUS
                                    || next.getY() - base.getY() >= MAX_TREE_HEIGHT) { return null; }
                            queue.addLast(next);
                        }
                    }
                }
            }
            if (!canopy || logs.size() < 3 || !logs.containsKey(pos)) { return null; }
            var evidence = new HarvestChunkState.TreeEvidence(logs);
            for (var entry : logs.entrySet()) {
                var targetChunk = loaded(level, entry.getKey());
                if (targetChunk == null) { return null; }
                var cache = targetChunk.getData(RpgData.HARVEST_TREES);
                if (cache.revision != snapshot.revision()) { cache.logs.clear(); cache.revision = snapshot.revision(); }
                cache.logs.put(entry.getKey().asLong(), evidence);
            }
            return evidence;
        }
    }

    /** Every read, including absent chunks, consumes the fixed budget; no getChunk call may load terrain. */
    private static final class TreeSearch {
        private final ServerLevel level;
        private int reads;
        TreeSearch(ServerLevel level) { this.level = level; }
        BlockState read(BlockPos pos) {
            if (++reads > MAX_TREE_READS || level.isOutsideBuildHeight(pos)) { return null; }
            var chunk = loaded(level, pos);
            return chunk == null ? null : chunk.getBlockState(pos);
        }
    }
}
