package dev.covenant.rpg.progression;

import dev.covenant.rpg.CharacterProfile;
import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.skills.ClassResources;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Logical-server award boundary. Stored levels/XP are never recalculated merely because balance changes. */
public final class Progression {
    private Progression() {}
    /** Accepted XP excludes old stored XP normalized after reload and new XP discarded at a cap. */
    public record Earned(int level, long experience, long acceptedXp) {}

    /** Overflow-safe incremental advancement, preserving pre-existing XP when a changed cap blocks awards. */
    public static Earned advance(int level, long xp, long award, int cap,
            java.util.function.IntToLongFunction required) {
        if (award < 0 || xp < 0 || level < 0) { throw new IllegalArgumentException("Negative progression"); }
        if (award == 0 || level >= cap) { return new Earned(level, xp, 0); }
        long offered = award;
        // Normalize previously earned XP only upon a real new award, never during reload/login.
        while (level < cap && xp >= required.applyAsLong(level)) {
            long threshold = required.applyAsLong(level);
            if (threshold <= 0) { throw new IllegalArgumentException("Missing XP requirement"); }
            xp -= threshold; level++;
        }
        while (level < cap && award > 0) {
            long threshold = required.applyAsLong(level);
            if (threshold <= 0) { throw new IllegalArgumentException("Missing XP requirement"); }
            long room = threshold - xp;
            if (award < room) { xp += award; award = 0; break; }
            award -= room; level++; xp = 0;
        }
        return new Earned(level, xp, offered - award);
    }

    public static boolean awardCharacter(ServerPlayer player, ServerLevel source, long amount) {
        ClassResources.requireServer(player);
        if (amount < 0) { throw new IllegalArgumentException("Negative XP award"); }
        var profile = PlayerProfiles.get(player);
        if (!profile.created() || PlayerProfiles.getSelectedClass(player).isEmpty() || amount == 0) { return false; }
        var data = ProgressionData.get(source.getServer());
        int cap = Math.min(data.maximum(ProgressionData.CHARACTER), data.gate(source, ProgressionData.CHARACTER));
        var next = advance(profile.level(), profile.experience(), amount, cap,
                level -> data.required(ProgressionData.CHARACTER, level));
        if (next.level == profile.level() && next.experience == profile.experience()) { return false; }
        PlayerProfiles.update(player, p -> p.withProgression(next.level, next.experience, p.witherClaimed()));
        refreshView(player);
        earned(player, ProgressionData.CHARACTER, profile.level(), next.level);
        return true;
    }

    /** Shared classless award engine for trusted activity hooks and commands, respecting source-dimension caps. */
    public static boolean awardProfession(ServerPlayer player, ServerLevel source, Identifier track, long amount) {
        ClassResources.requireServer(player);
        if (amount < 0 || !ProgressionData.PROFESSIONS.contains(track)) { throw new IllegalArgumentException("Invalid profession award"); }
        if (track.equals(ProgressionData.EXPLORATION)) { return false; }
        var data = ProgressionData.get(source.getServer());
        if (data.professionRequirements().isEmpty() || amount == 0) { return false; }
        var state = player.getData(RpgData.PROFESSIONS);
        var old = state.tracks().get(track);
        var next = advance(old.level(), old.experience(), amount, data.gate(source, track), level -> data.required(track, level));
        if (next.level == old.level() && next.experience == old.experience()) { return false; }
        int fraction = track.equals(ProgressionData.BUILDING) && next.level >= data.gate(source, track)
                ? 0 : old.experienceFraction();
        var updated = state.with(track, new ProfessionState.Track(next.level, next.experience, old.allocations(), fraction));
        var building = state.tracks().get(ProgressionData.BUILDING);
        if (!track.equals(ProgressionData.BUILDING) && next.acceptedXp > 0) {
            updated = creditBuilding(updated, data, source, next.acceptedXp);
        }
        // Commit both tracks together so owner sync and persistence cannot observe a partial award.
        player.setData(RpgData.PROFESSIONS, updated);
        refreshView(player);
        var cue = levelMessage(track, old.level(), next.level);
        if (!track.equals(ProgressionData.BUILDING)) {
            var buildingCue = levelMessage(ProgressionData.BUILDING, building.level(), updated.tracks().get(ProgressionData.BUILDING).level());
            if (cue == null) { cue = buildingCue; }
            else if (buildingCue != null) { cue = cue.copy().append(" · ").append(buildingCue); }
        }
        earned(player, cue);
        return true;
    }

    /** Exact 65/100 conversion, with bounded integer arithmetic even for Long.MAX_VALUE awards. */
    private static ProfessionState creditBuilding(ProfessionState state, ProgressionData.Balance data,
            ServerLevel source, long accepted) {
        var old = state.tracks().get(ProgressionData.BUILDING);
        int cap = data.gate(source, ProgressionData.BUILDING);
        if (old.level() >= cap) { return state; }
        int hundredths = (int) (accepted % 100) * 65 + old.experienceFraction();
        long whole = accepted / 100 * 65 + hundredths / 100;
        var next = advance(old.level(), old.experience(), whole, cap,
                level -> data.required(ProgressionData.BUILDING, level));
        int fraction = next.level() >= cap ? 0 : hundredths % 100;
        return state.with(ProgressionData.BUILDING,
                new ProfessionState.Track(next.level(), next.experience(), old.allocations(), fraction));
    }

    /** Full level, point entitlement and claim are committed in the same immutable profile snapshot. */
    public static boolean firstWither(ServerPlayer player) {
        ClassResources.requireServer(player);
        var old = PlayerProfiles.get(player);
        var data = ProgressionData.get(player.level().getServer());
        int maximum = data.maximum(ProgressionData.CHARACTER);
        if (!old.created() || PlayerProfiles.getSelectedClass(player).isEmpty() || old.witherClaimed() || old.level() >= maximum) { return false; }
        int level = old.level() + 1;
        long xp = 0;
        if (level < maximum) {
            long denominator = data.required(ProgressionData.CHARACTER, old.level());
            long numerator = data.required(ProgressionData.CHARACTER, level);
            // Decimal arithmetic avoids overflow and integer-boundary rounding loss in proportional progress.
            xp = java.math.BigInteger.valueOf(old.experience()).multiply(java.math.BigInteger.valueOf(numerator))
                    .divide(java.math.BigInteger.valueOf(denominator)).min(java.math.BigInteger.valueOf(Long.MAX_VALUE)).longValue();
        }
        long retained = xp;
        PlayerProfiles.update(player, p -> p.withProgression(level, retained, true));
        refreshView(player);
        earned(player, ProgressionData.CHARACTER, old.level(), level);
        return true;
    }

    /** One owner-only cue per real award crossing levels; never called by loading or synchronization. */
    static void earned(ServerPlayer player, Identifier track, int before, int after) {
        earned(player, levelMessage(track, before, after));
    }

    private static Component levelMessage(Identifier track, int before, int after) {
        if (after <= before) { return null; }
        boolean character = track.equals(ProgressionData.CHARACTER);
        var name = character ? Component.translatable("progression.covenant.character")
                : Component.translatable("profession." + track.getNamespace() + "." + track.getPath());
        return Component.translatable(character ? "progression.covenant.earned_skill" : "progression.covenant.earned_profession",
                name, after, after - before);
    }

    private static void earned(ServerPlayer player, Component cue) {
        if (cue == null) { return; }
        player.sendSystemMessage(cue, true);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP),
                net.minecraft.sounds.SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), .35f, 1.25f, 0L));
    }

    public static void refreshView(ServerPlayer player) {
        var data = ProgressionData.get(player.level().getServer());
        var profile = PlayerProfiles.get(player);
        var view = new ProgressionView(profile.created() ? data.required(ProgressionData.CHARACTER, profile.level()) : 0,
                data.gate(player.level(), ProgressionData.CHARACTER), data.maximum(ProgressionData.CHARACTER),
                ClassResources.inCombat(player));
        if (!view.equals(player.getData(RpgData.PROGRESSION_VIEW))) { player.setData(RpgData.PROGRESSION_VIEW, view); }
        ProfessionView.refresh(player);
    }

    public static CharacterProfile atLevel(CharacterProfile profile, int level) {
        if (!profile.created() || level < 1 || level > 1000) { throw new IllegalArgumentException("Invalid testing level"); }
        return profile.withProgression(level, 0, profile.witherClaimed());
    }
}
