package dev.covenant.client;

import dev.covenant.rpg.skills.LightningVisual;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.profiling.Profiler;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Client-only beam sampling keeps particle count bounded without one server packet per spark. */
final class ClientSkillVisuals {
    private ClientSkillVisuals() {}

    static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(LightningVisual.TYPE, (beam, context) -> {
            try (var zone = Profiler.get().zone("covenant:lightning_visuals")) {
                var level = context.player().level();
                if (!level.dimension().identifier().equals(beam.dimension())) { return; }
                var delta = beam.end().subtract(beam.start());
                int count = Math.min(96, Math.max(1, (int) Math.ceil(delta.length() * 4)));
                for (int i = 0; i <= count; i++) {
                    var pos = beam.start().add(delta.scale((double) i / count));
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                            pos.x + level.getRandom().nextGaussian() * .025,
                            pos.y + level.getRandom().nextGaussian() * .025,
                            pos.z + level.getRandom().nextGaussian() * .025, 0, 0, 0);
                }
            }
        });
    }
}
