package dev.covenant.rpg.equipment;

import com.mojang.serialization.MapCodec;
import dev.covenant.Covenant;
import dev.covenant.rpg.RpgData;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.*;

/** Native station registration; no ticker, chunk forcing, or client classes in common code. */
public final class Workbench {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Covenant.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Covenant.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Covenant.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Covenant.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Covenant.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>> LOOT =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Covenant.MOD_ID);
    public static final Supplier<StationBlock> BLOCK = BLOCKS.registerBlock("workbench", StationBlock::new,
            properties -> properties.strength(3.5f).sound(net.minecraft.world.level.block.SoundType.METAL));
    public static final Supplier<net.minecraft.world.item.BlockItem> ITEM = ITEMS.registerSimpleBlockItem("workbench", BLOCK);
    public static final Supplier<Item> MATERIAL = ITEMS.registerItem("affix_component", MaterialItem::new);
    public static final Supplier<BlockEntityType<WorkbenchBlockEntity>> ENTITY = ENTITIES.register("workbench",
            () -> new BlockEntityType<>(WorkbenchBlockEntity::new, BLOCK.get()));
    public static final Supplier<MenuType<WorkbenchMenu>> MENU = MENUS.register("workbench",
            () -> new MenuType<>(WorkbenchMenu::new, FeatureFlags.VANILLA_SET));
    public static final Supplier<DataComponentType<EquipmentQuality>> QUALITY = COMPONENTS.registerComponentType("equipment_quality",
            builder -> builder.persistent(EquipmentQuality.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(EquipmentQuality.CODEC)));
    public static final Supplier<DataComponentType<AffixMaterial>> MATERIAL_ID = COMPONENTS.registerComponentType("affix_material",
            builder -> builder.persistent(AffixMaterial.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(AffixMaterial.CODEC)));
    static { LOOT.register("affixed_equipment", () -> AffixedLoot.CODEC); }
    private Workbench() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus); COMPONENTS.register(bus); LOOT.register(bus);
        bus.addListener(WorkbenchMenu::registerNetwork);
        NeoForge.EVENT_BUS.addListener(ItemizationData::register);
        NeoForge.EVENT_BUS.addListener(Workbench::tooltip);
    }
    public static ItemStack material(AffixMaterial id, int count) {
        var stack = new ItemStack(MATERIAL.get(), count); stack.set(MATERIAL_ID.get(), id); return stack;
    }
    public static String name(net.minecraft.resources.Identifier id) {
        String[] words = id.getPath().replace('/', ' ').replace('_', ' ').split(" "); var result = new StringBuilder();
        for (var word : words) { if (!word.isEmpty()) { if (!result.isEmpty()) { result.append(' '); }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)); } }
        return result.toString();
    }
    public static ChatFormatting color(net.minecraft.resources.Identifier id) {
        return switch (id.toString()) { case "covenant:uncommon" -> ChatFormatting.GREEN; case "covenant:rare" -> ChatFormatting.AQUA;
            case "covenant:epic" -> ChatFormatting.LIGHT_PURPLE; case "covenant:legendary" -> ChatFormatting.GOLD; default -> ChatFormatting.WHITE; };
    }
    private static void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack(); var quality = stack.get(QUALITY.get());
        if (stack.is(dev.covenant.rpg.progression.CombatRespecItem.ITEM.get())) {
            event.getToolTip().add(Component.literal("Hold use for 2 seconds out of combat.").withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("Refunds combat skills and clears bindings; keeps level and XP.").withStyle(ChatFormatting.GRAY));
        } else if (stack.is(dev.covenant.rpg.progression.ProfessionRespecItem.ITEM.get())) {
            event.getToolTip().add(Component.literal("Hold use for 2 seconds out of combat.").withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("Refunds all four professions' allocations; keeps levels and XP.").withStyle(ChatFormatting.GRAY));
        } else if (stack.is(ITEM.get())) {
            event.getToolTip().add(Component.literal("Enchant at a table for Crafting XP, then learn Basic Affix Crafting.").withStyle(ChatFormatting.GRAY));
        }
        if (quality != null) {
            event.getToolTip().add(Component.literal(name(quality.rarity()) + " equipment").withStyle(color(quality.rarity())));
            event.getToolTip().add(Component.literal("Crafting Potential: " + quality.potential() + " remaining").withStyle(ChatFormatting.GRAY));
        }
        AffixPages.get(stack).ifPresent(page -> {
            for (var affix : page.affixes()) {
                event.getToolTip().add(Component.literal(name(affix.id()) + "  [T" + affix.tier() + " · " + name(affix.rarity()) + "]").withStyle(color(affix.rarity())));
                for (var modifier : affix.modifiers()) {
                    event.getToolTip().add(Component.literal("  " + AffixText.value(modifier.stat(), modifier.operation(), modifier.amount()) + " ")
                            .append(AffixText.stat(modifier.stat())).withStyle(ChatFormatting.GRAY));
                }
            }
        });
        if (stack.is(MATERIAL.get())) {
            var identity = stack.get(MATERIAL_ID.get());
            event.getToolTip().add(Component.literal("Workbench ingredient · matches family and rarity").withStyle(ChatFormatting.GRAY));
            if (identity != null && (!identity.family().getNamespace().equals(Covenant.MOD_ID)
                    || !identity.rarity().getNamespace().equals(Covenant.MOD_ID))) {
                event.getToolTip().add(Component.literal(identity.family() + " / " + identity.rarity()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
    private static final class MaterialItem extends Item {
        MaterialItem(Properties properties) { super(properties); }
        @Override public Component getName(ItemStack stack) {
            var identity = stack.get(MATERIAL_ID.get());
            return identity == null ? super.getName(stack) : Component.literal(name(identity.rarity()) + " " + name(identity.family()) + " Component").withStyle(color(identity.rarity()));
        }
    }
    public static final class StationBlock extends BaseEntityBlock {
        public static final MapCodec<StationBlock> CODEC = simpleCodec(StationBlock::new);
        StationBlock(Properties properties) { super(properties); }
        @Override public MapCodec<StationBlock> codec() { return CODEC; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WorkbenchBlockEntity(pos, state); }
        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof WorkbenchBlockEntity station) { player.openMenu(station); }
            return InteractionResult.SUCCESS;
        }
    }
}
