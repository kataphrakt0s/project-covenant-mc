package dev.covenant.rpg.skills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Presentation at accepted Reaver outcomes; passive refresh remains quiet. */
final class ReaverVisuals {
    private ReaverVisuals() {}

    static void shout(ServerPlayer source, double radius) {
        source.level().sendParticles(CombatParticles.BATTLE_CRY_PULSE.get(), source.getX(), source.getY() + .3, source.getZ(),
                0, radius, 0, 0, 1);
    }

    static void received(ServerPlayer source, LivingEntity target) {
        source.level().sendParticles(CombatParticles.BATTLE_CRY_RECEIVED.get(), target.getX(), target.getY(.6), target.getZ(), 1, 0, 0, 0, 0);
    }

    static void stormCharge(ServerPlayer player) {
        if (player.tickCount % 2 != 0) { return; }
        var hand = player.getEyePosition().add(player.getLookAngle().scale(.6)).add(0, -.25, 0);
        player.level().sendParticles(CombatParticles.COMBO_CRIMSON.get(), hand.x - .09, hand.y, hand.z, 0, .025, 0, 0, 1);
        player.level().sendParticles(CombatParticles.COMBO_STEEL.get(), hand.x + .09, hand.y, hand.z, 0, -.025, 0, 0, 1);
    }

    static void stormEnd(RageStorm storm) {
        if (storm.level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.sendParticles(CombatParticles.STORM_DISSIPATE.get(), storm.getX(), storm.getY(), storm.getZ(),
                    0, storm.visualRadius(), 0, 0, 1);
        }
    }
}
