package dev.covenant.rpg.skills;

import dev.covenant.rpg.PlayerProfiles;
import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.classes.PlayerClass;
import dev.covenant.rpg.stats.RpgStats;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Two explicit three-stage sequences, independent of pending casts and saved resource state. */
public final class Combos {
    public enum Kind {
        LUNGING(SkillDefinition.LUNGING_COMBO), RUSH(SkillDefinition.RIP_AND_RUSH);
        final SkillDefinition skill;
        Kind(SkillDefinition skill) { this.skill = skill; }
        static Kind of(SkillDefinition skill) { return skill == SkillDefinition.LUNGING_COMBO ? LUNGING : RUSH; }
    }

    static final class Progress {
        final Kind kind;
        final long cooldown;
        int stage;
        long deadline;
        boolean moving;
        Progress(Kind kind, long cooldown) { this.kind = kind; this.cooldown = cooldown; }
    }

    static final class Movement {
        final Progress progress;
        Vec3 step;
        Vec3 acceleration = Vec3.ZERO;
        final boolean noGravity;
        final float damage;
        final int shredDuration;
        final Set<UUID> attempted = new HashSet<>();
        int ticks;
        boolean stepping;
        boolean hitSoundPlayed;
        boolean travelled;
        double departureYaw;
        long lastTick = Long.MIN_VALUE;
        Movement(Progress progress, Vec3 step, int ticks, boolean noGravity, float damage, int shredDuration) {
            this.progress = progress; this.step = step; this.ticks = ticks; this.noGravity = noGravity;
            this.damage = damage; this.shredDuration = shredDuration;
        }
    }

    private static final Identifier RUSH_SPEED = Identifier.fromNamespaceAndPath("covenant", "rush_speed");
    private static final Kind[] KINDS = Kind.values();
    private Combos() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Combos::tick);
        // Removal precedes replacement-player attachment copying, including End return.
        NeoForge.EVENT_BUS.addListener(Combos::left);
    }

    static Component activate(ServerPlayer player, SkillDefinition skill) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        var kind = Kind.of(skill);
        var progress = runtime.combos.get(kind);
        if (progress != null && progress.moving) { return Skills.message("busy"); }
        int stage = progress == null ? 0 : progress.stage;
        boolean movement = kind == Kind.LUNGING ? stage == 0 : stage == 1;
        if (movement && (runtime.movement != null || player.isPassenger() || player.isSleeping())) { return Skills.message("busy"); }
        if (movement && Rooted.contains(player)) { return Skills.message("rooted"); }
        double baseCost = kind == Kind.LUNGING ? (stage == 1 ? 10 : 20) : (stage == 0 ? 30 : 20);
        double cost = RpgStats.scaleSkillValue(player, RpgStats.RESOURCE_COST, baseCost);
        if (!ClassResources.consume(player, cost)) { return Skills.message("resource"); }
        long now = ClassResources.now(player);
        if (progress == null) {
            progress = new Progress(kind, (long) Math.ceil(RpgStats.scaleSkillValue(player, RpgStats.COOLDOWN, skill.cooldownTicks())));
            runtime.combos.put(kind, progress);
        }
        // Commit the input guard and stage before callbacks can damage/re-enter/clean up this player.
        runtime.nextActivation = now + 1;
        runtime.executing = true;
        progress.deadline = 0;
        try {
            SkillSounds.release(player, skill);
            if (movement) {
                startMovement(player, progress, kind == Kind.LUNGING ? 4 : 6, kind == Kind.LUNGING ? 3 : 4);
            } else {
                attack(player, progress);
                completed(player, progress);
            }
            player.swing(InteractionHand.MAIN_HAND, true);
        } finally { runtime.executing = false; }
        return Skills.message("used", skill.displayName(), cost);
    }

    static int duration(ServerPlayer player, int ticks) {
        return (int) Math.clamp(Math.ceil(RpgStats.scaleSkillValue(player, RpgStats.SKILL_DURATION, ticks)), 1, Integer.MAX_VALUE);
    }

    private static void attack(ServerPlayer player, Progress progress) {
        float damage = (float) (player.getAttributeValue(RpgStats.DAMAGE) * AuraDamage.multiplier(player, false));
        int shredDuration = duration(player, ArmorShred.DURATION_TICKS);
        if (progress.kind == Kind.LUNGING && progress.stage == 1) {
            ComboVisuals.strike(player);
            var target = MeleeSkills.rendTarget(player);
            if (target != null) {
                var before = AuraDamage.before(target);
                MeleeSkills.hurtMeleeSkill(player, target, damage * 1.25f);
                if (before.landed(target)) {
                    ComboVisuals.contact(player, target);
                    SkillSounds.meleeHit(player);
                }
            }
            return;
        }
        var targets = progress.kind == Kind.LUNGING ? MeleeSkills.targets(player, SkillDefinition.CLEAVE) : circularTargets(player);
        float bleed = AuraDamage.bleedPower(player);
        int bleedDuration = duration(player, Bleeding.REND_DURATION_TICKS);
        int speedDuration = duration(player, 80);
        int consumed = 0;
        boolean landed = false;
        if (progress.kind == Kind.RUSH) { ComboVisuals.sweep(player, progress.stage == 2); }
        else { SkillVisuals.cleave(player, 3); }
        for (var target : targets) {
            if (player.getData(RpgData.SKILL_RUNTIME).combos.get(progress.kind) != progress) { break; }
            if (!Skills.canHit(player, target)) { continue; }
            if (progress.kind == Kind.RUSH && progress.stage == 0) {
                landed |= MeleeSkills.applyRend(player, target, damage, bleed, bleedDuration);
                continue;
            }
            boolean bleeding = Bleeding.count(target) > 0;
            var before = AuraDamage.before(target);
            MeleeSkills.hurtMeleeSkill(player, target, damage);
            if (!before.landed(target)) { continue; }
            landed = true;
            ComboVisuals.contact(player, target);
            if (progress.kind == Kind.LUNGING) {
                ArmorShred.apply(target, player, shredDuration);
            } else if (bleeding || Bleeding.count(target) > 0) {
                // Death cleanup may already have removed the pre-hit bleeds; a killing hit still earns credit.
                Bleeding.consume(target);
                consumed++;
                ComboVisuals.consume(player, target);
            }
        }
        if (landed) { SkillSounds.meleeHit(player); }
        if (consumed > 0 && player.isAlive() && player.getData(RpgData.SKILL_RUNTIME).combos.get(progress.kind) == progress) {
            player.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(new AttributeModifier(
                    RUSH_SPEED, Math.min(3, consumed) * .1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            player.getData(RpgData.SKILL_RUNTIME).rushSpeedUntil = ClassResources.now(player) + speedDuration;
            BuffDisplay.sync(player);
        }
    }

    private static List<LivingEntity> circularTargets(ServerPlayer player) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3),
                target -> Skills.canHit(player, target) && player.distanceToSqr(target) <= 9 && player.hasLineOfSight(target));
    }

    private static void startMovement(ServerPlayer player, Progress progress, int distance, int ticks) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        runtime.awaitingDashSequence = 0;
        progress.moving = true;
        runtime.movement = new Movement(progress, player.getLookAngle().normalize().scale((double) distance / ticks),
                ticks, player.isNoGravity(), (float) (player.getAttributeValue(RpgStats.DAMAGE) * .5 * AuraDamage.multiplier(player, false)),
                duration(player, ArmorShred.DURATION_TICKS));
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.setNoGravity(true);
        player.setData(RpgData.COMBO_MOVING, true);
        sendMotion(player, new DashMotion(++runtime.motionSequence, player.level().dimension().identifier(),
                player.position(), runtime.movement.step, ticks, runtime.movement.noGravity, Vec3.ZERO));
        display(player);
    }

    /** Fixed backward hop, sharing native collision, prediction, acknowledgements and cleanup with dashes. */
    static void disengage(ServerPlayer player) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        int ticks = 6;
        double yaw = Math.toRadians(player.getYRot());
        // y(n) = 4h*n*(N-n)/N^2, with h=.5: six discrete steps reach exactly .5 at tick three.
        var step = new Vec3(Math.sin(yaw) * 4 / ticks, 2.0 * (ticks - 1) / (ticks * ticks), -Math.cos(yaw) * 4 / ticks);
        var movement = new Movement(null, step, ticks, player.isNoGravity(), 0, 0);
        movement.acceleration = new Vec3(0, -4.0 / (ticks * ticks), 0);
        movement.departureYaw = yaw;
        runtime.movement = movement;
        runtime.awaitingDashSequence = 0;
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.setNoGravity(true);
        player.setData(RpgData.COMBO_MOVING, true);
        sendMotion(player, new DashMotion(++runtime.motionSequence, player.level().dimension().identifier(),
                player.position(), step, ticks, movement.noGravity, Vec3.ZERO, movement.acceleration));
        MovementVisuals.disengageDeparture(player);
    }

    /** Used by movement mixins on both sides; owner display remains separate. */
    public static boolean moving(LivingEntity entity) { return entity.getData(RpgData.COMBO_MOVING); }

    /** Hold late predicted packets until the owner confirms the authoritative endpoint. */
    public static boolean blocksMove(ServerPlayer player) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        var movement = runtime.movement;
        return movement != null ? !movement.stepping : runtime.awaitingDashSequence != 0;
    }

    /** Incoming reports stay held after travel; authoritative external movement must remain possible. */
    public static boolean awaitingMovement(ServerPlayer player) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        return runtime.movement == null && runtime.awaitingDashSequence != 0;
    }

    /** Only the executing dash owns Entity.move. The acknowledgement phase owns packets, not physics. */
    public static boolean blocksExternalMovement(ServerPlayer player) {
        var movement = player.getData(RpgData.SKILL_RUNTIME).movement;
        return movement != null && !movement.stepping;
    }

    /** Native teleports end owned travel and supersede its endpoint acknowledgement. */
    public static void teleported(ServerPlayer player) {
        interruptMovement(player, true);
        player.getData(RpgData.SKILL_RUNTIME).awaitingDashSequence = 0;
    }

    private static void sendMotion(ServerPlayer player, DashMotion motion) {
        if (player.connection.hasChannel(DashMotion.TYPE)) { PacketDistributor.sendToPlayer(player, motion); }
    }

    /** Matching acknowledgement releases movement; clients never supply an authoritative position. */
    public static void acknowledgeMovement(ServerPlayer player, long sequence, Vec3 reportedPosition) {
        ClassResources.requireServer(player);
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        if (runtime.movement != null || sequence == 0 || sequence != runtime.awaitingDashSequence) { return; }
        runtime.awaitingDashSequence = 0;
        if (!DashMotion.finite(reportedPosition) || player.position().distanceToSqr(reportedPosition) > .01) {
            SkillControls.correctPosition(player, player.getYRot(), player.getXRot());
        } else { player.connection.resetPosition(); }
    }

    /** Autosaving during travel must not persist the temporary native gravity flag. */
    public static boolean savedNoGravity(ServerPlayer player) {
        var movement = player.getData(RpgData.SKILL_RUNTIME).movement;
        return movement == null ? player.isNoGravity() : movement.noGravity;
    }

    private static void tick(PlayerTickEvent.Pre event) {
        try (var zone = Profiler.get().zone("covenant:combos_movement")) {
            if (!(event.getEntity() instanceof ServerPlayer player)) { return; }
            expire(player);
            var runtime = player.getData(RpgData.SKILL_RUNTIME);
            if (runtime.awaitingDashSequence != 0 && ClassResources.now(player) >= runtime.dashAcknowledgementUntil) {
                // Lost/unsupported acknowledgement: one bounded recovery, never a stream of teleports.
                acknowledgeMovement(player, runtime.awaitingDashSequence, new Vec3(Double.NaN, 0, 0));
            }
            if ((!player.isAlive() || player.isSpectator() || PlayerProfiles.getSelectedClass(player).orElse(null) != PlayerClass.REAVER)
                    && (!runtime.combos.isEmpty() || runtime.rushSpeedUntil != 0)) { clear(player); }
            var movement = runtime.movement;
            if (movement == null || movement.lastTick == ClassResources.now(player)) { return; }
            if (movement.progress == null && (!player.isAlive() || player.isSpectator()
                    || PlayerProfiles.getSelectedClass(player).orElse(null) != PlayerClass.STALKER
                    || !player.getData(RpgData.SKILL_LOADOUT).learned().contains(SkillDefinition.DISENGAGE.id())
                    || player.isPassenger() || player.isSleeping())) {
                interruptMovement(player, false); return;
            }
            movement.lastTick = ClassResources.now(player);
            if (Rooted.contains(player) || Auras.frozen(player)) { interruptMovement(player, false); return; }
            player.resetFallDistance();
            player.setDeltaMovement(Vec3.ZERO);
            movement.stepping = true;
            DashMovement.Result result;
            try {
                result = DashMovement.move(player, movement.step, movement.progress != null ? (body, travelled) -> {
                    if (runtime.movement != movement) { return false; }
                    ComboVisuals.trail(player, movement.progress.kind, body, travelled);
                    if (movement.progress.kind == Kind.RUSH) { dashHits(player, movement, body, travelled); }
                    return runtime.movement == movement;
                } : movement.progress == null ? (body, travelled) -> {
                    if (runtime.movement != movement) { return false; }
                    if (travelled.lengthSqr() >= 1e-8) {
                        movement.travelled = true;
                        MovementVisuals.disengageStep(player.level(),
                                new Vec3((body.minX + body.maxX) * .5, body.minY, (body.minZ + body.maxZ) * .5), travelled);
                    }
                    return runtime.movement == movement;
                } : null, movement.progress != null);
            }
            finally { movement.stepping = false; }
            // Connection.tickPlayer restores firstGood after doTick. Commit this server-owned
            // position to that anchor so vanilla does not undo the step. No packet is sent.
            player.connection.resetPosition();
            if (runtime.movement != movement) { return; }
            if (Math.abs(result.nextStep().y - movement.step.y) > 1e-8) { movement.acceleration = Vec3.ZERO; }
            movement.step = result.nextStep().add(movement.acceleration);
            if (--movement.ticks == 0 || movement.step.lengthSqr() < 1e-8) {
                // Only natural/terrain completion gets an arrival. Interruptions and lifecycle cleanup stay quiet.
                if (movement.progress == null && movement.travelled) { MovementVisuals.disengageArrival(player, movement.departureYaw); }
                interruptMovement(player, false);
            }
        }
    }

    private static void dashHits(ServerPlayer player, Movement movement, AABB body, Vec3 travelled) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        runtime.executing = true;
        try {
            for (var target : player.level().getEntitiesOfClass(LivingEntity.class, body.expandTowards(travelled),
                    enemy -> Skills.canHit(player, enemy) && !movement.attempted.contains(enemy.getUUID()))) {
                if (runtime.movement != movement) { break; }
                if (!sweptContact(body, travelled, target.getBoundingBox()) || !player.hasLineOfSight(target)) { continue; }
                movement.attempted.add(target.getUUID());
                var before = AuraDamage.before(target);
                MeleeSkills.hurtMeleeSkill(player, target, movement.damage);
                if (before.landed(target)) {
                    ArmorShred.apply(target, player, movement.shredDuration);
                    ComboVisuals.contact(player, target);
                    if (!movement.hitSoundPlayed) {
                        movement.hitSoundPlayed = true;
                        SkillSounds.meleeHit(player);
                    }
                }
            }
        } finally { runtime.executing = false; }
    }

    /** Swept AABB slab test, rather than the enclosing box (which over-hits diagonal travel). */
    static boolean sweptContact(AABB body, Vec3 delta, AABB target) {
        double enter = 0, exit = 1;
        for (int i = 0; i < 3; i++) {
            double low = switch (i) { case 0 -> target.minX - body.maxX; case 1 -> target.minY - body.maxY; default -> target.minZ - body.maxZ; };
            double high = switch (i) { case 0 -> target.maxX - body.minX; case 1 -> target.maxY - body.minY; default -> target.maxZ - body.minZ; };
            double step = switch (i) { case 0 -> delta.x; case 1 -> delta.y; default -> delta.z; };
            if (Math.abs(step) < 1e-12) { if (low > 0 || high < 0) { return false; } }
            else {
                double a = low / step, b = high / step;
                enter = Math.max(enter, Math.min(a, b)); exit = Math.min(exit, Math.max(a, b));
                if (enter > exit) { return false; }
            }
        }
        return true;
    }

    /** Root/freeze call before further input. Knockback calls after its native impulse was applied. */
    public static void interruptMovement(ServerPlayer player, boolean preserveImpulse) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        var movement = runtime.movement;
        if (movement == null) { return; }
        stopMovement(player, preserveImpulse);
        if (movement.progress != null) { completed(player, movement.progress); }
    }

    private static void stopMovement(ServerPlayer player, boolean preserveImpulse) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        var movement = runtime.movement;
        if (movement == null) { return; }
        runtime.movement = null;
        player.setNoGravity(movement.noGravity);
        player.resetFallDistance();
        if (!preserveImpulse) { player.setDeltaMovement(Vec3.ZERO); }
        player.setData(RpgData.COMBO_MOVING, false);
        runtime.awaitingDashSequence = ++runtime.motionSequence;
        runtime.dashAcknowledgementUntil = ClassResources.now(player) + 40;
        sendMotion(player, new DashMotion(runtime.motionSequence, player.level().dimension().identifier(),
                player.position(), Vec3.ZERO, 0, movement.noGravity, player.getDeltaMovement()));
        player.hurtMarked = true;
    }

    private static void completed(ServerPlayer player, Progress progress) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        if (runtime.combos.get(progress.kind) != progress) { return; }
        progress.moving = false;
        if (++progress.stage == 3) { end(player, progress.kind, ClassResources.now(player)); }
        else { progress.deadline = ClassResources.now(player) + (progress.stage == 1 ? 40 : 60); display(player); }
    }

    static void expire(ServerPlayer player) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        long now = ClassResources.now(player);
        // Exactly two bounded entries, no global entity scan or tick allocation.
        for (var kind : KINDS) {
            var progress = runtime.combos.get(kind);
            if (progress != null && !progress.moving && progress.deadline != 0 && now >= progress.deadline) { end(player, kind, progress.deadline); }
        }
        if (runtime.rushSpeedUntil != 0 && now >= runtime.rushSpeedUntil) { clearSpeed(player); }
    }

    private static void end(ServerPlayer player, Kind kind, long endedAt) {
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        var progress = runtime.combos.remove(kind);
        if (progress == null) { return; }
        if (runtime.movement != null && runtime.movement.progress == progress) { stopMovement(player, false); }
        long until = endedAt + Math.max(1, progress.cooldown);
        var state = player.getData(RpgData.SKILLS);
        player.setData(RpgData.SKILLS, state.withCooldown(kind.skill.id(), Math.max(until, state.cooldowns().getOrDefault(kind.skill.id(), 0L))));
        display(player);
    }

    static void cancel(ServerPlayer player, SkillDefinition skill) {
        if (skill == SkillDefinition.DISENGAGE) {
            var movement = player.getData(RpgData.SKILL_RUNTIME).movement;
            if (movement != null && movement.progress == null) { interruptMovement(player, false); }
            return;
        }
        if (skill.effect() != SkillDefinition.Effect.COMBO) { return; }
        end(player, Kind.of(skill), ClassResources.now(player));
        if (skill == SkillDefinition.RIP_AND_RUSH) { clearSpeed(player); }
    }

    /** Only actual validated edits invoke this; duplicate assignments still share the same sequence/buff. */
    public static void bindingChanged(ServerPlayer player, Identifier oldSkill) {
        if (oldSkill != null) { SkillDefinition.find(oldSkill).ifPresent(skill -> cancel(player, skill)); }
    }

    public static void clear(ServerPlayer player) {
        interruptMovement(player, false);
        for (var kind : KINDS) { end(player, kind, ClassResources.now(player)); }
        clearSpeed(player);
    }

    private static void clearSpeed(ServerPlayer player) {
        boolean changed = player.getData(RpgData.SKILL_RUNTIME).rushSpeedUntil != 0;
        player.getData(RpgData.SKILL_RUNTIME).rushSpeedUntil = 0;
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(RUSH_SPEED);
        if (changed) { BuffDisplay.sync(player); }
    }

    private static void display(ServerPlayer player) {
        var rows = new ArrayList<ComboDisplay.Row>(2);
        for (var progress : player.getData(RpgData.SKILL_RUNTIME).combos.values()) {
            rows.add(new ComboDisplay.Row(progress.kind.ordinal(), progress.stage, progress.moving, progress.deadline));
        }
        player.setData(RpgData.COMBO_DISPLAY, rows.isEmpty() ? ComboDisplay.EMPTY
                : new ComboDisplay(ClassResources.now(player) - player.level().getGameTime(), rows));
    }

    private static void left(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
            player.getData(RpgData.SKILL_RUNTIME).awaitingDashSequence = 0;
        }
    }
}
