package dev.covenant.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Use native piercing collision, exclusion tracking, block stopping, and saved arrow state. */
@Mixin(AbstractArrow.class)
public interface ArrowPiercingAccessor {
    @Invoker("setPierceLevel")
    void covenant$setPierceLevel(byte level);
}
