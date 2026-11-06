package dev.covenant.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.covenant.rpg.skills.AuraDamage;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.Local;

/** Capture every native shot after creation and before spawning; draw power is native bow speed / 3. */
@Mixin(ProjectileWeaponItem.class)
abstract class AuraProjectileMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "shoot", at = @At("HEAD"), cancellable = true)
    private void covenant$freezeShooting(net.minecraft.server.level.ServerLevel level, LivingEntity shooter,
            net.minecraft.world.InteractionHand hand, ItemStack weapon, java.util.List<ItemStack> ammo,
            float power, float uncertainty, boolean crit, LivingEntity target,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (dev.covenant.rpg.skills.Auras.frozen(shooter)) { ci.cancel(); }
    }

    @WrapOperation(method = "shoot", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/item/ProjectileWeaponItem;createProjectile(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile covenant$capture(ProjectileWeaponItem item, Level level, LivingEntity shooter,
            ItemStack weapon, ItemStack ammo, boolean crit, Operation<Projectile> original, @Local(argsOnly = true, ordinal = 0) float power) {
        var projectile = original.call(item, level, shooter, weapon, ammo, crit);
        AuraDamage.snapshot(projectile, shooter, weapon.getItem() instanceof BowItem ? power / 3 : 1);
        return projectile;
    }
}
