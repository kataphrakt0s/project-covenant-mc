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
        boolean multishot = skill == SkillDefinition.MULTISHOT;
        var fan = multishot ? java.util.Optional.of(java.util.UUID.randomUUID()) : java.util.Optional.<java.util.UUID>empty();
        int count = multishot ? 5 : 1;
        for (int index = 0; index < count; index++) {
            var arrow = ((ArrowItem) ammo.getItem()).createArrow(player.level(), ammo, player, bow);
            // Match NeoForge's weapon customization hook before applying the skill's explicit tuning.
            arrow = ((BowItem) bow.getItem()).customArrow(arrow, ammo, bow);
            double multiplier = AuraDamage.multiplier(player, false);
            arrow.setBaseDamage((skill.id().equals(SkillDefinition.ARCANE_ARROW.id()) ? 2 : 2 * skill.power()) * multiplier);
            arrow.setData(RpgData.AURA_PROJECTILE_POWER, multiplier);
            ((ArrowPiercingAccessor) arrow).covenant$setPierceLevel((byte) (skill == SkillDefinition.PIERCING_SHOT ? 2 : 0));
            arrow.pickup = shot.ammunitionCost() == 0 || (multishot && index != 2)
                    ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
            int rank = Math.max(1, player.getData(RpgData.SKILL_LOADOUT).rank(skill.id()));
            float yaw = player.getYRot() + (multishot ? (index - 2) * 7.5f : 0);
            double rankSpeed = skill == SkillDefinition.PIERCING_SHOT || skill == SkillDefinition.AIMED_SHOT ? 1 + .02 * (rank - 1) : 1;
            arrow.shootFromRotation(player, player.getXRot(), yaw, 0, (float) (3 * rankSpeed), 0);
            if (multishot || skill == SkillDefinition.ARCANE_ARROW) {
                arrow.setData(RpgData.STALKER_SHOT, new StalkerArrows.Shot(
                        skill == SkillDefinition.ARCANE_ARROW ? Combos.duration(player, Vulnerability.DURATION_TICKS) : 0,
                        fan, multishot ? ClassResources.now(player) + StalkerArrows.FAN_LIFETIME_TICKS : 0));
            }
            ProjectileCombat.captureSupport(arrow, player);
            SkillVisuals.markArrow(arrow, skill);
            ProjectileCombat.markSkill(arrow);
            Projectile.spawnProjectile(arrow, player.level(), ammo.copyWithCount(1));
        }
        ammo.shrink(shot.ammunitionCost());
        bow.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        if (skill != SkillDefinition.AIMED_SHOT) { SkillVisuals.bowDraw(player, SkillVisuals.BOW_DRAW_TICKS); }
        SkillSounds.release(player, skill);
    }
}
