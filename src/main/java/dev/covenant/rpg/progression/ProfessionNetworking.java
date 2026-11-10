package dev.covenant.rpg.progression;

import dev.covenant.rpg.RpgData;
import dev.covenant.rpg.skills.ClassResources;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Bounded intent-only purchases, dispatched on the logical server thread by NeoForge. */
public final class ProfessionNetworking {
    private ProfessionNetworking() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(dev.covenant.CovenantNetwork.version("professions/4")).playToServer(Apply.TYPE, Apply.STREAM_CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player && player.getData(RpgData.SKILL_RUNTIME)
                    .acceptRequest(ClassResources.now(player), request)) {
                player.sendSystemMessage(ProfessionAllocations.apply(player, request.profession(), request.revision(), request.expected(), request.desired()));
            }
        });
    }
    public record Apply(Identifier profession, long revision, Map<Identifier, ProfessionState.Allocation> expected,
            Map<Identifier, Integer> desired) implements CustomPacketPayload {
        public Apply { expected = Map.copyOf(expected); desired = Map.copyOf(desired); }
        public static final Type<Apply> TYPE = new Type<>(ProfessionData.id("apply_profession"));
        public static final StreamCodec<FriendlyByteBuf, Apply> STREAM_CODEC = StreamCodec.of((buf, request) -> {
            buf.writeIdentifier(request.profession); buf.writeVarLong(request.revision);
            buf.writeVarInt(request.expected.size());
            request.expected.forEach((id, allocation) -> { buf.writeIdentifier(id); buf.writeVarInt(allocation.rank()); buf.writeVarInt(allocation.paidPoints()); });
            buf.writeVarInt(request.desired.size());
            request.desired.forEach((id, rank) -> { buf.writeIdentifier(id); buf.writeVarInt(rank); });
        }, buf -> {
            var profession = buf.readIdentifier(); long revision = buf.readVarLong();
            if (revision < 1) { throw invalid(); }
            var expected = new HashMap<Identifier, ProfessionState.Allocation>();
            for (int remaining = count(buf); remaining > 0; remaining--) {
                var id = buf.readIdentifier(); int rank = buf.readVarInt(); int paid = buf.readVarInt();
                if (rank < 1 || rank > 64 || paid < rank || paid > 1000 || expected.put(id, new ProfessionState.Allocation(rank, paid)) != null) { throw invalid(); }
            }
            var desired = new HashMap<Identifier, Integer>();
            for (int remaining = count(buf); remaining > 0; remaining--) {
                var id = buf.readIdentifier(); int rank = buf.readVarInt();
                if (rank < 1 || rank > 64 || desired.put(id, rank) != null) { throw invalid(); }
            }
            return new Apply(profession, revision, expected, desired);
        });
        private static int count(FriendlyByteBuf buf) { int size = buf.readVarInt(); if (size < 0 || size > 128) { throw invalid(); } return size; }
        private static io.netty.handler.codec.DecoderException invalid() { return new io.netty.handler.codec.DecoderException("Invalid profession allocation payload"); }
        @Override public Type<Apply> type() { return TYPE; }
    }
}
