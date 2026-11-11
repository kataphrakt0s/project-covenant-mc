package dev.covenant.client;

import dev.covenant.rpg.skills.LightningVisual;
import dev.covenant.rpg.skills.CombatParticles;
import net.minecraft.util.profiling.Profiler;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** One bounded jagged stroke per authoritative link, using the existing dimension-scoped endpoints. */
final class ClientSkillVisuals {
    private ClientSkillVisuals() {}

    static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(LightningVisual.TYPE, (beam, context) -> {
            try (var zone = Profiler.get().zone("covenant:lightning_visuals")) {
                var level = context.player().level();
                if (!level.dimension().identifier().equals(beam.dimension())) { return; }
                var points = LightningPath.points(new LightningPath.Point(beam.start().x, beam.start().y, beam.start().z),
                        new LightningPath.Point(beam.end().x, beam.end().y, beam.end().z));
                for (int i = 1; i < points.size(); i++) {
                    var a = points.get(i - 1); var b = points.get(i);
                    if (a.equals(b)) { continue; }
                    level.addParticle(CombatParticles.LIGHTNING_THREAD.get(),
                            (a.x() + b.x()) * .5, (a.y() + b.y()) * .5, (a.z() + b.z()) * .5,
                            b.x() - a.x(), b.y() - a.y(), b.z() - a.z());
                    // At most sixteen sparks per link, attached to the real path rather than new branches.
                    if (i % 4 == 0) {
                        level.addParticle(CombatParticles.ELECTRIC_ENERGY.get(), a.x(), a.y(), a.z(),
                                (level.getRandom().nextDouble() - .5) * .1,
                                (level.getRandom().nextDouble() - .5) * .1,
                                (level.getRandom().nextDouble() - .5) * .1);
                    }
                }
            }
        });
    }
}
