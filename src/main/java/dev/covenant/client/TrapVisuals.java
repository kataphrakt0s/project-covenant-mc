package dev.covenant.client;

import dev.covenant.rpg.skills.TrapEntity;
import dev.covenant.rpg.skills.TrapKind;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Sparse supported frost fractures/embers; native particle settings never suppress essential boundaries. */
final class TrapVisuals {
    private static final DustParticleOptions ICE = new DustParticleOptions(0xC2F4FF, .55f);

    private TrapVisuals() {}

    static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof TrapEntity trap) || !trap.level().isClientSide()
                || trap.tickCount % 6 != 0) { return; }
        if (trap.phase() == TrapEntity.Phase.ARMED) {
            if (trap.kind() == TrapKind.FROST || trap.kind() == TrapKind.FLAME) {
                trap.level().addParticle(trap.kind() == TrapKind.FROST ? ParticleTypes.SNOWFLAKE : ParticleTypes.SMALL_FLAME,
                        trap.getX(), trap.getY() + .32, trap.getZ(), 0, .01, 0);
            }
            // Bear stays mechanical; Blast has a solid unlit primer and no countdown decoration.
        } else if (trap.phase() == TrapEntity.Phase.TRIGGERED
                && (trap.kind() == TrapKind.FROST || trap.kind() == TrapKind.FLAME)) {
            // Cycle between six angular interior marks rather than filling the patch with a mist.
            int mark = (trap.tickCount / 6) % 6;
            double angle = Math.PI * 2 * mark / 6;
            double radius = trap.kind().radius * .58;
            var offset = new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            if (!GroundMarks.supported(trap, offset, true)) { return; }
            if (trap.kind() == TrapKind.FLAME) {
                trap.level().addParticle(ParticleTypes.SMALL_FLAME,
                        trap.getX() + offset.x, trap.getY() + .045, trap.getZ() + offset.z, 0, .008, 0);
                return;
            }
            for (int i = -1; i <= 1; i++) {
                double x = offset.x + i * .07;
                double z = offset.z + Math.abs(i) * .07;
                trap.level().addParticle(ICE, trap.getX() + x, trap.getY() + .045, trap.getZ() + z, 0, 0, 0);
            }
        }
    }
}
