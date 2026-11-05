package dev.covenant.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.covenant.rpg.skills.Bleeding;
import dev.covenant.rpg.skills.TrapKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Scheduled bleed/flame hits preserve native hurt state so separate sources cannot suppress each other. */
@Mixin(LivingEntity.class)
abstract class BleedHurtCooldownMixin {
    @Shadow protected float lastHurt;

    @WrapMethod(method = "hurtServer")
    private boolean covenant$preserveHurtCooldown(ServerLevel level, DamageSource source, float damage,
            Operation<Boolean> original) {
        if (!source.is(Bleeding.DAMAGE_TYPE) && !source.is(TrapKind.FLAME.damageType)) { return original.call(level, source, damage); }
        var target = (LivingEntity) (Object) this;
        int cooldown = target.invulnerableTime;
        float previousHit = lastHurt;
        try { return original.call(level, source, damage); }
        finally {
            target.invulnerableTime = cooldown;
            lastHurt = previousHit;
        }
    }
}
