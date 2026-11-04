package dev.covenant.rpg.equipment;

import com.mojang.serialization.Codec;
import dev.covenant.Covenant;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Open namespaced categories, not an enum of Minecraft slots or item classes. Category a:b uses
 * item tag a:affix_compatible/b. Datapacks can opt in any mod's items without a code dependency.
 * Compatibility permits a page; it does not define where an item is currently equipped.
 */
public record EquipmentCategory(Identifier id) {
    public static final Codec<EquipmentCategory> CODEC = Identifier.CODEC.xmap(EquipmentCategory::new, EquipmentCategory::id);
    public static final EquipmentCategory MAIN_HAND = builtin("main_hand");
    public static final EquipmentCategory OFF_HAND = builtin("off_hand");
    public static final EquipmentCategory HEAD_ARMOR = builtin("head_armor");
    public static final EquipmentCategory BODY_ARMOR = builtin("body_armor");
    public static final EquipmentCategory LEG_ARMOR = builtin("leg_armor");
    public static final EquipmentCategory FOOT_ARMOR = builtin("foot_armor");
    public static final EquipmentCategory RING = builtin("ring");
    public static final EquipmentCategory NECKLACE = builtin("necklace");
    public static final EquipmentCategory ACCESSORY = builtin("accessory");
    public static final EquipmentCategory TRINKET = builtin("trinket");

    public EquipmentCategory {
        Objects.requireNonNull(id);
    }

    public TagKey<Item> compatibilityTag() {
        return TagKey.create(Registries.ITEM, id.withPath("affix_compatible/" + id.getPath()));
    }

    public boolean supports(ItemStack stack) {
        return !stack.isEmpty() && stack.is(compatibilityTag());
    }

    private static EquipmentCategory builtin(String path) {
        return new EquipmentCategory(Identifier.fromNamespaceAndPath(Covenant.MOD_ID, path));
    }
}
