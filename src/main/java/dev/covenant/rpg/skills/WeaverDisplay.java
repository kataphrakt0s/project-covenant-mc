package dev.covenant.rpg.skills;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Unsaved owner-only view. Element order is fire, ice, lightning; values are percentages of capacity/bonus. */
public record WeaverDisplay(boolean discharge, boolean threefold, int fire, int ice, int lightning,
        int maxCharges, int firePower, int icePower, int lightningPower, boolean buff) {
    public static final WeaverDisplay EMPTY = new WeaverDisplay(false, false, 0, 0, 0, 5, 0, 0, 0, false);
    public static final StreamCodec<RegistryFriendlyByteBuf, WeaverDisplay> STREAM_CODEC = StreamCodec.of((b, v) -> {
        b.writeBoolean(v.discharge); b.writeBoolean(v.threefold); b.writeVarInt(v.fire); b.writeVarInt(v.ice); b.writeVarInt(v.lightning);
        b.writeVarInt(v.maxCharges); b.writeVarInt(v.firePower); b.writeVarInt(v.icePower); b.writeVarInt(v.lightningPower); b.writeBoolean(v.buff);
    }, b -> new WeaverDisplay(b.readBoolean(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt(),
            b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean()));
}
