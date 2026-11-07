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
        var registrar = event.registrar("3");
        registrar.playToServer(Apply.TYPE, Apply.STREAM_CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player && accept(player, request)) {
                player.sendSystemMessage(SkillLoadouts.apply(player, request.expected(), request.desired()));
            }
        });
        registrar.playToClient(DashMotion.TYPE, DashMotion.STREAM_CODEC);
        registrar.playToClient(LightningVisual.TYPE, LightningVisual.STREAM_CODEC);
        registrar.playToServer(DashFinished.TYPE, DashFinished.STREAM_CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player) { Combos.acknowledgeMovement(player, request.sequence(), request.position()); }
        });
        registrar.playToServer(Learn.TYPE, Learn.STREAM_CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player && accept(player, request)) {
                player.sendSystemMessage(SkillLoadouts.learn(player, request.skill(), request.learn()));
            }
        });
        registrar.playToServer(Bind.TYPE, Bind.STREAM_CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player && accept(player, request)) {
                player.sendSystemMessage(SkillLoadouts.bind(player, request.slot(), request.skill().orElse(null)));
            }
        });
        registrar.playToServer(Use.TYPE, Use.STREAM_CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player && accept(player, request)) {
                player.sendSystemMessage(SkillLoadouts.activateSlot(player, request.slot()));
            }
        });
    }

    private static boolean accept(ServerPlayer player, CustomPacketPayload request) {
        ClassResources.requireServer(player);
        return player.getData(dev.covenant.rpg.RpgData.SKILL_RUNTIME).acceptRequest(ClassResources.now(player), request);
    }

    public record Apply(java.util.Map<Identifier, Integer> expected, java.util.Map<Identifier, Integer> desired) implements CustomPacketPayload {
        public Apply { expected = java.util.Map.copyOf(expected); desired = java.util.Map.copyOf(desired); }
        public static final Type<Apply> TYPE = SkillNetworking.type("apply_skills");
        public static final StreamCodec<FriendlyByteBuf, Apply> STREAM_CODEC = StreamCodec.of((buf, value) -> {
            writeRanks(buf, value.expected); writeRanks(buf, value.desired);
        }, buf -> new Apply(readRanks(buf), readRanks(buf)));
        private static void writeRanks(FriendlyByteBuf buf, java.util.Map<Identifier, Integer> ranks) {
            buf.writeVarInt(ranks.size()); ranks.forEach((id, rank) -> { buf.writeIdentifier(id); buf.writeVarInt(rank); });
        }
        private static java.util.Map<Identifier, Integer> readRanks(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            if (size < 0 || size > 62) { throw new io.netty.handler.codec.DecoderException("Too many skill ranks"); }
            var result = new java.util.HashMap<Identifier, Integer>();
            for (int index = 0; index < size; index++) {
                var id = buf.readIdentifier(); int rank = buf.readVarInt();
                if (rank < 1 || rank > 12 || result.put(id, rank) != null) { throw new io.netty.handler.codec.DecoderException("Invalid skill rank"); }
            }
            return result;
        }
        @Override public Type<Apply> type() { return TYPE; }
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
