package dev.covenant.rpg.skills;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Shared native-companion target policy and loaded-terrain placement. No entity registry or world scans. */
final class Companions {
    /** Native health, status and companion-specific state, stored only in the owner's attachment. */
    static net.minecraft.nbt.CompoundTag capture(TamableAnimal companion) {
        var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, companion.registryAccess());
        companion.saveWithoutId(output);
        return output.buildResult();
    }
    private Companions() {}

    static void register() { NeoForge.EVENT_BUS.addListener(Companions::damaged); }

    static boolean canAttack(TamableAnimal companion, LivingEntity target) {
        return target != null && target.level() == companion.level() && target.canBeSeenAsEnemy()
                && (!(target instanceof Player) || companion.level().getDifficulty() != Difficulty.PEACEFUL)
                && companion.getOwner() instanceof ServerPlayer owner && Skills.canHit(owner, target);
    }

    /** At most 75 candidates, preserving the wolf's ordering and native collision checks for each body's size. */
    static boolean place(ServerPlayer owner, TamableAnimal companion) {
        var center = owner.blockPosition();
        for (int dy : new int[] {0, 1, -1}) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dz == 0) { continue; }
                    var pos = center.offset(dx, dy, dz);
                    var level = owner.level();
                    var box = companion.getDimensions(companion.getPose()).makeBoundingBox(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
                    if (!level.hasChunksAt(BlockPos.containing(box.minX, box.minY - 1, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                            || !level.getWorldBorder().isWithinBounds(box) || box.minY < level.getMinY() || box.maxY > level.getMaxY() + 1
                            || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                            || level.containsAnyLiquid(box) || !level.noCollision(companion, box)) { continue; }
                    companion.snapTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, owner.getYRot(), 0);
                    return true;
                }
            }
        }
        return false;
    }

    static void teleportToOwner(TamableAnimal companion) {
        if (companion.getOwner() instanceof ServerPlayer owner && place(owner, companion)) {
            companion.getNavigation().stop(); companion.setTarget(null);
            companion.setDeltaMovement(Vec3.ZERO); companion.fallDistance = 0;
        }
    }

    private static void direct(TamableAnimal companion, LivingEntity target) {
        if (companion != null && companion.isAlive() && companion.canAttack(target)) { companion.setTarget(target); }
    }

    private static void damaged(LivingDamageEvent.Post event) {
        if (AuraDamage.landedDamage(event) <= 0) { return; }
        var source = event.getSource().getEntity();
        var target = event.getEntity();
        if (source instanceof ServerPlayer owner) {
            direct(HunterPets.current(owner), target);
            direct(ElementalGolems.current(owner), target);
        }
        if (source instanceof LivingEntity attacker) {
            if (target instanceof ServerPlayer owner) {
                direct(HunterPets.current(owner), attacker);
                direct(ElementalGolems.current(owner), attacker);
            } else if (target instanceof HunterPet pet) { direct(pet, attacker); }
            else if (target instanceof ElementalGolem golem) { direct(golem, attacker); }
        }
    }
}
