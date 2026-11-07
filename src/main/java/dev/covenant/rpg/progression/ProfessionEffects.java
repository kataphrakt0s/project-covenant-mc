package dev.covenant.rpg.progression;

import dev.covenant.rpg.RpgData;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.OwnableEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Concrete profession effects; no cached effective stats or owner traversal for summon damage. */
public final class ProfessionEffects {
    /** Summons such as future Ballistas can exclude their attributed damage through this native tag. */
    public static final TagKey<DamageType> IGNORES_OFFENSIVE_MASTERY = TagKey.create(Registries.DAMAGE_TYPE,
            ProfessionData.id("ignores_offensive_mastery"));
    private ProfessionEffects() {}

    public static void incoming(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                || source.getDirectEntity() instanceof OwnableEntity || source.is(IGNORES_OFFENSIVE_MASTERY)) { return; }
        var node = ProfessionData.snapshot(player.level().getServer()).catalogue().abilities().get(ProfessionData.OFFENSIVE_MASTERY);
        if (node == null) { return; }
        int rank = player.getData(RpgData.PROFESSIONS).tracks().get(node.profession()).rank(node.id());
        if (rank == 0) { return; }
        // Removing authored ranks never erases the saved purchase; use the last remaining rank's tuning.
        double bonus = node.ranks().get(Math.min(rank, node.ranks().size()) - 1).bonus();
        event.setAmount((float) Math.min(Float.MAX_VALUE, event.getAmount() * (1 + bonus)));
    }
}
