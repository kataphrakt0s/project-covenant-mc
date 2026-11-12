package dev.covenant.rpg.progression;

import dev.covenant.rpg.RpgData;
import java.util.Collection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

/** Runs on native accepted reel-in, after ItemFishedEvent cancellation has been resolved. */
public final class ProfessionFishing {
    private ProfessionFishing() {}
    public static void caught(ServerPlayer player, FishingHook hook, Collection<ItemStack> loot) {
        if (!ProfessionHarvesting.participant(player) || hook.isRemoved() || hook.getPlayerOwner() != player
                || hook.level() != player.level() || loot.stream().noneMatch(stack -> !stack.isEmpty())
                || hook.getData(RpgData.FISHING_REWARDED)) { return; }
        hook.setData(RpgData.FISHING_REWARDED, true);
        long reward = ProfessionData.snapshot(player.level().getServer()).catalogue().activityRewards()
                .getOrDefault(ProfessionData.id("fishing/catch"), 0L);
        Progression.awardProfession(player, player.level(), ProgressionData.FISHING, reward);
    }
}
