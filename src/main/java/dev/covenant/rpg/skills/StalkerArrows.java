package dev.covenant.rpg.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.covenant.rpg.RpgData;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/** Native arrow payloads and target-owned fan claims; no world scan or global projectile registry. */
public final class StalkerArrows {
    /** A fan expires after sixty seconds of server time, including time in unloaded chunks. */
    public static final int FAN_LIFETIME_TICKS = 1200;
    public record Shot(int vulnerabilityTicks, Optional<UUID> fan, long expiresAt) {
        public static final Shot EMPTY = new Shot(0, Optional.empty(), 0);
        public static final Codec<Shot> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("vulnerability_ticks", 0).forGetter(Shot::vulnerabilityTicks),
                UUIDUtil.CODEC.optionalFieldOf("fan").forGetter(Shot::fan),
                Codec.LONG.optionalFieldOf("expires_at", 0L).forGetter(Shot::expiresAt)
        ).apply(i, Shot::new));
    }
    /** Saved with the target, so sibling arrows cannot retry after either side unloads/reloads. */
    public record Hits(Map<String, Long> fans) {
        public static final Hits EMPTY = new Hits(Map.of());
        public static final Codec<Hits> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG).xmap(Hits::new, Hits::fans);
        public Hits { fans = Map.copyOf(fans); }
    }
    private StalkerArrows() {}

    public static boolean expired(AbstractArrow arrow) {
        var shot = arrow.getData(RpgData.STALKER_SHOT);
        return shot.fan().isPresent() && arrow.level() instanceof ServerLevel level
                && level.getServer().overworld().getGameTime() >= shot.expiresAt();
    }

    /** Claim before any damage callback, including shield/Dodge checks and ammunition effects. */
    public static boolean claim(AbstractArrow arrow, LivingEntity target) {
        var shot = arrow.getData(RpgData.STALKER_SHOT);
        if (shot.fan().isEmpty()) { return true; }
        if (expired(arrow)) { return false; }
        long now = ((ServerLevel) target.level()).getServer().overworld().getGameTime();
        var old = target.getData(RpgData.MULTISHOT_HITS).fans();
        String key = shot.fan().orElseThrow().toString();
        if (old.getOrDefault(key, Long.MIN_VALUE) > now) { return false; }
        var next = new HashMap<>(old);
        next.values().removeIf(deadline -> deadline <= now);
        next.put(key, shot.expiresAt());
        target.setData(RpgData.MULTISHOT_HITS, new Hits(next));
        return true;
    }

    public static DamageSource source(AbstractArrow arrow, DamageSource ordinary) {
        if (arrow.getData(RpgData.STALKER_SHOT).vulnerabilityTicks() == 0) { return ordinary; }
        var type = arrow.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, SkillDefinition.ARCANE_ARROW.id()));
        return new DamageSource(type, arrow, arrow.getOwner());
    }

    public static float damage(AbstractArrow arrow, float ordinary) {
        return arrow.getData(RpgData.STALKER_SHOT).vulnerabilityTicks() == 0 ? ordinary
                : (float) (SkillDefinition.ARCANE_ARROW.power() * arrow.getData(RpgData.AURA_PROJECTILE_POWER));
    }

    public static void landed(AbstractArrow arrow, LivingEntity target, float health, float absorption) {
        int ticks = arrow.getData(RpgData.STALKER_SHOT).vulnerabilityTicks();
        if (ticks > 0 && (target.getHealth() < health || target.getAbsorptionAmount() < absorption)
                && arrow.getOwner() instanceof LivingEntity owner) {
            Vulnerability.apply(target, owner, ticks);
        }
    }
}
