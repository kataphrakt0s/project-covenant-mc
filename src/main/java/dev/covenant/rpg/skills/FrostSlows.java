package dev.covenant.rpg.skills;

import dev.covenant.Covenant;
import dev.covenant.rpg.RpgData;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Source membership is transient on the target; a single modifier prevents overlap stacking. */
public final class FrostSlows {
    public static final Identifier MODIFIER = Identifier.fromNamespaceAndPath(Covenant.MOD_ID, "frost_patch_slow");

    public static final class Sources {
        final Set<TrapEntity> patches = new HashSet<>();
    }

    private FrostSlows() {}
    public static void register() {
        NeoForge.EVENT_BUS.addListener(FrostSlows::tick);
        NeoForge.EVENT_BUS.addListener(FrostSlows::left);
    }

    static void add(LivingEntity target, TrapEntity patch) {
        target.getData(RpgData.FROST_SOURCES).patches.add(patch);
        CovenantSlows.update(target);
    }

    static void remove(LivingEntity target, TrapEntity patch) {
        if (!target.hasData(RpgData.FROST_SOURCES)) { return; }
        var sources = target.getData(RpgData.FROST_SOURCES).patches;
        sources.remove(patch);
        if (sources.isEmpty()) { clear(target); } else { CovenantSlows.update(target); }
    }

    public static void cleanse(LivingEntity target) { clear(target); }

    private static void clear(LivingEntity target) {
        var movement = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) { movement.removeModifier(MODIFIER); }
        target.removeData(RpgData.FROST_SOURCES);
        CovenantSlows.update(target);
    }

    private static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target) || target.level().isClientSide()
                || !target.hasData(RpgData.FROST_SOURCES)) { return; }
        // Recheck after target movement as well as patch ticking, so final exits have no lingering slow.
        var sources = target.getData(RpgData.FROST_SOURCES).patches;
        sources.removeIf(patch -> !patch.covers(target));
        if (sources.isEmpty()) { clear(target); } else { CovenantSlows.update(target); }
    }

    private static void left(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity target && !target.level().isClientSide()
                && target.hasData(RpgData.FROST_SOURCES)) { clear(target); }
    }
}
