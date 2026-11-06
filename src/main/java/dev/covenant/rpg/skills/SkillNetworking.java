package dev.covenant.rpg.skills;

import dev.covenant.Covenant;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Small intent-only packets. NeoForge dispatches these handlers on the logical server main thread. */
public final class SkillNetworking {
    private SkillNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(DashMotion.TYPE, DashMotion.STREAM_CODEC);
        registrar.playToServer(DashFinished.TYPE, DashFinished.STREAM_CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player) { Combos.acknowledgeMovement(player, request.sequence(), request.position()); }
        });
        registrar.playToServer(Learn.TYPE, Learn.STREAM_CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                player.sendSystemMessage(SkillLoadouts.learn(player, request.skill(), request.learn()));
            }
        });
        registrar.playToServer(Bind.TYPE, Bind.STREAM_CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                player.sendSystemMessage(SkillLoadouts.bind(player, request.slot(), request.skill().orElse(null)));
            }
        });
        registrar.playToServer(Use.TYPE, Use.STREAM_CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                player.sendSystemMessage(SkillLoadouts.activateSlot(player, request.slot()));
            }
        });
    }

    public record Learn(Identifier skill, boolean learn) implements CustomPacketPayload {
        public static final Type<Learn> TYPE = SkillNetworking.type("learn_skill");
        public static final StreamCodec<FriendlyByteBuf, Learn> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Learn::skill, ByteBufCodecs.BOOL, Learn::learn, Learn::new);
        @Override public Type<Learn> type() { return TYPE; }
    }

    public record Bind(int slot, Optional<Identifier> skill) implements CustomPacketPayload {
        public static final Type<Bind> TYPE = SkillNetworking.type("bind_skill");
        public static final StreamCodec<FriendlyByteBuf, Bind> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Bind::slot, Identifier.STREAM_CODEC.apply(ByteBufCodecs::optional), Bind::skill, Bind::new);
        @Override public Type<Bind> type() { return TYPE; }
    }

    public record Use(int slot) implements CustomPacketPayload {
        public static final Type<Use> TYPE = SkillNetworking.type("use_skill_slot");
        public static final StreamCodec<FriendlyByteBuf, Use> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Use::slot, Use::new);
        @Override public Type<Use> type() { return TYPE; }
    }

    /** Acknowledges an already-ended server movement; reported position can only trigger a correction. */
    public record DashFinished(long sequence, net.minecraft.world.phys.Vec3 position) implements CustomPacketPayload {
        public static final Type<DashFinished> TYPE = SkillNetworking.type("dash_finished");
        public static final StreamCodec<FriendlyByteBuf, DashFinished> STREAM_CODEC = StreamCodec.of((buf, value) -> {
            buf.writeLong(value.sequence); DashMotion.vector(buf, value.position);
        }, buf -> new DashFinished(buf.readLong(), DashMotion.vector(buf)));
        @Override public Type<DashFinished> type() { return TYPE; }
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Covenant.MOD_ID, name));
    }
}
