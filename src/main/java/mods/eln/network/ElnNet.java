package mods.eln.network;

import mods.eln.Eln;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.ByteArrayOutputStream;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * EA's network channel on 26.x: one payload carrying the 1.12 packet bytes unchanged (first byte = EA packet id, then
 * whatever the node / element / GUI wrote with its DataOutputStream). Server->client publishes, client->server GUI
 * packets. Handlers are plugged in by the server packet handler and by the client setup (P3), so this class loads on
 * both sides.
 */
public final class ElnNet {
    private ElnNet() {
    }

    public record Data(byte[] bytes) implements CustomPacketPayload {
        public static final Type<Data> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Eln.MODID, "data"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC =
            ByteBufCodecs.BYTE_ARRAY.<RegistryFriendlyByteBuf>cast().map(Data::new, Data::bytes);

        @Override
        public Type<Data> type() {
            return TYPE;
        }
    }

    /** Server side: bytes from a client (GUI packets). Set by the server packet handler. */
    public static BiConsumer<byte[], ServerPlayer> serverHandler = (b, p) -> {
    };
    /** Client side: bytes from the server. Set by the client setup. */
    public static Consumer<byte[]> clientHandler = b -> {
    };

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar(BuildInfo.channelVersion("1")).playBidirectional(Data.TYPE, Data.CODEC,
            (Data d, IPayloadContext ctx) -> serverHandler.accept(d.bytes(), (ServerPlayer) ctx.player()),
            (Data d, IPayloadContext ctx) -> clientHandler.accept(d.bytes()))
            .playToServer(mods.eln.bike.BikeCrash.TYPE, mods.eln.bike.BikeCrash.CODEC,
                (mods.eln.bike.BikeCrash c, IPayloadContext ctx) -> mods.eln.bike.ElectricBike.onCrash((ServerPlayer) ctx.player(), c.entityId()))
            .playToServer(mods.eln.bike.BikeLamp.TYPE, mods.eln.bike.BikeLamp.CODEC,
                (mods.eln.bike.BikeLamp l, IPayloadContext ctx) -> mods.eln.bike.ElectricBike.onLampToggle((ServerPlayer) ctx.player())));
    }

    public static void sendToPlayer(ServerPlayer player, ByteArrayOutputStream bos) {
        Data d = new Data(bos.toByteArray());
        // only to clients that have EA's channel (a vanilla client or a test's mock player would refuse it)
        if (player.connection == null || !player.connection.hasChannel(d)) return;
        PacketDistributor.sendToPlayer(player, d);
    }

    public static void sendToTracking(ServerLevel level, int blockX, int blockZ, ByteArrayOutputStream bos) {
        Data d = new Data(bos.toByteArray());
        for (ServerPlayer p : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(blockX >> 4, blockZ >> 4), false))
            if (p.connection != null && p.connection.hasChannel(d)) PacketDistributor.sendToPlayer(p, d);
    }

    /** Client -> server (GUI packets). Only call on the client. */
    public static void sendToServer(ByteArrayOutputStream bos) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new Data(bos.toByteArray()));
    }
}
