package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import net.minecraft.server.level.ServerPlayer;

/** Explicit spell-family execution. No second cost, timer, or validation path. */
final class SpellSkills {
    private SpellSkills() {}

    static boolean isSpell(SkillDefinition skill) {
        return switch (skill.effect()) {
            case ARCANE_BOLT, ELEMENTAL_BOLT, LIGHTNING_BOLT, DISCHARGE, TELEPORT, FIRESTORM -> true;
            default -> false;
        };
    }

    static void execute(ServerPlayer player, SkillDefinition skill) {
        SkillSounds.release(player, skill);
        switch (skill.effect()) {
            case LIGHTNING_BOLT -> ElementalBolts.lightning(player);
            case ELEMENTAL_BOLT -> {
                var bolt = new ElementalBolt(skill == SkillDefinition.FIRE_BOLT ? Skills.FIRE_BOLT.get() : Skills.ICE_BOLT.get(), player.level());
                bolt.setOwner(player);
                ProjectileCombat.captureSupport(bolt, player);
                bolt.initialize(ElementalBolts.duration(player, skill));
                bolt.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
                bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5f, 0);
                player.level().addFreshEntity(bolt);
            }
            case ARCANE_BOLT -> {
                var bolt = new ArcaneBolt(Skills.ARCANE_BOLT.get(), player.level());
                bolt.setOwner(player);
                ProjectileCombat.captureSupport(bolt, player);
                bolt.setData(RpgData.AURA_PROJECTILE_POWER, AuraDamage.multiplier(player, false) + ElementalGolems.spellBonus(player));
                bolt.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
                bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5f, 0);
                player.level().addFreshEntity(bolt);
            }
            default -> throw new IllegalArgumentException("Not a spell: " + skill.id());
        }
    }
}
