package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Bounded native-particle effects. No client classes, gameplay state, or independent damage pipeline. */
public final class SkillVisuals {
    /** Cosmetic bow-pose duration: six game ticks (0.3 seconds at 20 TPS), with no firing delay. */
    public static final int BOW_DRAW_TICKS = 6;
    private static final DustParticleOptions YELLOW = new DustParticleOptions(0xFFF0A0, 0.6f);
    private static final DustParticleOptions RED = new DustParticleOptions(0xE34336, 0.8f);
    private static final DustParticleOptions FIRE = new DustParticleOptions(0xC12748, 0.8f);
    private static final DustParticleOptions ICE = new DustParticleOptions(0xC2F4FF, 0.65f);
    private static final int PIERCING_STYLE = 0;
    private static final int AIMED_STYLE = 1;
    private static final int ARCANE_STYLE = 2;
    private static final int MULTISHOT_STYLE = 3;
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> LIGHTNING_DAMAGE =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, SkillDefinition.LIGHTNING_BOLT.id());

    private SkillVisuals() {}

    /** Two local decorative motes every eight entity ticks; native limiting applies, with no server packets. */
    static void golemAmbient(ElementalGolem golem) {
        if (!golem.level().isClientSide() || !golem.isAlive() || golem.isInvisible()
                || (golem.tickCount & 7) != (golem.getId() & 7)) { return; }
        var particle = switch (golem.form()) {
            case FIRE -> CombatParticles.EMBER.get();
            case ICE -> CombatParticles.ICE_SHARD.get();
            case LIGHTNING -> CombatParticles.ELECTRIC_ENERGY.get();
            case ARCANE -> CombatParticles.ARCANE_ENERGY.get();
        };
        double yaw = Math.toRadians(golem.yBodyRot);
        double forwardX = -Math.sin(yaw), forwardZ = Math.cos(yaw);
        for (int i = 0; i < 2; i++) {
            double side = (golem.getRandom().nextDouble() - .5) * .3;
            golem.level().addParticle(particle, golem.getX() + forwardX * .32 + forwardZ * side,
                    golem.getY() + 1.8125 + (golem.getRandom().nextDouble() - .5) * .2,
                    golem.getZ() + forwardZ * .32 - forwardX * side,
                    forwardX * .008, golem.form() == ElementalGolem.Form.FIRE ? .018 : -.004, forwardZ * .008);
        }
    }

    static void elementalTrail(ElementalBolt bolt) {
        boolean fire = bolt.skill() == SkillDefinition.FIRE_BOLT;
        energyTrail(bolt, fire ? CombatParticles.FIRE_ENERGY.get() : CombatParticles.ICE_ENERGY.get(), fire);
        if (bolt.tickCount % 2 != 0) { return; }
        var delta = bolt.position().subtract(bolt.xo, bolt.yo, bolt.zo);
        if (delta.lengthSqr() < .0001 || delta.lengthSqr() > 36) { return; }
        for (int i = 0; i < 2; i++) {
            var pos = bolt.position().subtract(delta.scale(i * .5));
            bolt.level().addParticle(fire ? CombatParticles.EMBER.get() : CombatParticles.ICE_SHARD.get(),
                    pos.x, pos.y, pos.z, fire ? 0 : delta.x, fire ? .018 : delta.y, fire ? 0 : delta.z);
        }
    }

    /** Distance-spaced local emission, capped at fifteen energy particles per projectile tick. */
    private static void energyTrail(Entity bolt, net.minecraft.core.particles.SimpleParticleType particle, boolean fire) {
        var delta = bolt.position().subtract(bolt.xo, bolt.yo, bolt.zo);
        double distance = delta.length();
        if (distance < .01 || distance > 6) { return; }
        int samples = Math.min(10, Math.max(1, (int) Math.ceil(distance / .18)));
        var forward = delta.scale(1 / distance);
        var right = forward.cross(Math.abs(forward.y) > .9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        var up = right.cross(forward);
        var flow = forward.scale(.07);
        for (int i = 0; i < samples; i++) {
            double t = i / (double) samples;
            var pos = bolt.position().subtract(delta.scale(t));
            bolt.level().addParticle(particle, pos.x, pos.y, pos.z, flow.x, flow.y, flow.z);
            if (i % 2 == 0) {
                double angle = (bolt.tickCount - t) * (fire ? 2.3 : 1.4) + bolt.getId();
                var outward = right.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                var wisp = pos.add(outward.scale(fire ? .11 : .075));
                var drift = flow.add(outward.scale(fire ? .04 : .025));
                bolt.level().addParticle(particle, wisp.x, wisp.y, wisp.z, drift.x, drift.y, drift.z);
            }
        }
    }

    static void elementalImpact(ServerLevel level, Vec3 pos, SkillDefinition skill) {
        boolean fire = skill == SkillDefinition.FIRE_BOLT;
        level.sendParticles(fire ? CombatParticles.FIRE_COLLISION.get() : CombatParticles.ICE_COLLISION.get(),
                pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }

    static void lightningImpact(ServerLevel level, Vec3 pos) {
        level.sendParticles(CombatParticles.LIGHTNING_COLLISION.get(), pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }

    static void lightning(ServerLevel level, Vec3 start, Vec3 end) {
        var delta = end.subtract(start);
        var visual = new LightningVisual(level.dimension().identifier(), start, end);
        for (var player : level.players()) {
            double t = delta.lengthSqr() == 0 ? 0 : Math.clamp(player.position().subtract(start).dot(delta) / delta.lengthSqr(), 0, 1);
            if (player.position().distanceToSqr(start.add(delta.scale(t))) <= 32 * 32 && player.connection.hasChannel(LightningVisual.TYPE)) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, visual);
            }
        }
    }

    static void trapSnap(ServerLevel level, TrapEntity trap) {
        var particle = switch (trap.kind()) {
            case BEAR -> ParticleTypes.CRIT;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case FLAME -> CombatParticles.EMBER.get();
            case BLAST -> ParticleTypes.EXPLOSION;
        };
        level.sendParticles(particle,
                trap.getX(), trap.getY() + 0.2, trap.getZ(), trap.kind() == TrapKind.BLAST ? 1 : 8,
                0.15, 0.1, 0.15, 0.01);
        if (trap.kind() == TrapKind.FROST) {
            level.sendParticles(ICE, trap.getX(), trap.getY() + .2, trap.getZ(), 12, .25, .1, .25, .04);
        } else if (trap.kind() == TrapKind.BEAR) {
            // Opposing inward contact streaks reinforce the mechanical snap, never imply a lasting area.
            for (int side = -1; side <= 1; side += 2) {
                level.sendParticles(ParticleTypes.CRIT, trap.getX(), trap.getY() + .15,
                        trap.getZ() + side * .3, 0, 0, 0, -side, .12);
            }
        } else if (trap.kind() == TrapKind.FLAME) {
            level.sendParticles(FIRE, trap.getX(), trap.getY() + .15, trap.getZ(), 6, .2, .05, .2, .02);
        } else if (trap.kind() == TrapKind.BLAST) {
            // Short outward pressure/debris, without TNT fuse smoke or a persistent damage ring.
            level.sendParticles(ParticleTypes.POOF, trap.getX(), trap.getY() + .15, trap.getZ(), 8, .2, .08, .2, .08);
        }
    }

    static void frostExpiry(TrapEntity trap) {
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            trap.level().addParticle(ParticleTypes.SNOWFLAKE, trap.getX() + Math.cos(angle) * .35,
                    trap.getY() + .08, trap.getZ() + Math.sin(angle) * .35, 0, .015, 0);
        }
    }

    static void flameExpiry(TrapEntity trap) {
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 2 * i / 4;
            trap.level().addParticle(ParticleTypes.SMOKE, trap.getX() + Math.cos(angle) * .2,
                    trap.getY() + .08, trap.getZ() + Math.sin(angle) * .2, 0, .01, 0);
        }
    }

    static void bowDraw(ServerPlayer player, long ticks) {
        player.setData(RpgData.BOW_DRAW_DURATION, ticks);
        player.setData(RpgData.BOW_DRAW_UNTIL, player.level().getGameTime() + ticks);
    }

    static void charge(ServerPlayer player, SkillDefinition skill) {
        if (skill.effect() == SkillDefinition.Effect.DISCHARGE) { WeaverVisuals.dischargeCharge(player); return; }
        if (player.tickCount % 2 != 0) { return; }
        var hand = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.25, 0);
        if (skill == SkillDefinition.ARCANE_BOLT) {
            // Two opposite corners gather at the hand during the real cast, with no persistent cast object.
            double angle = player.tickCount * Math.PI / 8;
            for (int side = -1; side <= 1; side += 2) {
                var pos = hand.add(Math.cos(angle) * .1 * side, Math.sin(angle) * .1 * side, 0);
                player.level().sendParticles(CombatParticles.ARCANE_MOTE.get(), pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
            return;
        }
        var mote = skill.id().equals(SkillDefinition.FIRE_BOLT.id()) || skill.effect() == SkillDefinition.Effect.FIRESTORM ? CombatParticles.EMBER.get()
                : skill == SkillDefinition.ICE_BOLT ? CombatParticles.ICE_SHARD.get() : CombatParticles.LIGHTNING_MOTE.get();
        // Native count-zero packets preserve gentle rise or shard orientation without random velocity.
        player.level().sendParticles(mote, hand.x, hand.y, hand.z, 0, 0, skill.id().equals(SkillDefinition.FIRE_BOLT.id()) || skill.effect() == SkillDefinition.Effect.FIRESTORM ? .018 : 0, 0, 1);
    }

    static void rageCharge(ServerPlayer player) {
        ReaverVisuals.stormCharge(player);
    }

    static void aimedCharge(ServerPlayer player) {
        if (player.tickCount % 4 != 0) { return; }
        var hand = player.getEyePosition().add(player.getLookAngle().scale(.6)).add(0, -.25, 0);
        player.level().sendParticles(CombatParticles.AIMED_GATHER.get(), hand.x, hand.y, hand.z, 1, 0, 0, 0, 0);
    }

    static void cleave(ServerPlayer player, double range) {
        // A single native particle packet carries the actual yaw/range, not one packet per arc sample.
        // Count zero preserves these auxiliary values (as with vanilla's sweep-size parameter).
        player.level().sendParticles(CombatParticles.CLEAVE.get(), player.getX(), player.getY() + .8, player.getZ(),
                0, Math.toRadians(player.getYRot()), range, 0, 1);
    }

    static void cleaveHit(ServerLevel level, LivingEntity target) {
        level.sendParticles(CombatParticles.CONTACT.get(), target.getX(), target.getY(.5), target.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(RED, target.getX(), target.getY(.5), target.getZ(), 3, .12, .15, .12, 0);
    }

    static void bleedTick(ServerLevel level, LivingEntity target) {
        level.sendParticles(CombatParticles.BLEED_TICK.get(), target.getX(), target.getY(.5), target.getZ(), 1, 0, 0, 0, 0);
    }

    static void rendHit(ServerLevel level, LivingEntity target) {
        level.sendParticles(CombatParticles.REND_CONTACT.get(), target.getX(), target.getY(.5), target.getZ(), 1, 0, 0, 0, 0);
    }

    static void boltImpact(ServerLevel level, Vec3 position) {
        // Open broken seal is collision feedback, including walls and undamaged/immune entities.
        level.sendParticles(CombatParticles.ARCANE_COLLISION.get(), position.x, position.y, position.z, 1, 0, 0, 0, 0);
    }

    /** Called only on clients. A tracked projectile supplies its own lifecycle; no recurring particle packets. */
    static void boltTrail(Entity bolt) {
        energyTrail(bolt, CombatParticles.ARCANE_ENERGY.get(), false);
    }

    /** Cosmetic style is independent of saved projectile provenance and supplied before initial tracking. */
    static void markArrow(AbstractArrow arrow, SkillDefinition skill) {
        int style = skill == SkillDefinition.ARCANE_ARROW ? ARCANE_STYLE
                : skill == SkillDefinition.AIMED_SHOT ? AIMED_STYLE
                : skill == SkillDefinition.MULTISHOT || skill.effect() == SkillDefinition.Effect.CHANNEL ? MULTISHOT_STYLE : PIERCING_STYLE;
        arrow.setData(RpgData.SKILL_SHOT_STYLE, style);
        arrow.setData(RpgData.SKILL_SHOT, true);
    }

    /** Client entity callback checks only marked arrows, without scanning the world's entities. */
    public static void arrowTick(EntityTickEvent.Post event) {
        try (var zone = Profiler.get().zone("covenant:arrow_visuals")) {
            if (event.getEntity() instanceof AbstractArrow arrow && arrow.level().isClientSide()
                    && !arrow.isRemoved() && arrow.tickCount <= 100
                    && arrow.hasData(RpgData.SKILL_SHOT) && arrow.getData(RpgData.SKILL_SHOT)) {
                int style = arrow.getData(RpgData.SKILL_SHOT_STYLE);
                var delta = arrow.position().subtract(arrow.xo, arrow.yo, arrow.zo);
                if (delta.lengthSqr() < .0001 || delta.lengthSqr() > 36) { return; }
                // One small short-lived streak per fan arrow, versus denser single precision shots.
                int count = style == MULTISHOT_STYLE ? 1 : style == ARCANE_STYLE ? 3
                        : Math.min(10, Math.max(1, (int) Math.ceil(delta.length() / .4)));
                var streak = switch (style) {
                    case AIMED_STYLE -> CombatParticles.AIMED_STREAK.get();
                    case ARCANE_STYLE -> CombatParticles.ARCANE_STREAK.get();
                    case MULTISHOT_STYLE -> CombatParticles.VOLLEY_STREAK.get();
                    default -> CombatParticles.PIERCING_STREAK.get();
                };
                for (int i = 0; i < count; i++) {
                    var pos = arrow.position().subtract(delta.scale(i / (double) count));
                    arrow.level().addParticle(streak, pos.x, pos.y, pos.z, delta.x, delta.y, delta.z);
                }
            }
        }
    }

    /** Run after other impact handlers so canceled collisions do not play an impact effect. */
    public static void arrowImpact(ProjectileImpactEvent event) {
        try (var zone = Profiler.get().zone("covenant:arrow_impact_visuals")) {
            var arrow = event.getProjectile();
            if (!(arrow instanceof AbstractArrow) || !(arrow.level() instanceof ServerLevel level)
                    || !arrow.hasData(RpgData.SKILL_SHOT) || !arrow.getData(RpgData.SKILL_SHOT)) { return; }
            var hit = event.getRayTraceResult().getLocation();
            int style = arrow.getData(RpgData.SKILL_SHOT_STYLE);
            if (style == ARCANE_STYLE) {
                boltImpact(level, hit);
            } else {
                level.sendParticles(YELLOW, hit.x, hit.y, hit.z, style == MULTISHOT_STYLE ? 1 : 3, .04, .04, .04, 0);
            }
            // The entity-impact event runs before native damage. Keep style through that call so
            // accepted damage can choose its contact mark; entity removal owns terminal cleanup.
            if (event.getRayTraceResult().getType() == HitResult.Type.BLOCK) {
                arrow.removeData(RpgData.SKILL_SHOT);
                arrow.removeData(RpgData.SKILL_SHOT_STYLE);
            }
        }
    }

    /** A compact cross confirms accepted damage, distinct from the open collision seal or arrow dust. */
    public static void damageLanded(LivingDamageEvent.Post event) {
        var projectile = event.getSource().getDirectEntity();
        boolean spell = projectile instanceof ArcaneBolt || projectile instanceof ElementalBolt || event.getSource().is(LIGHTNING_DAMAGE);
        boolean arrow = projectile instanceof AbstractArrow && projectile.hasData(RpgData.SKILL_SHOT)
                && projectile.getData(RpgData.SKILL_SHOT);
        if ((!spell && !arrow) || AuraDamage.landedDamage(event) <= 0
                || !(event.getEntity().level() instanceof ServerLevel level)) { return; }
        var target = event.getEntity();
        // Native post-damage accounting includes absorption and Covenant shields, and excludes rejected hits.
        var contact = arrow && projectile.getData(RpgData.SKILL_SHOT_STYLE) == ARCANE_STYLE
                ? CombatParticles.ARCANE_CONTACT.get() : CombatParticles.CONTACT.get();
        level.sendParticles(contact, target.getX(), target.getY(.5), target.getZ(), 1, 0, 0, 0, 0);
    }
}
