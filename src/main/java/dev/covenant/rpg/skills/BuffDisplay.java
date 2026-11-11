package dev.covenant.rpg.skills;

import dev.covenant.rpg.RpgData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Unsaved owner-only buff receipt snapshot. Timers use the persistent skill clock, not client wall time. */
public record BuffDisplay(long clockOffset, List<Row> rows) {
    public static final BuffDisplay EMPTY = new BuffDisplay(0, List.of());
    private static final int MAX_ROWS = 32;
    private static final Auras.Kind[] AURA_KINDS = Auras.Kind.values();
    private static final Identifier DISCHARGE_FREEZE = Identifier.fromNamespaceAndPath("covenant", "discharge_freeze");

    /** Passive presence may coexist with an active deadline; zero deadline means no timed active effect. */
    public record Row(Identifier skill, boolean passive, long expiresAt, int stacks) {
        public Row {
            if (skill == null || expiresAt < 0 || stacks < 0 || stacks > 32 || (!passive && expiresAt == 0 && stacks == 0)) {
                throw new IllegalArgumentException("Invalid buff display row");
            }
        }
    }

    public BuffDisplay {
        rows = List.copyOf(rows);
        if (rows.size() > MAX_ROWS) { throw new IllegalArgumentException("Too many buff display rows"); }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, BuffDisplay> STREAM_CODEC = StreamCodec.of((buffer, view) -> {
        buffer.writeLong(view.clockOffset);
        buffer.writeVarInt(view.rows.size());
        for (var row : view.rows) {
            buffer.writeIdentifier(row.skill); buffer.writeBoolean(row.passive);
            buffer.writeLong(row.expiresAt); buffer.writeVarInt(row.stacks);
        }
    }, buffer -> {
        long offset = buffer.readLong();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ROWS) { throw new IllegalArgumentException("Invalid buff display row count"); }
        var rows = new ArrayList<Row>(count);
        for (int i = 0; i < count; i++) {
            rows.add(new Row(buffer.readIdentifier(), buffer.readBoolean(), buffer.readLong(), buffer.readVarInt()));
        }
        return new BuffDisplay(offset, rows);
    });

    /** Called by existing grant/expiry/consumption transitions; no new tick loop or nearby-entity query. */
    public static void sync(ServerPlayer player) {
        ClassResources.requireServer(player);
        // Spectators hide the HUD locally, but existing received grants can survive that mode change.
        if (!player.isAlive()) { clear(player); return; }
        long now = ClassResources.now(player);
        var runtime = player.getData(RpgData.SKILL_RUNTIME);
        var auras = player.hasData(RpgData.AURAS) ? player.getData(RpgData.AURAS) : null;
        var golemForm=ElementalGolems.benefit(player);
        if (auras == null && runtime.rushSpeedUntil <= now && runtime.bloodFeverCount == 0
                && (runtime.weaver == null || runtime.weaver.until <= now) && golemForm==null) { clear(player); return; }
        var rows = new ArrayList<Row>();
        if (golemForm!=null) { rows.add(new Row(golemForm.benefitId(),true,0,0)); }
        if (auras != null) {
            for (var kind : AURA_KINDS) {
                boolean passive = latest(auras.passive.get(kind), now) > 0;
                long until = latest(auras.active.get(kind), now);
                if (kind == Auras.Kind.FORTITUDE && auras.shield > 0 && auras.shieldUntil > now) { until = auras.shieldUntil; }
                if (passive || until > 0) {
                    rows.add(new Row(kind == Auras.Kind.DISCHARGE_FREEZE ? DISCHARGE_FREEZE : kind.id, passive, until, 0));
                }
            }
        }
        if (runtime.weaver != null && runtime.weaver.until > now && WeaverPower.assigned(player)) {
            rows.add(new Row(SkillDefinition.THREEFOLD_SYSTEM.id(), false, runtime.weaver.until, 0));
        }
        if (runtime.rushSpeedUntil > now) { rows.add(new Row(SkillDefinition.RIP_AND_RUSH.id(), false, runtime.rushSpeedUntil, 0)); }
        if (runtime.bloodFeverCount > 0) { rows.add(new Row(SkillDefinition.BLOOD_FEVER.id(), true, 0, runtime.bloodFeverCount)); }
        var view = rows.isEmpty() ? EMPTY : new BuffDisplay(now - player.level().getGameTime(), rows);
        if (!view.equals(player.getData(RpgData.BUFF_DISPLAY))) { player.setData(RpgData.BUFF_DISPLAY, view); }
    }

    public static void clear(ServerPlayer player) {
        if (player.hasData(RpgData.BUFF_DISPLAY) && !player.getData(RpgData.BUFF_DISPLAY).equals(EMPTY)) {
            player.setData(RpgData.BUFF_DISPLAY, EMPTY);
        }
    }

    private static long latest(Map<UUID, Long> grants, long now) {
        long latest = 0;
        if (grants != null) { for (long until : grants.values()) { if (until > now) { latest = Math.max(latest, until); } } }
        return latest;
    }
}
