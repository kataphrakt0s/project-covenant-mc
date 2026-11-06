package dev.covenant.rpg.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.covenant.rpg.RpgData;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Saved server-only provenance and one-bonus-per-target history for committed projectiles.
 * Missing fields/attachments mean an ordinary shot with no recorded bonus hits. Old saves
 * never stored skill identity or aura snapshots, so that lost history cannot be reconstructed.
 * The tracking-synced skill trail is cosmetic and is deliberately independent of this state.
 */
public final class ProjectileCombat {
    /** Captured multiplicative power; native arrows already store this factor in their base damage. */
    public static final Codec<Double> POWER_CODEC = Codec.DOUBLE.validate(value ->
            Double.isFinite(value) && value >= 0 ? DataResult.success(value)
                    : DataResult.error(() -> "Projectile power must be finite and nonnegative"));
    /** Captured extra magic damage, in health points; zero means no Enchanting bonus. */
    public static final Codec<Float> ENCHANTMENT_CODEC = Codec.FLOAT.validate(value ->
            Float.isFinite(value) && value >= 0 ? DataResult.success(value)
                    : DataResult.error(() -> "Projectile enchantment damage must be finite and nonnegative"));
    public static final Codec<ProjectileCombat> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("skill", false).forGetter(state -> state.skill),
            UUIDUtil.CODEC.listOf().optionalFieldOf("enchanted_targets", List.of())
                    .forGetter(state -> List.copyOf(state.enchantedTargets))
    ).apply(i, ProjectileCombat::new));

    private boolean skill;
    private final Set<UUID> enchantedTargets;

    public ProjectileCombat() { this(false, List.of()); }

    private ProjectileCombat(boolean skill, List<UUID> enchantedTargets) {
        this.skill = skill;
        this.enchantedTargets = new HashSet<>(enchantedTargets);
    }

    /** Mark at the server's skill firing boundary; neither trail removal nor reload changes provenance. */
    public static void markSkill(Projectile projectile) {
        projectile.getData(RpgData.PROJECTILE_COMBAT).skill = true;
    }

    public static boolean isSkill(Entity entity) {
        return entity != null && entity.hasData(RpgData.PROJECTILE_COMBAT)
                && entity.getData(RpgData.PROJECTILE_COMBAT).skill;
    }

    /** Claim before nested bonus damage so reentrant hits cannot award the same bonus again. */
    static boolean claimEnchantment(Projectile projectile, LivingEntity target) {
        return projectile.getData(RpgData.PROJECTILE_COMBAT).enchantedTargets.add(target.getUUID());
    }
}
