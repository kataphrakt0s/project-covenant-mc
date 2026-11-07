package dev.covenant.rpg.progression;

import dev.covenant.Covenant;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Combat Respec's native held-use flow, refunding all four profession tracks atomically. */
public final class ProfessionRespecItem extends Item {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Covenant.MOD_ID);
    public static final java.util.function.Supplier<ProfessionRespecItem> ITEM = ITEMS.registerItem("profession_respec", ProfessionRespecItem::new);
    public ProfessionRespecItem(Properties properties) { super(properties); }
    public static void register(IEventBus bus) { ITEMS.register(bus); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server && !ProfessionAllocations.canRespec(server)) { return InteractionResult.FAIL; }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return CombatRespecItem.USE_TICKS; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BOW; }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof ServerPlayer player && !ProfessionAllocations.canRespec(player)) { player.stopUsingItem(); }
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && ProfessionAllocations.respec(player)) {
            stack.shrink(1);
            player.sendSystemMessage(ProfessionAllocations.message("refunded"));
        }
        return stack;
    }
}
