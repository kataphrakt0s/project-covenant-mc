package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Target-local unsaved coverage, sharing one healing clock across all suppliers and coverage transitions. */
public final class WarBanners {
    public static final class Coverage {
        final Map<GroundSkillEntity,Long> sources = new IdentityHashMap<>();
        long nextHeal;
    }
    private WarBanners() {}
    static void grant(GroundSkillEntity banner, LivingEntity target) {
        var state=target.getData(RpgData.BANNER_COVERAGE);
        boolean was=covered(target); long now=Auras.now(target);
        state.sources.put(banner,now+20); banner.recipients.add(target);
        if (!was) {
            state.nextHeal=Math.max(state.nextHeal,now+20);
            if (target instanceof ServerPlayer player) { BuffDisplay.sync(player); }
        }
    }
    static void remove(GroundSkillEntity banner, LivingEntity target) {
        if (!target.hasData(RpgData.BANNER_COVERAGE)) { return; }
        target.getData(RpgData.BANNER_COVERAGE).sources.remove(banner);
        if (target instanceof ServerPlayer player) { BuffDisplay.sync(player); }
    }
    public static boolean covered(LivingEntity target) {
        if (!target.hasData(RpgData.BANNER_COVERAGE)) { return false; }
        long now=Auras.now(target);
        for (var entry : target.getData(RpgData.BANNER_COVERAGE).sources.entrySet()) {
            if (!entry.getKey().isRemoved() && entry.getValue()>now) { return true; }
        }
        return false;
    }
    static void supportOwners(LivingEntity target, Set<UUID> owners) {
        if (!target.hasData(RpgData.BANNER_COVERAGE)) { return; }
        long now=Auras.now(target);
        for (var entry : target.getData(RpgData.BANNER_COVERAGE).sources.entrySet()) {
            if (!entry.getKey().isRemoved() && entry.getValue()>now) { owners.add(entry.getKey().owner().getUUID()); }
        }
    }
    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target) || target.level().isClientSide() || !target.hasData(RpgData.BANNER_COVERAGE)) { return; }
        var state=target.getData(RpgData.BANNER_COVERAGE); long now=Auras.now(target);
        if (state.sources.isEmpty()) {
            if (now>=state.nextHeal) { target.removeData(RpgData.BANNER_COVERAGE); }
            return;
        }
        boolean changed=state.sources.entrySet().removeIf(e -> e.getKey().isRemoved() || e.getValue()<=now);
        if (changed && target instanceof ServerPlayer player) { BuffDisplay.sync(player); }
        if (state.sources.isEmpty() || !target.isAlive() || target.isSpectator() || now<state.nextHeal) { return; }
        state.nextHeal=now+20;
        double healing=0;
        for (var source : state.sources.keySet()) { healing=Math.max(healing,source.healing()); }
        if (healing>0) { target.heal((float)healing); }
    }
}
