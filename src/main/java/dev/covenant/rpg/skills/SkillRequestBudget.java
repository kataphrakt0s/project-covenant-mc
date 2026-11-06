package dev.covenant.rpg.skills;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Unsaved ingress budget, separate from skill cooldowns and the successful-activation guard. */
final class SkillRequestBudget {
    /** Up to sixteen distinct requests in a burst; one token recovers per server game tick. */
    private static final int BURST = 16;
    private int tokens = BURST;
    private long lastTick = Long.MIN_VALUE;
    private final Set<CustomPacketPayload> seen = new HashSet<>();

    boolean accept(long now, CustomPacketPayload request) {
        if (now != lastTick) {
            if (lastTick != Long.MIN_VALUE) { tokens = (int) Math.min(BURST, tokens + Math.max(0, now - lastTick)); }
            lastTick = now;
            seen.clear();
        }
        if (tokens == 0 || seen.contains(request)) { return false; }
        --tokens;
        seen.add(request);
        return true;
    }
}
