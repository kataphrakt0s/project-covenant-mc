package dev.covenant.rpg.equipment;

import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Shared display units for stored modifiers and server-authored possible results. */
public final class AffixText {
    private AffixText() {}

    public static Component stat(Identifier id) {
        var attribute = BuiltInRegistries.ATTRIBUTE.getValue(id);
        return attribute == null ? Component.literal(id.toString()) : Component.translatable(attribute.getDescriptionId());
    }

    public static String value(Identifier stat, RpgModifier.Operation operation, double amount) {
        boolean percent = operation != RpgModifier.Operation.ADD || stat.equals(ItemizationData.id("dodge"));
        String number = String.format(Locale.ROOT, "%.3f", amount * (percent ? 100 : 1)).replaceAll("\\.?0+$", "");
        return (amount >= 0 ? "+" : "") + number + (percent ? "%" : "");
    }

    public static String range(WorkbenchOperations.Plan plan) {
        var family = plan.family();
        var range = family.ranges().get(plan.tier() - 1);
        double sign = family.reduction() ? -1 : 1;
        double a = range.min() * plan.affixRarity().magnitude() * sign;
        double b = range.max() * plan.affixRarity().magnitude() * sign;
        return value(family.stat(), family.operation(), Math.min(a, b)) + " to "
                + value(family.stat(), family.operation(), Math.max(a, b)) + " " + stat(family.stat()).getString();
    }
}
