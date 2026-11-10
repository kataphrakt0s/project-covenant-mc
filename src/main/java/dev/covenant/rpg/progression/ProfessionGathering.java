package dev.covenant.rpg.progression;

import dev.covenant.rpg.RpgData;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Bounded, synchronous extra harvests through the native player path; no ticking, chunk loading or saved jobs. */
public final class ProfessionGathering {
    private ProfessionGathering() {}

    /** The native harvest wrapper exposes only its synchronous batch and currently harvested block. */
    public interface HarvestAccess {
        Batch covenant$gatheringBatch();
        BlockPos covenant$harvestingBlock();
        /** Confirm actual removal and suppress recursive expansion/native mining wear for a hoe-use harvest. */
        boolean covenant$harvestCrop(BlockPos pos);
    }

    /** Relocate only this batch's generated loot, after loot modifiers, without collecting nearby loose items. */
    public static void drops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) { return; }
        var access = (HarvestAccess) player.gameMode;
        var batch = access.covenant$gatheringBatch();
        if (batch == null || event.getLevel() != batch.level || !event.getPos().equals(access.covenant$harvestingBlock())) { return; }
        if (event.getPos().equals(batch.origin) && !event.getDrops().isEmpty()) {
            batch.dropPoint = event.getDrops().getFirst().position();
        }
        for (var drop : event.getDrops()) { drop.setPos(batch.dropPoint); }
    }

    private record Target(BlockPos pos, BlockState state) {}

    /** Captured before confirmed removal; executed only after the initiating harvest's native loot/tool handling. */
    public static final class Batch {
        private final ServerLevel level;
        private final ItemStack tool;
        private final boolean mining;
        private final List<Target> targets;
        private final BlockPos origin;
        private Vec3 dropPoint;

        private Batch(ServerPlayer player, BlockPos origin, boolean mining, List<Target> targets) {
            level = player.level(); tool = player.getMainHandItem(); this.mining = mining; this.targets = List.copyOf(targets);
            this.origin = origin.immutable(); dropPoint = Vec3.atCenterOf(origin);
        }

        public void harvest(ServerPlayer player, java.util.function.Predicate<BlockPos> harvest) {
            try (var zone = Profiler.get().zone("covenant:gathering_harvest")) {
                for (var target : targets) {
                    // Never continue with a replacement tool, even if an item-break handler supplied another one.
                    if (!participant(player) || player.level() != level || player.isShiftKeyDown()
                            || player.getMainHandItem() != tool || !suitable(tool, mining)) { return; }
                    var state = read(level, target.pos());
                    if (state != target.state() || !level.getWorldBorder().isWithinBounds(target.pos()) || !level.mayInteract(player, target.pos())
                            || state.getDestroySpeed(level, target.pos()) < 0
                            || !state.canHarvestBlock(level, target.pos(), player)) { return; }
                    // The mixin suppresses recursive expansion. Native events, Adventure checks, durability,
                    // loot, enchantments, exhaustion and per-original-block profession XP run for every block.
                    if (!harvest.test(target.pos())) { return; }
                }
            }
        }
    }

    private static boolean participant(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator() && !(player instanceof FakePlayer);
    }

    private static boolean suitable(ItemStack tool, boolean mining) {
        return !tool.isEmpty() && tool.is(mining ? ItemTags.PICKAXES : ItemTags.AXES);
    }

    private static int limit(ServerPlayer player, ProfessionData.Snapshot snapshot, Identifier ability) {
        var node = snapshot.catalogue().abilities().get(ability);
        if (node == null) { return 0; }
        int rank = player.getData(RpgData.PROFESSIONS).tracks().get(node.profession()).rank(ability);
        return rank == 0 ? 0 : node.ranks().get(Math.min(rank, node.ranks().size()) - 1).blocks();
    }

    /** Final BreakEvent/tool acceptance has happened. The supplied tool is the native pre-durability copy. */
    public static Batch prepare(ServerPlayer player, BlockPos origin, BlockState original, boolean canHarvest, ItemStack tool) {
        if (!canHarvest || !participant(player) || player.isShiftKeyDown() || player.getMainHandItem().isEmpty()) { return null; }
        try (var zone = Profiler.get().zone("covenant:gathering_prepare")) {
            var snapshot = ProfessionData.snapshot(player.level().getServer());
            int miningLimit = limit(player, snapshot, ProfessionData.VEIN_MINING);
            if (miningLimit > 1 && suitable(tool, true)) {
                for (var ore : snapshot.catalogue().harvesting().ores()) {
                    if (!ore.blocks().test(original)) { continue; }
                    // Reward/activity ID is the existing family identity; multiple pack mappings may share it.
                    return collect(player, origin, original, miningLimit, true, state -> {
                        for (var candidate : snapshot.catalogue().harvesting().ores()) {
                            if (candidate.blocks().test(state)) { return candidate.activity().equals(ore.activity()); }
                        }
                        return false;
                    }, null);
                }
            }
            int loggingLimit = limit(player, snapshot, ProfessionData.TREE_FELLING);
            if (loggingLimit <= 1 || !suitable(tool, false)) { return null; }
            for (var tree : snapshot.catalogue().harvesting().trees()) {
                if (!tree.logs().test(original)) { continue; }
                var evidence = ProfessionHarvesting.recognizedTree(player.level(), origin, original, snapshot, tree);
                if (evidence == null) { return null; }
                var remaining = remaining(player.level(), evidence, snapshot.revision());
                if (!remaining.containsKey(origin)) { return null; }
                for (var pos : remaining.keySet()) {
                    if (pos.getY() < origin.getY()) { return null; } // Only a lowest remaining log may start a batch.
                }
                return collect(player, origin, original, loggingLimit, false, tree.logs()::test, remaining);
            }
            return null;
        }
    }

    private static Map<BlockPos, BlockState> remaining(ServerLevel level, HarvestChunkState.TreeEvidence evidence, long revision) {
        var remaining = new HashMap<BlockPos, BlockState>();
        for (var entry : evidence.logs().entrySet()) {
            var pos = entry.getKey();
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null) { return Map.of(); }
            if (!chunk.hasData(RpgData.HARVEST_TREES)) { continue; }
            var cache = chunk.getData(RpgData.HARVEST_TREES);
            if (cache.revision == revision && cache.logs.get(pos.asLong()) == evidence && chunk.getBlockState(pos) == entry.getValue()) {
                remaining.put(pos, entry.getValue());
            }
        }
        return remaining;
    }

    /** Stable breadth-first order. At most 64 members and 26 neighbour reads per member; trees never descend. */
    private static Batch collect(ServerPlayer player, BlockPos origin, BlockState original, int limit, boolean mining,
            java.util.function.Predicate<BlockState> matches, Map<BlockPos, BlockState> certified) {
        var queue = new ArrayDeque<Target>(); var seen = new HashSet<BlockPos>(); var targets = new ArrayList<Target>();
        queue.add(new Target(origin.immutable(), original)); seen.add(origin.immutable());
        int found = 1;
        while (!queue.isEmpty() && found < limit) {
            var current = queue.removeFirst();
            for (int dy = mining ? -1 : 0; dy <= 1 && found < limit; dy++) {
                for (int dx = -1; dx <= 1 && found < limit; dx++) {
                    for (int dz = -1; dz <= 1 && found < limit; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) { continue; }
                        var pos = current.pos().offset(dx, dy, dz);
                        if (!seen.add(pos)) { continue; }
                        var state = certified == null ? read(player.level(), pos) : certified.get(pos);
                        if (state == null || !matches.test(state)) { continue; }
                        var target = new Target(pos, state); queue.addLast(target); targets.add(target); found++;
                    }
                }
            }
        }
        return targets.isEmpty() ? null : new Batch(player, origin, mining, targets);
    }

    private static BlockState read(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) { return null; }
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }
}
