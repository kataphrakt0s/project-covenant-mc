package dev.covenant.rpg.skills;

import dev.covenant.mixin.ArrowPiercingAccessor;
import dev.covenant.rpg.RpgData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Bow-family preparation and execution; Skills owns resource and cooldown commitment. */
final class BowSkills {
    private BowSkills() {}

    record Shot(ItemStack bow, ItemStack ammunition, int ammunitionCost) {}

    static Shot prepare(ServerPlayer player) {
        var bow = player.getMainHandItem();
        var ammo = player.getProjectile(bow);
        boolean infinite = player.hasInfiniteMaterials() || ((ArrowItem) ammo.getItem()).isInfinite(ammo, bow, player);
        int amount = infinite ? 0 : Math.max(0, EnchantmentHelper.processAmmoUse(player.level(), bow, ammo, 1));
        return amount > ammo.getCount() ? null : new Shot(bow, ammo, amount);
    }

    static void fire(ServerPlayer player, SkillDefinition skill, Shot shot) {
        var bow = shot.bow();
        var ammo = shot.ammunition();
        var arrow = ((ArrowItem) ammo.getItem()).createArrow(player.level(), ammo, player, bow);
        // Match NeoForge's weapon customization hook before applying the skill's explicit tuning.
        arrow = ((BowItem) bow.getItem()).customArrow(arrow, ammo, bow);
        double multiplier = AuraDamage.multiplier(player, false);
        arrow.setBaseDamage(2 * skill.power() * multiplier);
        arrow.setData(RpgData.AURA_PROJECTILE_POWER, multiplier);
        ((ArrowPiercingAccessor) arrow).covenant$setPierceLevel((byte) (skill == SkillDefinition.PIERCING_SHOT ? 2 : 0));
        arrow.pickup = shot.ammunitionCost() == 0 ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 3, 0);
        arrow.setData(RpgData.SKILL_SHOT, true);
        ProjectileCombat.markSkill(arrow);
        Projectile.spawnProjectile(arrow, player.level(), ammo.copyWithCount(1));
        ammo.shrink(shot.ammunitionCost());
        bow.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        if (skill != SkillDefinition.AIMED_SHOT) { SkillVisuals.bowDraw(player, SkillVisuals.BOW_DRAW_TICKS); }
    }
}
