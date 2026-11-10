package dev.covenant.client;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.skills.Auras;
import dev.covenant.rpg.skills.SkillDefinition;
import dev.covenant.rpg.skills.SkillLoadout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.profiling.Profiler;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** One local owner's shared aura boundary, made from a bounded set of native dust particles. */
public final class AuraRangeVisuals {
    /** Normal/decreased particle budgets; Minimal follows vanilla by suppressing optional dust. */
    private static final int NORMAL_COUNT = 48, DECREASED_COUNT = 32;
    private static final DustParticleOptions EDGE = new DustParticleOptions(0xBACDD8, .55f);
    private static final Auras.Kind[] KINDS = Auras.Kind.values();
    private static final double[][] NORMAL_DIRECTIONS = directions(NORMAL_COUNT);
    private static final double[][] DECREASED_DIRECTIONS = directions(DECREASED_COUNT);
    private static final Particle[] particles = new Particle[NORMAL_COUNT];
    private static final long[] renewAt = new long[NORMAL_COUNT];
    private static LocalPlayer owner;
    private static ClientLevel viewLevel;
    private static SkillLoadout loadout;
    private static PlayerClass playerClass;
    private static double radius;
    private static double lastX, lastY, lastZ;
    private static int count;
    private static long ticks;

    private AuraRangeVisuals() {}

    /** Register on the game event bus; this does no server work and generates no network traffic. */
    public static void tick(ClientTickEvent.Post event) {
        try (var zone = Profiler.get().zone("covenant:aura_range_visuals")) {
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;
            if (player == null || minecraft.level == null || !player.isAlive() || player.isSpectator()
                    || minecraft.options.particles().get() == ParticleStatus.MINIMAL) {
                clear();
                return;
            }
            if (owner != player || viewLevel != minecraft.level) {
                clear();
                owner = player;
                viewLevel = minecraft.level;
            }
            var nextLoadout = player.getData(RpgData.SKILL_LOADOUT);
            var nextClass = PlayerProfiles.getSelectedClass(player).orElse(null);
            if (loadout != nextLoadout || playerClass != nextClass) {
                loadout = nextLoadout;
                playerClass = nextClass;
                radius = 0;
                for (var kind : KINDS) {
                    double candidate = kind.passiveRadius();
                    if (playerClass != null && candidate > 0 && loadout.learned().contains(kind.id) && loadout.bindings().containsValue(kind.id)
                            && SkillDefinition.find(kind.id).orElseThrow().allowedClasses().contains(playerClass)) {
                        // These passives share one field; duplicate slots and overlapping auras never stack outlines.
                        radius = Math.max(radius, candidate);
                    }
                }
            }
            if (radius == 0) {
                removeParticles();
                return;
            }
            if (minecraft.isPaused()) { return; }
            int nextCount = minecraft.options.particles().get() == ParticleStatus.DECREASED ? DECREASED_COUNT : NORMAL_COUNT;
            if (count != nextCount) {
                removeParticles();
                count = nextCount;
            }
            double x = player.getX(), y = player.getY() + .08, z = player.getZ();
            // An instantaneous relocation must not interpolate a ring across the old and new destinations.
            double dx = x - lastX, dy = y - lastY, dz = z - lastZ;
            if (dx * dx + dy * dy + dz * dz > 16) {
                removeParticles();
                count = nextCount;
            }
            boolean moved = x != lastX || y != lastY || z != lastZ;
            var directions = count == NORMAL_COUNT ? NORMAL_DIRECTIONS : DECREASED_DIRECTIONS;
            ticks++;
            for (int i = 0; i < count; i++) {
                double px = x + directions[i][0] * radius;
                double pz = z + directions[i][1] * radius;
                var particle = particles[i];
                if (particle == null || !particle.isAlive() || ticks >= renewAt[i]) {
                    if (particle != null) { particle.remove(); }
                    particle = minecraft.particleEngine.createParticle(EDGE, px, y, pz, 0, 0, 0);
                    particles[i] = particle;
                    if (particle == null) { continue; }
                    // Native dust otherwise has random initial movement. Keep boundary points fixed to the owner.
                    particle.setParticleSpeed(0, 0, 0);
                    int lifetime = 20 + i % 8;
                    particle.setLifetime(lifetime);
                    renewAt[i] = ticks + lifetime;
                } else if (moved) {
                    particle.setPos(px, y, pz);
                }
            }
            lastX = x; lastY = y; lastZ = z;
        }
    }

    private static double[][] directions(int samples) {
        var result = new double[samples][2];
        for (int i = 0; i < samples; i++) {
            double angle = Math.PI * 2 * i / samples;
            result[i][0] = Math.cos(angle);
            result[i][1] = Math.sin(angle);
        }
        return result;
    }

    private static void removeParticles() {
        for (int i = 0; i < particles.length; i++) {
            if (particles[i] != null) { particles[i].remove(); particles[i] = null; }
        }
        count = 0;
    }

    private static void clear() {
        removeParticles();
        owner = null; viewLevel = null; loadout = null; playerClass = null; radius = 0; ticks = 0;
    }
}
