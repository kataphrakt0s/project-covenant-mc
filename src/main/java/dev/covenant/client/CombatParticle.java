package dev.covenant.client;

import dev.covenant.rpg.skills.CombatParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Matrix3f;
import net.minecraft.world.phys.AABB;

/** Short original sprite sequences, with world-oriented Cleave and motion-aligned arrow streaks. */
final class CombatParticle extends SimpleAnimatedParticle {
    private final Mode mode;
    private final double a, b, c;

    private enum Mode { MOTE, COLLISION, CONTACT, STREAK, AIMED, VOLLEY, REND, CLEAVE, EMBER, SHARD, BEAM }

    private CombatParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                           Mode mode, double a, double b, double c) {
        super(level, x, y, z, sprites, 0);
        this.mode = mode; this.a = a; this.b = b; this.c = c;
        hasPhysics = false;
        lifetime = switch (mode) {
            case STREAK, VOLLEY -> 2;
            case AIMED -> 3;
            case CLEAVE, REND -> 4;
            case MOTE -> 5;
            case COLLISION, CONTACT -> 6;
            case EMBER, SHARD -> 6;
            case BEAM -> 4;
        };
        quadSize = switch (mode) {
            case MOTE -> .075f;
            case COLLISION -> .19f;
            case CONTACT -> .17f;
            case STREAK -> .24f;
            case AIMED -> .3f;
            case VOLLEY -> .16f;
            case REND -> .22f;
            // Native single-particle auxiliary values encode yaw (radians) and range (blocks).
            case CLEAVE -> (float) Math.clamp(b, 0, 6);
            case EMBER, SHARD -> .085f;
            case BEAM -> (float) Math.min(1, Math.sqrt(a * a + b * b + c * c) * .5);
        };
        setSize(quadSize * 2, mode == Mode.CLEAVE ? .2f : quadSize * 2);
        if (mode == Mode.BEAM) {
            setBoundingBox(new AABB(x - quadSize, y - quadSize, z - quadSize, x + quadSize, y + quadSize, z + quadSize));
        }
        setSprite(sprites.get(0, lifetime - 1));
    }

    static void register(RegisterParticleProvidersEvent event) {
        register(event, CombatParticles.ARCANE_MOTE.get(), Mode.MOTE);
        register(event, CombatParticles.ARCANE_COLLISION.get(), Mode.COLLISION);
        register(event, CombatParticles.CONTACT.get(), Mode.CONTACT);
        register(event, CombatParticles.PIERCING_STREAK.get(), Mode.STREAK);
        register(event, CombatParticles.CLEAVE.get(), Mode.CLEAVE);
        register(event, CombatParticles.EMBER.get(), Mode.EMBER);
        register(event, CombatParticles.ICE_SHARD.get(), Mode.SHARD);
        register(event, CombatParticles.LIGHTNING_MOTE.get(), Mode.MOTE);
        register(event, CombatParticles.LIGHTNING_THREAD.get(), Mode.BEAM);
        register(event, CombatParticles.AIMED_STREAK.get(), Mode.AIMED);
        register(event, CombatParticles.ARCANE_STREAK.get(), Mode.STREAK);
        register(event, CombatParticles.VOLLEY_STREAK.get(), Mode.VOLLEY);
        register(event, CombatParticles.AIMED_GATHER.get(), Mode.MOTE);
        register(event, CombatParticles.ARCANE_CONTACT.get(), Mode.CONTACT);
        register(event, CombatParticles.REND_CONTACT.get(), Mode.REND);
        register(event, CombatParticles.BLEED_TICK.get(), Mode.MOTE);
    }

    private static void register(RegisterParticleProvidersEvent event, net.minecraft.core.particles.SimpleParticleType type, Mode mode) {
        event.registerSpriteSet(type, sprites -> (options, level, x, y, z, a, b, c, random) ->
                new CombatParticle(level, x, y, z, sprites, mode, a, b, c));
    }

    @Override public void tick() {
        // These marks stay at their real release/contact/path point; no drifting or collision simulation.
        if (++age >= lifetime) { remove(); return; }
        // Include the final breakup frame before removal, even with these very short lifetimes.
        setSprite(sprites.get(age, lifetime - 1));
        alpha = 1 - (float) age / lifetime;
        if (mode == Mode.BEAM) { alpha *= age % 2 == 0 ? 1 : .45f; }
        if (mode == Mode.EMBER) {
            xo = x; yo = y; zo = z;
            move(a, b, c);
        }
    }

    @Override public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
        if (mode == Mode.CLEAVE) {
            var rotation = new Quaternionf().rotationY((float) -a).rotateX((float) (Math.PI / 2));
            extractRotatedQuad(state, camera, rotation, partialTick);
            // Native particle pipelines cull backfaces. Flip local X, retaining the same forward
            // direction, so the horizontal crescent is visible from above and below.
            extractRotatedQuad(state, camera, rotation.rotateY((float) Math.PI), partialTick);
        } else if (mode == Mode.BEAM) {
            // Each square carries a narrow straight strip: its Y axis is the actual 3D segment,
            // and its normal faces the viewer. Perspective cannot bend its endpoints off the path.
            var along = new Vector3f((float) a, (float) b, (float) c);
            if (along.lengthSquared() < .000001f) { return; }
            along.normalize();
            var view = new Vector3f((float) (camera.position().x - x), (float) (camera.position().y - y), (float) (camera.position().z - z));
            var across = along.cross(view, new Vector3f());
            if (across.lengthSquared() < .000001f) {
                across = along.cross(Math.abs(along.y) > .9 ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0), across);
            }
            across.normalize();
            var normal = across.cross(along, new Vector3f());
            var rotation = new Quaternionf().setFromNormalized(new Matrix3f().setColumn(0, across).setColumn(1, along).setColumn(2, normal));
            extractRotatedQuad(state, camera, rotation, partialTick);
        } else if (mode == Mode.STREAK || mode == Mode.SHARD || mode == Mode.AIMED || mode == Mode.VOLLEY) {
            var local = new Vector3f((float) a, (float) b, (float) c).rotate(new Quaternionf(camera.rotation()).conjugate());
            // A shot toward the camera stays a small mark rather than widening across the aim point.
            float angle = (float) Math.atan2(-local.x, local.y);
            extractRotatedQuad(state, camera, new Quaternionf(camera.rotation()).rotateZ(angle), partialTick);
        } else { super.extract(state, camera, partialTick); }
    }
}
