package dev.covenant.client;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.skills.Chilled;
import dev.covenant.rpg.skills.Rooted;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Quiet target-local recognition from existing synced control state; no entity scans or packets. */
final class ControlVisuals {
    private static final DustParticleOptions IRON = new DustParticleOptions(0xAFB9B4, .45f);
    private static final DustParticleOptions ICE = new DustParticleOptions(0xC2F4FF, .5f);

    private ControlVisuals() {}

    static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target) || !target.level().isClientSide()
                || !target.isAlive() || target.isInvisible() || (target.tickCount + target.getId()) % 12 != 0) { return; }
        if (target.hasData(RpgData.AURA_FROZEN) && target.getData(RpgData.AURA_FROZEN)) {
            double r = Math.min(.65, target.getBbWidth() * .45);
            for (int i = 0; i < 3; i++) {
                double angle = i * Math.PI * 2 / 3;
                target.level().addParticle(ICE, target.getX() + Math.cos(angle) * r,
                        target.getY() + .15 + i * .08, target.getZ() + Math.sin(angle) * r, 0, 0, 0);
            }
        } else if (Rooted.contains(target)) {
            double r = Math.min(.65, target.getBbWidth() * .45);
            for (int side = -1; side <= 1; side += 2) {
                target.level().addParticle(IRON, target.getX() + side * r,
                        target.getY() + .12, target.getZ(), 0, 0, 0);
            }
        } else if (target.hasEffect(Chilled.EFFECT)) {
            target.level().addParticle(ParticleTypes.SNOWFLAKE, target.getX(),
                    target.getY() + Math.min(.5, target.getBbHeight() * .3), target.getZ(), 0, .005, 0);
        }
    }
}
