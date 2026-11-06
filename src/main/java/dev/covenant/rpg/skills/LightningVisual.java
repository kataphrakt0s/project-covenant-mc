package dev.covenant.rpg.skills;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Cosmetic server-to-client beam endpoints; damage and targeting never depend on this payload. */
public record LightningVisual(Identifier dimension, Vec3 start, Vec3 end) implements CustomPacketPayload {
    public static final Type<LightningVisual> TYPE = new Type<>(Identifier.fromNamespaceAndPath("covenant", "lightning_visual"));
    public static final StreamCodec<FriendlyByteBuf, LightningVisual> STREAM_CODEC = StreamCodec.of((buf, beam) -> {
        Identifier.STREAM_CODEC.encode(buf, beam.dimension);
        DashMotion.vector(buf, beam.start); DashMotion.vector(buf, beam.end);
    }, buf -> new LightningVisual(Identifier.STREAM_CODEC.decode(buf), DashMotion.vector(buf), DashMotion.vector(buf)));

    public LightningVisual {
        if (dimension == null || !DashMotion.finite(start) || !DashMotion.finite(end) || start.distanceToSqr(end) > 32 * 32) {
            throw new IllegalArgumentException("Invalid lightning visual");
        }
    }

    @Override public Type<LightningVisual> type() { return TYPE; }
}
