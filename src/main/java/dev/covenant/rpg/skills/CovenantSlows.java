package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** One derived modifier; native Chilled keeps its saved timer while weaker slows remain represented. */
public final class CovenantSlows {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("covenant", "movement_slow");
    private CovenantSlows() {}
    public static void update(LivingEntity target) {
        try (var zone = Profiler.get().zone("covenant:slow_update")) {
            if (target.level().isClientSide()) { return; }
            double slow = Auras.passive(target, Auras.Kind.FREEZING) ? .1 : 0;
            if (target.hasEffect(Chilled.EFFECT)) { slow = Math.max(slow, .3); }
            slow = Math.max(slow, DischargeSlow.strength(target));
            if (target.hasData(RpgData.FROST_SOURCES) && !target.getData(RpgData.FROST_SOURCES).patches.isEmpty()) { slow = Math.max(slow, .5); }
            var speed = target.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed == null) { return; }
            var previous = speed.getModifier(ID);
            if (slow == 0) { if (previous != null) { speed.removeModifier(ID); } }
            else if (previous == null || previous.amount() != -slow) {
                speed.addOrUpdateTransientModifier(new AttributeModifier(ID, -slow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }
}
