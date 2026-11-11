package dev.covenant.rpg.skills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** One-shot elemental receipts from committed charges/reserves, separate from real hit/chain feedback. */
final class WeaverVisuals {
    private WeaverVisuals() {}

    static void dischargeCharge(ServerPlayer player) {
        if (player.tickCount % 4 != 0) { return; }
        var hand = player.getEyePosition().add(player.getLookAngle().scale(.6)).add(0, -.3, 0);
        for (var element : WeaverPower.Element.values()) {
            if (WeaverPower.charges(player, element) == 0) { continue; }
            var type = switch (element) {
                case FIRE -> CombatParticles.FIRE_ENERGY.get();
                case ICE -> CombatParticles.ICE_SHARD.get();
                case LIGHTNING -> CombatParticles.ELECTRIC_ENERGY.get();
            };
            player.level().sendParticles(type, hand.x + (element.ordinal() - 1) * .1, hand.y, hand.z, 1, 0, 0, 0, 0);
        }
    }

    static void discharge(ServerPlayer player, int[] consumed, WeaverSkillData.Discharge config) {
        for (int e = 0; e < 3; e++) {
            if (consumed[e] == 0) { continue; }
            var type = switch (e) {
                case 0 -> CombatParticles.DISCHARGE_FIRE.get();
                case 1 -> CombatParticles.DISCHARGE_ICE.get();
                default -> CombatParticles.DISCHARGE_LIGHTNING.get();
            };
            // Radius, normalized consumed bank and full-charge rider flag, carried by native count-zero data.
            player.level().sendParticles(type, player.getX(), player.getY() + .5, player.getZ(),
                    0, config.radius(), (double) consumed[e] / config.stacks(), consumed[e] == config.stacks() ? 1 : 0, 1);
        }
    }

    static void contact(ServerPlayer player, LivingEntity target) {
        player.level().sendParticles(CombatParticles.CONTACT.get(), target.getX(), target.getY(.5), target.getZ(), 1, 0, 0, 0, 0);
    }

    static void threefold(ServerPlayer player, double[] consumed) {
        player.level().sendParticles(CombatParticles.THREEFOLD_CONSUME.get(), player.getX(), player.getY() + .75, player.getZ(),
                0, consumed[0], consumed[1], consumed[2], 1);
    }

    static void expire(ServerPlayer player, int elements) {
        if (elements == 0) { return; }
        player.level().sendParticles(CombatParticles.THREEFOLD_EXPIRE.get(), player.getX(), player.getY() + .6, player.getZ(),
                0, elements, 0, 0, 1);
    }
}
