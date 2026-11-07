package dev.covenant.rpg.skills;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Server-approved start/stop trajectory. Sent only to the moving player, never saved. */
public record DashMotion(long sequence, Identifier dimension, Vec3 position, Vec3 step,
        int ticks, boolean noGravity, Vec3 velocity, Vec3 acceleration) implements CustomPacketPayload {
    public DashMotion(long sequence, Identifier dimension, Vec3 position, Vec3 step, int ticks, boolean noGravity, Vec3 velocity) {
        this(sequence, dimension, position, step, ticks, noGravity, velocity, Vec3.ZERO);
    }
    public static final Type<DashMotion> TYPE = new Type<>(Identifier.fromNamespaceAndPath("covenant", "dash_motion"));
    public static final StreamCodec<FriendlyByteBuf, DashMotion> STREAM_CODEC = StreamCodec.of((buf, value) -> {
        buf.writeLong(value.sequence); Identifier.STREAM_CODEC.encode(buf, value.dimension);
        vector(buf, value.position); vector(buf, value.step); buf.writeByte(value.ticks);
        buf.writeBoolean(value.noGravity); vector(buf, value.velocity); vector(buf, value.acceleration);
    }, buf -> new DashMotion(buf.readLong(), Identifier.STREAM_CODEC.decode(buf), vector(buf), vector(buf),
            buf.readUnsignedByte(), buf.readBoolean(), vector(buf), vector(buf)));

    public DashMotion {
        if (sequence <= 0 || ticks < 0 || ticks > 8 || dimension == null
                || !finite(position) || !finite(step) || !finite(velocity) || !finite(acceleration)
                || acceleration.x != 0 || acceleration.z != 0 || Math.abs(acceleration.y) > .2
                || step.lengthSqr() > 2.25 + 1e-8) {
            throw new IllegalArgumentException("Invalid dash motion");
        }
    }
    public boolean active() { return ticks > 0; }
    @Override public Type<DashMotion> type() { return TYPE; }
    static void vector(FriendlyByteBuf buf, Vec3 value) { buf.writeDouble(value.x); buf.writeDouble(value.y); buf.writeDouble(value.z); }
    static Vec3 vector(FriendlyByteBuf buf) { return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()); }
    static boolean finite(Vec3 value) { return value != null && Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z); }
}
