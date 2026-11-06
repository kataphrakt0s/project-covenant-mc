package dev.covenant.rpg.progression;

import dev.covenant.Covenant;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.skills.ClassResources;
import dev.covenant.rpg.skills.SkillLoadouts;
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

/** Hold for two seconds out of combat; interrupted and rejected uses never spend the item. */
public final class CombatRespecItem extends Item {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Covenant.MOD_ID);
    public static final java.util.function.Supplier<CombatRespecItem> ITEM = ITEMS.registerItem("combat_respec", CombatRespecItem::new);
    public static final int USE_TICKS = 40;
    public CombatRespecItem(Properties properties) { super(properties); }
    public static void register(IEventBus bus) { ITEMS.register(bus); }
    private static boolean canUse(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !ClassResources.inCombat(player)
                && !player.getData(RpgData.SKILL_LOADOUT).ranks().isEmpty();
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && !canUse(serverPlayer)) { return InteractionResult.FAIL; }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return USE_TICKS; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BOW; }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof ServerPlayer player && !canUse(player)) { player.stopUsingItem(); }
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && SkillLoadouts.respec(player)) {
            stack.shrink(1);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Skills refunded. All skill bindings cleared."));
        }
        return stack;
    }
}
