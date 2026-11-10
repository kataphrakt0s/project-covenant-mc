package dev.covenant.rpg.stats;

import dev.covenant.Covenant;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.equipment.EquipmentCategory;
import dev.covenant.rpg.equipment.RpgModifier;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/** Bridges per-stack pages to native transient equipment attributes without changing item defaults. */
final class PageAttributeModifiers {
    private static final Map<EquipmentCategory, EquipmentSlotGroup> SLOTS = Map.of(
            EquipmentCategory.MAIN_HAND, EquipmentSlotGroup.MAINHAND,
            EquipmentCategory.OFF_HAND, EquipmentSlotGroup.OFFHAND,
            EquipmentCategory.HEAD_ARMOR, EquipmentSlotGroup.HEAD,
            EquipmentCategory.BODY_ARMOR, EquipmentSlotGroup.CHEST,
            EquipmentCategory.LEG_ARMOR, EquipmentSlotGroup.LEGS,
            EquipmentCategory.FOOT_ARMOR, EquipmentSlotGroup.FEET);

    private PageAttributeModifiers() {}

    static void apply(ItemAttributeModifierEvent event) {
        var page = event.getItemStack().get(RpgData.AFFIX_PAGE.get());
        if (page == null) { return; }
        try (var zone = Profiler.get().zone("covenant:equipment_modifiers")) {
            var slot = SLOTS.get(page.category());
            // Custom categories retain their data but require an explicit future slot integration.
            if (slot == null) { return; }
            String source = "page/" + page.identity() + "/";
            add(event, page.modifiers(), slot, source + "base/");
            for (int i = 0; i < page.affixes().size(); i++) {
                add(event, page.affixes().get(i).modifiers(), slot, source + "affix/" + i + "/");
            }
        }
    }

    private static void add(ItemAttributeModifierEvent event, List<RpgModifier> modifiers,
            EquipmentSlotGroup slot, String source) {
        for (int i = 0; i < modifiers.size(); i++) {
            var modifier = modifiers.get(i);
            var attribute = BuiltInRegistries.ATTRIBUTE.get(modifier.stat());
            // Unknown IDs remain round-trippable data, never an alias for a different stat.
            if (attribute.isEmpty()) { continue; }
            var operation = switch (modifier.operation()) {
                case ADD -> AttributeModifier.Operation.ADD_VALUE;
                case MULTIPLY_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                case MULTIPLY_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            };
            event.addModifier(attribute.get(), new AttributeModifier(
                    Identifier.fromNamespaceAndPath(Covenant.MOD_ID, source + i),
                    modifier.amount(), operation), slot);
        }
    }
}
