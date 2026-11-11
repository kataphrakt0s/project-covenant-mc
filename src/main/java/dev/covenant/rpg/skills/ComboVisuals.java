package dev.covenant.rpg.skills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Stage-specific presentation on the existing authoritative combo execution path. */
final class ComboVisuals {
    private ComboVisuals() {}

    static void strike(ServerPlayer player) {
        var aim = player.getLookAngle();
        var hand = player.getEyePosition().add(aim.scale(.35)).add(0, -.35, 0);
        player.level().sendParticles(CombatParticles.LUNGE_STRIKE.get(), hand.x, hand.y, hand.z,
                0, aim.x, aim.y, aim.z, 1);
    }

    static void sweep(ServerPlayer player, boolean finisher) {
        player.level().sendParticles((finisher ? CombatParticles.RUSH_FINISHER : CombatParticles.RIP_SWEEP).get(),
                player.getX(), player.getY() + .65, player.getZ(), 0, Math.toRadians(player.getYRot()), 3, 0, 1);
    }

    /** Emit only the real clipped segment, never the requested velocity or a predicted endpoint. */
    static void trail(ServerPlayer player, Combos.Kind kind, AABB body, Vec3 travelled) {
        if (travelled.lengthSqr() < 1e-8) { return; }
        player.level().sendParticles((kind == Combos.Kind.LUNGING ? CombatParticles.LUNGE_TRAIL : CombatParticles.RUSH_TRAIL).get(),
                (body.minX + body.maxX) * .5, body.minY + .3, (body.minZ + body.maxZ) * .5,
                0, travelled.x, travelled.y, travelled.z, 1);
    }

    /** Damage confirmation only; native immunity can independently reject armor shred. */
    static void contact(ServerPlayer player, LivingEntity target) {
        player.level().sendParticles(CombatParticles.COMBO_CONTACT.get(), target.getX(), target.getY(.5), target.getZ(), 1, 0, 0, 0, 0);
    }

    /** Same qualifying bleed branch as the finisher reward, including lethal hits with pre-hit bleed. */
    static void consume(ServerPlayer player, LivingEntity target) {
        player.level().sendParticles(CombatParticles.COMBO_CONSUME.get(), target.getX(), target.getY(.5), target.getZ(), 1, 0, 0, 0, 0);
    }
}
