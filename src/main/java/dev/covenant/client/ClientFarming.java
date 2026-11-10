package dev.covenant.client;

import dev.covenant.rpg.RpgData;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Consume a predicted hoe-use target so vanilla does not also send an offhand action. */
final class ClientFarming {
    private ClientFarming() {}
    static void use(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        if (!event.getLevel().isClientSide() || event.isCanceled() || event.getUseItem().isFalse() || event.getUseBlock().isFalse()
                || event.getHand() != InteractionHand.MAIN_HAND || !player.isAlive() || player.isCreative() || player.isSpectator()
                || !player.getMainHandItem().is(ItemTags.HOES) || player.getCooldowns().isOnCooldown(player.getMainHandItem())) { return; }
        var state = event.getLevel().getBlockState(event.getPos());
        for (var crop : player.getData(RpgData.PROFESSION_VIEW).crops()) {
            if (crop.test(state)) {
                // MultiPlayerGameMode still sends the original use-on packet; predict only consumption and swing.
                event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS); return;
            }
        }
    }
}
