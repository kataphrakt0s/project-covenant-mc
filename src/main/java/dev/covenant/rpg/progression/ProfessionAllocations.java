package dev.covenant.rpg.progression;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.skills.ClassResources;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;

/** Atomic class-independent point purchases. Client intent never supplies trusted costs or effects. */
public final class ProfessionAllocations {
    private ProfessionAllocations() {}

    public static Component apply(ServerPlayer player, Identifier profession, long revision,
            Map<Identifier, ProfessionState.Allocation> expected, Map<Identifier, Integer> desired) {
        try (var zone = Profiler.get().zone("covenant:profession_allocation")) {
            ClassResources.requireServer(player);
            if (!player.isAlive() || player.isSpectator()) { return message("unavailable"); }
            if (ClassResources.inCombat(player)) { return message("combat"); }
            var state = player.getData(RpgData.PROFESSIONS);
            var previous = state.tracks().get(profession);
            var snapshot = ProfessionData.snapshot(player.level().getServer());
            if (previous == null || snapshot == null || snapshot.revision() != revision || !previous.allocations().equals(expected)) { return message("stale"); }
            if (previous.ranks().equals(desired)) { return message("no_pending"); }
            if (desired.size() > 128 || !desired.keySet().containsAll(expected.keySet())) { return message("invalid"); }
            var nodes = snapshot.catalogue().abilities();
            var allocations = new HashMap<>(previous.allocations());
            long spent = previous.spentPoints();
            for (var entry : desired.entrySet()) {
                int oldRank = previous.rank(entry.getKey());
                int rank = entry.getValue();
                if (rank < oldRank || rank < 1 || rank > 64) { return message("invalid"); }
                if (rank == oldRank) { continue; } // Removed/retuned catalogue entries retain their paid reservation.
                var node = nodes.get(entry.getKey());
                if (node == null || !node.profession().equals(profession) || !node.implemented() || rank > node.ranks().size()) { return message("planned"); }
                if (node.prerequisite().filter(id -> desired.getOrDefault(id, 0) == 0).isPresent()) { return message("prerequisite"); }
                long paid = previous.allocations().containsKey(node.id()) ? previous.allocations().get(node.id()).paidPoints() : 0;
                for (int index = oldRank; index < rank; index++) {
                    var next = node.ranks().get(index);
                    if (previous.level() < next.level()) { return message("level"); }
                    spent += next.cost(); paid += next.cost();
                }
                if (spent > previous.level() || paid > 1000) { return message("points"); }
                allocations.put(node.id(), new ProfessionState.Allocation(rank, (int) paid));
            }
            player.setData(RpgData.PROFESSIONS, state.with(profession,
                    new ProfessionState.Track(previous.level(), previous.experience(), allocations, previous.experienceFraction())));
            return message("applied");
        }
    }

    public static boolean canRespec(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !ClassResources.inCombat(player)
                && player.getData(RpgData.PROFESSIONS).tracks().values().stream().anyMatch(t -> !t.allocations().isEmpty());
    }
    /** One saved snapshot clears all tracks; the held-item caller consumes one item after success. */
    public static boolean respec(ServerPlayer player) {
        ClassResources.requireServer(player);
        if (!canRespec(player)) { return false; }
        var tracks = new HashMap<Identifier, ProfessionState.Track>();
        player.getData(RpgData.PROFESSIONS).tracks().forEach((id, track) -> tracks.put(id,
                new ProfessionState.Track(track.level(), track.experience(), Map.of(), track.experienceFraction())));
        player.setData(RpgData.PROFESSIONS, new ProfessionState(tracks));
        return true;
    }
    public static Component message(String key) { return Component.translatable("professions.covenant." + key); }
}
