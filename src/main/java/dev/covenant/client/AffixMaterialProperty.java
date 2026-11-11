package dev.covenant.client;

import com.mojang.serialization.Codec;
import dev.covenant.rpg.equipment.ItemizationData;
import dev.covenant.rpg.equipment.Workbench;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import org.jspecify.annotations.Nullable;

/** Reads the existing synced component; resource-pack fallbacks cover unknown families and rarities. */
public record AffixMaterialProperty(boolean rarity) implements SelectItemModelProperty<String> {
    public static final Type<AffixMaterialProperty, String> TYPE = Type.create(
            Codec.BOOL.optionalFieldOf("rarity", false).xmap(AffixMaterialProperty::new, AffixMaterialProperty::rarity), Codec.STRING);
    public static void register(RegisterSelectItemModelPropertyEvent event) { event.register(ItemizationData.id("affix_material"), TYPE); }
    @Override public String get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext context) {
        var id = stack.get(Workbench.MATERIAL_ID.get());
        return id == null ? "" : (rarity ? id.rarity() : id.family()).toString();
    }
    @Override public Codec<String> valueCodec() { return Codec.STRING; }
    @Override public Type<AffixMaterialProperty, String> type() { return TYPE; }
}
